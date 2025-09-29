from datetime import datetime, timedelta
from sqlalchemy import func
from app.db.models.news import News
from app.core.crawler.naver_crawler import (
    discover_and_store, discover_links, fetch_and_parse, embed_title, EMBED_MODEL_NAME, SID1_TO_SECTION
)
from app.utils.config import settings
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
import random
import threading
from concurrent.futures import ThreadPoolExecutor, as_completed
from loguru import logger

engine = create_engine(settings.DATABASE_URL, pool_pre_ping=True)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
MAX_PAGE=100

def save_news_to_db(article: dict, db):
    """크롤링된 기사 -> DB 저장"""
    try:
        content_blocks = article.get("body", [])
        category = ", ".join(article.get("category", []))
        created_at = (
            datetime.fromisoformat(article["published_at"])
            if article.get("published_at")
            else None
        )

        # created_at이 None이면 DB에 저장하지 않음 (스포츠/연예 뉴스 필터링)
        if created_at is None:
            logger.warning(f"[SKIP] created_at이 None인 기사 스킵: {article.get('title', 'Unknown')}")
            return

        embedding = article.get("title_embedding", {}).get("vector")

        # ✅ 첫 번째 이미지 URL 추출
        thumbnail_url = None
        if isinstance(content_blocks, list):
            for block in content_blocks:
                if block.get("type") == "image":
                    thumbnail_url = block.get("content")
                    break

        news = News(
            title=article.get("title"),
            content=content_blocks,
            category=category,
            author=article.get("reporter"),
            news_paper=article.get("press"),
            created_at=created_at,
            views=random.randint(1000, 10000),
            embedding=embedding,
            thumbnail=thumbnail_url,
            scrap_count=random.randint(1, 20)
        )
        db.add(news)
        db.commit()
        logger.info(f"[DB] saved: {news.title}")

    except Exception as e:
        db.rollback()
        logger.error(f"[DB ERROR] {e}")


def get_latest_times_per_section(db):
    """DB에서 섹션별 최신 작성시간 가져오기"""
    rows = (
        db.query(News.category, func.max(News.created_at))
        .group_by(News.category)
        .all()
    )
    return {row[0]: row[1] for row in rows if row[1]}
    
def crawl_section(sid1: str, reg_date: str, latest_times: dict, seen_ids_lock: threading.Lock, global_seen_ids: set[str]):
    """단일 섹션 크롤링 (스레드용)"""
    section_name = SID1_TO_SECTION.get(sid1, "기타")
    latest_dt = latest_times.get(section_name)

    # 각 스레드별로 별도 DB 세션 생성
    db = SessionLocal()
    local_seen_ids = set()

    try:
        logger.info(f"▶ [Thread-{sid1}] 섹션 {sid1} ({section_name}) 크롤링 시작")

        def thread_safe_save(article: dict, db_session):
            """스레드 안전한 저장 함수 (테스트용 - 프린트만)"""
            canon_url = article.get("url")
            if not canon_url:
                return

            # 전역 seen_ids 체크 (락 사용)
            with seen_ids_lock:
                if canon_url in global_seen_ids:
                    return
                global_seen_ids.add(canon_url)

            # 로컬에도 추가
            local_seen_ids.add(canon_url)


            # DB 저장
            try:
                save_news_to_db(article, db_session)
            except Exception as e:
                logger.error(f"    ❌ [Thread-{sid1}] DB 저장 오류: {e}")

        discover_and_store(sid1, reg_date, db, latest_dt, local_seen_ids, thread_safe_save, max_pages=MAX_PAGE)

        logger.info(f"✔ [Thread-{sid1}] 섹션 {sid1} ({section_name}) 크롤링 완료 - {len(local_seen_ids)}개 기사")
        return len(local_seen_ids)

    except Exception as e:
        logger.error(f"❌ [Thread-{sid1}] 섹션 {sid1} 크롤링 오류: {e}")
        return 0
    finally:
        db.close()

