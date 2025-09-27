#!/usr/bin/env python3
"""
BigQuery에서 날짜별 이벤트 데이터를 CSV로 추출하는 스크립트
"""

import os
from datetime import datetime, timedelta
from zoneinfo import ZoneInfo
from pathlib import Path

import pandas as pd
from google.cloud import bigquery
from google.cloud.bigquery import ScalarQueryParameter, QueryJobConfig
from loguru import logger

from app.core.analytics.bq import get_client, fetch_events
from app.config import PROJECT_ID, DEFAULT_DATASET

def export_daily_events_to_csv(target_date: str, output_dir: str = "data"):
    """
    특정 날짜의 이벤트 데이터를 CSV로 추출
    
    Args:
        target_date: YYYYMMDD 형식의 날짜 (예: "20240910")
        output_dir: CSV 파일을 저장할 디렉토리
    """
    # 출력 디렉토리 생성
    output_path = Path(output_dir)
    output_path.mkdir(exist_ok=True)
    
    # BigQuery 클라이언트 생성
    client = get_client()
    
    # 원시 이벤트 데이터 추출 SQL
    raw_events_sql = f"""
    SELECT
      SAFE_CAST(e.user_id AS INT64) AS user_id,
      COALESCE(ep_item.value.int_value, SAFE_CAST(ep_item.value.string_value AS INT64)) AS news_id,
      e.event_name,
      TIMESTAMP_MICROS(e.event_timestamp) AS ts,
      SAFE_CAST(ep_dwell.value.int_value AS INT64) AS dwell_ms,
      SAFE_CAST(ep_scroll.value.int_value AS INT64) AS scroll_pct,
      ep_action.value.string_value AS action,
      ep_feed.value.string_value AS feed_source,
      ep_from.value.string_value AS from_source
    FROM `{PROJECT_ID}.{DEFAULT_DATASET}.events_{target_date}` e
    LEFT JOIN UNNEST(e.event_params) ep_item ON ep_item.key = 'news_id'
    LEFT JOIN UNNEST(e.event_params) ep_dwell ON ep_dwell.key = 'dwell_ms'
    LEFT JOIN UNNEST(e.event_params) ep_scroll ON ep_scroll.key = 'scroll_pct'
    LEFT JOIN UNNEST(e.event_params) ep_action ON ep_action.key = 'action'
    LEFT JOIN UNNEST(e.event_params) ep_feed ON ep_feed.key = 'feed_source'
    LEFT JOIN UNNEST(e.event_params) ep_from ON ep_from.key = 'from_source'
    WHERE e.user_id IS NOT NULL
      AND (ep_item.value.int_value IS NOT NULL OR ep_item.value.string_value IS NOT NULL)
      AND (STARTS_WITH(e.event_name, 'news_') OR STARTS_WITH(e.event_name, 'toon_'))
    ORDER BY ts
    """
    
    # 점수화된 상호작용 데이터 추출 SQL
    scored_interactions_sql = f"""
    WITH base AS (
      SELECT 
        SAFE_CAST(user_id AS INT64) AS user_id,
        COALESCE(ep_item.value.int_value, SAFE_CAST(ep_item.value.string_value AS INT64)) AS news_id,
        event_name,
        TIMESTAMP_MICROS(event_timestamp) AS ts,
        SAFE_CAST(ep_dwell.value.int_value AS INT64) AS dwell_ms,
        SAFE_CAST(ep_scroll.value.int_value AS INT64) AS scroll_pct,
        ep_action.value.string_value AS action
      FROM `{PROJECT_ID}.{DEFAULT_DATASET}.events_{target_date}` e
      LEFT JOIN UNNEST(e.event_params) ep_item ON ep_item.key = 'news_id'
      LEFT JOIN UNNEST(e.event_params) ep_dwell ON ep_dwell.key = 'dwell_ms'
      LEFT JOIN UNNEST(e.event_params) ep_scroll ON ep_scroll.key = 'scroll_pct'
      LEFT JOIN UNNEST(e.event_params) ep_action ON ep_action.key = 'action'
      WHERE user_id IS NOT NULL
        AND (ep_item.value.int_value IS NOT NULL OR ep_item.value.string_value IS NOT NULL)
        AND (STARTS_WITH(event_name, 'news_') OR STARTS_WITH(event_name, 'toon_'))
    ), scored AS (
      SELECT
        user_id, news_id, event_name,
        CASE event_name
          WHEN 'news_bookmark' THEN IF(action='add', 3.0, NULL)
          WHEN 'news_click'     THEN 2.0
          WHEN 'news_view_end'  THEN IF(COALESCE(dwell_ms,0) >= 15000 OR COALESCE(scroll_pct,0) >= 70, 1.2, 0.6)
          WHEN 'toon_expand_news' THEN 1.0
          WHEN 'toon_positive'  THEN 0.8
          ELSE NULL
        END AS score,
        dwell_ms, scroll_pct, action, ts
      FROM base
    )
    SELECT user_id, news_id, event_name, score, dwell_ms, scroll_pct, action, ts
    FROM scored
    WHERE score IS NOT NULL
    ORDER BY ts
    """
    
    logger.info(f"{target_date} 날짜 데이터 추출 시작...")
    
    try:
        # 1. 기존 fetch_events 함수 활용해서 데이터 추출 (해당 날짜만)
        logger.info("이벤트 데이터 추출 중...")
        raw_df = fetch_events(client, DEFAULT_DATASET, target_date, target_date)
        raw_csv_path = output_path / f"events_{target_date}.csv"
        raw_df.to_csv(raw_csv_path, index=False, encoding='utf-8-sig')
        logger.success(f"이벤트 데이터 저장: {raw_csv_path} ({len(raw_df)}행)")
        
        # 2. 데이터 통계 출력
        logger.info(f"\n=== {target_date} 데이터 요약 ===")
        logger.info(f"총 이벤트 수: {len(raw_df):,}")
        if not raw_df.empty:
            logger.info(f"유니크 사용자 수: {raw_df['user_id'].nunique():,}")
            logger.info(f"유니크 뉴스 수: {raw_df['news_id'].nunique():,}")
            logger.info(f"이벤트별 분포:")
            for event, count in raw_df['event_name'].value_counts().head(10).items():
                logger.info(f"  - {event}: {count:,}")
        
        return raw_df
        
    except Exception as e:
        logger.error(f"데이터 추출 실패: {e}")
        return None

