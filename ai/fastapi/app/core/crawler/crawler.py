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
            thumbnail=thumbnail_url
        )
        db.add(news)
        db.commit()
        print(f"[DB] saved: {news.title}")

    except Exception as e:
        db.rollback()
        print("[DB ERROR]", e)


def get_latest_times_per_section(db):
    """DB에서 섹션별 최신 작성시간 가져오기"""
    rows = (
        db.query(News.category, func.max(News.created_at))
        .group_by(News.category)
        .all()
    )
    return {row[0]: row[1] for row in rows if row[1]}
    
def run_crawl_job():
    today = datetime.today()
    reg_dates = [(today - timedelta(days=i)).strftime("%Y%m%d") for i in range(1)]

    db = SessionLocal()
    latest_times = get_latest_times_per_section(db)
    print(f"[LATEST] 섹션별 최신 작성시간: {latest_times}")

    seen_ids: set[str] = set()
    for reg in reg_dates:
        for sid1 in ["100", "101", "102", "103", "104", "105"]:
            section_name = SID1_TO_SECTION.get(sid1, "기타")
            latest_dt = latest_times.get(section_name)

            print(f"▶ 섹션 {sid1} ({section_name}) 크롤링 시작")
            discover_and_store(sid1, reg, db, latest_dt, seen_ids, save_news_to_db, max_pages=MAX_PAGE)

    db.close()
    print("✔ 크롤링 완료")


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
