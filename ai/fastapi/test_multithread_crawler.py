#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
멀티스레드 크롤링 테스트 스크립트 (DB 저장 없이 프린트만)
"""

import threading
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime, timedelta
import sys
import os

# FastAPI 앱 경로 추가
sys.path.append(os.path.join(os.path.dirname(__file__), 'fastapi'))

from app.core.crawler.naver_crawler import (
    discover_links, fetch_and_parse, embed_title, EMBED_MODEL_NAME, SID1_TO_SECTION
)

MAX_TEST_PAGES = 2  # 테스트용으로 적게

def test_crawl_section(sid1: str, reg_date: str, seen_ids_lock: threading.Lock, global_seen_ids: set[str]):
    """테스트용 단일 섹션 크롤링 (프린트만)"""
    section_name = SID1_TO_SECTION.get(sid1, "기타")
    local_seen_ids = set()

    try:
        print(f"🚀 [Thread-{sid1}] 섹션 {sid1} ({section_name}) 크롤링 시작")

        # 링크 수집
        urls = discover_links(sid1, reg_date, max_pages=MAX_TEST_PAGES)
        print(f"📋 [Thread-{sid1}] 수집된 링크: {len(urls)}개")

        for i, url in enumerate(urls[:10]):  # 최대 10개만 테스트
            try:
                article = fetch_and_parse(url, sid1=sid1)
                if not article:
                    continue

                canon_url = article.get("url")
                if not canon_url:
                    continue

                # 전역 seen_ids 체크 (락 사용)
                with seen_ids_lock:
                    if canon_url in global_seen_ids:
                        continue
                    global_seen_ids.add(canon_url)

                local_seen_ids.add(canon_url)

                # 프린트
                title = article.get("title", "제목 없음")
                press = article.get("press", "언론사 없음")
                category = ", ".join(article.get("category", []))
                published = article.get("published_at", "날짜 없음")

                # 본문 미리보기
                body = article.get("body", [])
                if isinstance(body, list) and body:
                    preview = body[0].get("content", "")[:50] + "..."
                else:
                    preview = str(body)[:50] + "..." if body else "본문 없음"

                print(f"  📰 [Thread-{sid1}][{i+1:2d}/10] {title}")
                print(f"      🏢 {press} | 📅 {published[:19] if published else 'N/A'}")
                print(f"      📝 {preview}")
                print()

            except Exception as e:
                print(f"    ❌ [Thread-{sid1}] 기사 파싱 오류: {e}")

        print(f"✅ [Thread-{sid1}] 섹션 {sid1} ({section_name}) 완료 - {len(local_seen_ids)}개 기사")
        return len(local_seen_ids)

    except Exception as e:
        print(f"💥 [Thread-{sid1}] 섹션 {sid1} 크롤링 오류: {e}")
        return 0

def test_multithread_crawling():
    """멀티스레드 크롤링 테스트"""
    print("🔥 멀티스레드 크롤링 테스트 시작!\n")

    today = datetime.today()
    reg_date = today.strftime("%Y%m%d")  # 오늘만

    # 스레드 안전한 전역 seen_ids
    global_seen_ids: set[str] = set()
    seen_ids_lock = threading.Lock()

    total_articles = 0

    print(f"📅 테스트 날짜: {reg_date}")
    print(f"📄 페이지 제한: {MAX_TEST_PAGES}페이지")
    print(f"🔗 기사 제한: 섹션당 최대 10개")
    print("-" * 60)

    # 6개 섹션 동시 실행
    sections = ["100", "101", "102", "103", "104", "105"]

    start_time = datetime.now()

    with ThreadPoolExecutor(max_workers=6, thread_name_prefix="TestCrawler") as executor:
        # 각 섹션별로 스레드 시작
        future_to_section = {
            executor.submit(test_crawl_section, sid1, reg_date, seen_ids_lock, global_seen_ids): sid1
            for sid1 in sections
        }

        # 완료된 스레드 결과 수집
        for future in as_completed(future_to_section):
            sid1 = future_to_section[future]
            try:
                article_count = future.result()
                total_articles += article_count
                section_name = SID1_TO_SECTION.get(sid1, "기타")
                print(f"🏁 섹션 {sid1} ({section_name}) 스레드 완료!")
            except Exception as e:
                print(f"💥 섹션 {sid1} 스레드 실행 오류: {e}")

    end_time = datetime.now()
    duration = (end_time - start_time).total_seconds()

    print("\n" + "=" * 60)
    print("🎉 멀티스레드 크롤링 테스트 완료!")
    print(f"⏱️  실행 시간: {duration:.2f}초")
    print(f"📊 총 수집 기사: {total_articles:,}개")
    print(f"🔗 고유 URL: {len(global_seen_ids):,}개")
    print(f"🧵 동시 스레드: 6개")

if __name__ == "__main__":
    test_multithread_crawling()