def export_recent_days(days: int = 7, output_dir: str = "data"):
    """
    최근 N일간의 데이터를 모두 CSV로 추출
    
    Args:
        days: 추출할 일수
        output_dir: CSV 파일을 저장할 디렉토리
    """
    kst = ZoneInfo("Asia/Seoul")
    today = datetime.now(kst).date()
    
    for i in range(days):
        target_date = today - timedelta(days=i)
        date_str = target_date.strftime("%Y%m%d")
        
        logger.info(f"\n{'='*50}")
        logger.info(f"날짜: {date_str}")
        logger.info('='*50)
        
        raw_df, scored_df = export_daily_events_to_csv(date_str, output_dir)
        
        if raw_df is None:
            logger.warning(f"{date_str} 데이터 없음 또는 추출 실패")
            continue

if __name__ == "__main__":
    import argparse
    
    parser = argparse.ArgumentParser(description="BigQuery에서 날짜별 이벤트 데이터 CSV 추출")
    parser.add_argument("--date", type=str, help="추출할 날짜 (YYYYMMDD 형식, 예: 20240910)")
    parser.add_argument("--days", type=int, default=1, help="최근 N일간 추출 (기본: 1일)")
    parser.add_argument("--output", type=str, default="data", help="출력 디렉토리 (기본: data)")
    
    args = parser.parse_args()
    
    if args.date:
        # 특정 날짜 추출
        export_daily_events_to_csv(args.date, args.output)
    else:
        # 최근 N일간 추출
        export_recent_days(args.days, args.output)