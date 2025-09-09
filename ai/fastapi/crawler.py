# app/crawler.py
import hashlib
from datetime import datetime, timedelta, timezone

from models import News, Base
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from naver_crawler import discover_links, fetch_and_parse, embed_title, EMBED_MODEL_NAME

# DB 연결 (환경변수에서 읽도록 변경 권장)
DATABASE_URL = "postgresql+psycopg2://postgres:1234@localhost:5432/newsdb"
engine = create_engine(DATABASE_URL)
SessionLocal = sessionmaker(bind=engine)

def save_news_to_db(article: dict, db):
    """크롤링된 기사 -> DB 저장"""
    try:
        # 본문 텍스트만 추출
        content_text = "\n".join(
            b["content"] for b in article.get("body", []) if b.get("type") == "text"
        )
        category = ", ".join(article.get("section", []))
        created_at = (
            datetime.fromisoformat(article["published_at"])
            if article.get("published_at")
            else None
        )
        embedding = article.get("title_embedding", {}).get("vector")

        news = News(
            title=article.get("title"),
            content=content_text,
            category=category,
            author=article.get("reporter"),
            newsPaper=article.get("press"),
            created_at=created_at,
            views=0,
            embedding=embedding,
        )
        db.add(news)
        db.commit()
        print(f"[DB] saved: {news.title}")

    except Exception as e:
        db.rollback()
        print("[DB ERROR]", e)


def run_crawl_job():
    """크롤링 실행 → PostgreSQL 저장"""
    today = datetime.today()
    reg_dates = [(today - timedelta(days=i)).strftime("%Y%m%d") for i in range(1)]

    db = SessionLocal()

    seen_ids: set[str] = set()
    for reg in reg_dates:
        for sid1 in ["100", "101", "102", "103", "104", "105"]:
            try:
                urls = discover_links(sid1, reg, max_pages=1)
            except Exception as e:
                print(f"[ERR] discover sid1={sid1}/{reg} -> {e}")
                continue

            for u in urls:
                try:
                    item = fetch_and_parse(u)
                    if not item:
                        continue

                    # 제목 임베딩 추가
                    try:
                        emb = embed_title(item.get("title") or "")
                        if emb:
                            item["title_embedding"] = {
                                "model": EMBED_MODEL_NAME,
                                "vector": emb,
                                "dim": len(emb),
                                "normalized": True,
                            }
                    except Exception as e:
                        print("[EMB-ERR] title embedding:", e)

                    # oid/aid 기준 중복 제거
                    canon_url = item.get("url")
                    if canon_url and canon_url in seen_ids:
                        continue
                    seen_ids.add(canon_url)

                    # DB 저장
                    save_news_to_db(item, db)

                except Exception as e:
                    print("[ERR]", u, e)

    db.close()
    print("✔ 크롤링 완료")