def run_crawl_job():
    """멀티스레드 크롤링 실행"""
    today = datetime.today()
    reg_dates = [(today - timedelta(days=i)).strftime("%Y%m%d") for i in range(1)]

    # 메인 DB 세션으로 최신 시간 조회
    db = SessionLocal()
    latest_times = get_latest_times_per_section(db)
    db.close()

    logger.info(f"[LATEST] 섹션별 최신 작성시간: {latest_times}")

    # 스레드 안전한 전역 seen_ids
    global_seen_ids: set[str] = set()
    seen_ids_lock = threading.Lock()

    total_articles = 0

    for reg in reg_dates:
        logger.info(f"\n=== 날짜: {reg} (멀티스레드 크롤링) ===")

        # ThreadPoolExecutor로 6개 섹션 동시 실행
        sections = ["100", "101", "102", "103", "104", "105"]

        with ThreadPoolExecutor(max_workers=6, thread_name_prefix="Crawler") as executor:
            # 각 섹션별로 스레드 시작
            future_to_section = {
                executor.submit(crawl_section, sid1, reg, latest_times, seen_ids_lock, global_seen_ids): sid1
                for sid1 in sections
            }

            # 완료된 스레드 결과 수집
            for future in as_completed(future_to_section):
                sid1 = future_to_section[future]
                try:
                    article_count = future.result()
                    total_articles += article_count
                except Exception as e:
                    logger.error(f"❌ 섹션 {sid1} 스레드 실행 오류: {e}")

    logger.info(f"\n🎉 ===== 멀티스레드 크롤링 완료! =====")
    logger.info(f"📊 크롤링 결과 요약:")
    logger.info(f"  📰 총 수집 기사: {total_articles:,}개")
    logger.info(f"  🔗 고유 URL: {len(global_seen_ids):,}개")
    logger.info(f"  🧵 사용된 스레드: 6개 (섹션별 병렬 처리)")
    logger.info(f"⏰ 크롤링 완료 시각: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    logger.info(f"==========================================\n")


# def run_crawl_job():
#     """크롤링 실행 → PostgreSQL 저장"""
#     today = datetime.today()
#     reg_dates = [(today - timedelta(days=i)).strftime("%Y%m%d") for i in range(1)]

#     db = SessionLocal()
#     latest_times = get_latest_times_per_section(db)
#     print(f"[LATEST] 섹션별 최신 작성시간: {latest_times}")

#     seen_ids: set[str] = set()
#     for reg in reg_dates:
#         for sid1 in ["100", "101", "102", "103", "104", "105"]:
#             section_name = SID1_TO_SECTION.get(sid1, "기타")
#             stop_section = False

#             try:
#                 urls = discover_links(sid1, reg, max_pages=MAX_PAGE)
#             except Exception as e:
#                 print(f"[ERR] discover sid1={sid1}/{reg} -> {e}")
#                 continue

#             for u in urls:
#                 try:
#                     item = fetch_and_parse(u, sid1=sid1)
#                     if not item:
#                         continue

#                     pub_time = item.get("published_at")
#                     if pub_time:
#                         pub_dt = datetime.fromisoformat(pub_time)
#                         latest_dt = latest_times.get(section_name)

#                         if latest_dt and pub_dt <= latest_dt:
#                             print(f"[STOP] 섹션 {sid1} ({section_name}) "
#                                   f"- {pub_dt} <= {latest_dt}")
#                             stop_section = True
#                             break

#                     # 제목 임베딩 추가
#                     try:
#                         emb = embed_title(item.get("title") or "")
#                         if emb:
#                             item["title_embedding"] = {
#                                 "model": EMBED_MODEL_NAME,
#                                 "vector": emb,
#                                 "dim": len(emb),
#                                 "normalized": True,
#                             }
#                     except Exception as e:
#                         print("[EMB-ERR] title embedding:", e)

#                     canon_url = item.get("url")
#                     if canon_url and canon_url in seen_ids:
#                         continue
#                     seen_ids.add(canon_url)

#                     save_news_to_db(item, db)

#                 except Exception as e:
#                     print("[ERR]", u, e)

#             if stop_section:
#                 print(f"[INFO] 섹션 {sid1} ({section_name}) 크롤링 중단")
#                 continue

#     db.close()
#     print("✔ 크롤링 완료")
