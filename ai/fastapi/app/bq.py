# BigQuery 연결 및 데이터 조회를 위한 유틸리티 모듈
import os
from typing import Optional
import pandas as pd
from google.cloud import bigquery
from google.oauth2 import service_account

# 프로젝트 설정 정보 가져오기
from app.config import PROJECT_ID, SERVICE_ACCOUNT_JSON

def get_client() -> bigquery.Client:
    """
    BigQuery 클라이언트를 생성하여 반환합니다.
    
    인증 우선순위:
      1) 환경변수 GOOGLE_APPLICATION_CREDENTIALS에 지정된 서비스 계정 파일
      2) fastapi/service-account.json (기본 경로)
      3) 로컬/런타임 ADC (Application Default Credentials) - 있다면
    
    Returns:
        bigquery.Client: 인증된 BigQuery 클라이언트 객체
    """
    # 환경변수에서 서비스 계정 파일 경로를 가져오거나, 기본 경로 사용
    key_path = os.getenv("GOOGLE_APPLICATION_CREDENTIALS", SERVICE_ACCOUNT_JSON)
    
    # 서비스 계정 파일이 존재하는 경우 해당 파일로 인증
    if os.path.exists(key_path):
        creds = service_account.Credentials.from_service_account_file(key_path)
        return bigquery.Client(project=PROJECT_ID, credentials=creds)
    
    # 서비스 계정 파일이 없으면 기본 인증 방식 사용 (ADC 등)
    return bigquery.Client(project=PROJECT_ID)

def query_to_df(client: bigquery.Client, sql: str, job_config: Optional[bigquery.QueryJobConfig]=None) -> pd.DataFrame:
    """
    BigQuery SQL 쿼리를 실행하여 결과를 pandas DataFrame으로 반환합니다.
    
    Args:
        client: BigQuery 클라이언트 객체
        sql: 실행할 SQL 쿼리 문자열
        job_config: 쿼리 작업 설정 (매개변수, 옵션 등)
    
    Returns:
        pd.DataFrame: 쿼리 결과를 담은 DataFrame
    """
    # SQL 쿼리 실행
    job = client.query(sql, job_config=job_config)
    # 결과를 DataFrame으로 변환 (BigQuery Storage API 사용으로 빠른 읽기)
    return job.result().to_dataframe(create_bqstorage_client=True)

def get_latest_date(client: bigquery.Client, dataset: str) -> Optional[str]:
    """
    데이터셋에서 가장 최신 날짜의 events_ 테이블을 찾아 날짜를 반환합니다.
    
    Google Analytics 4는 일별로 events_YYYYMMDD 형태의 테이블을 생성하므로,
    INFORMATION_SCHEMA를 조회하여 가장 최신 날짜를 찾습니다.
    
    Args:
        client: BigQuery 클라이언트 객체
        dataset: 조회할 데이터셋 이름
    
    Returns:
        Optional[str]: 최신 날짜 (YYYYMMDD 형식) 또는 None (테이블이 없는 경우)
    """
    # INFORMATION_SCHEMA를 통해 events_로 시작하는 테이블들을 조회
    sql = f"""
    SELECT REGEXP_EXTRACT(table_name, r'events_(\\d{{8}})') AS latest
    FROM `{PROJECT_ID}.{dataset}`.INFORMATION_SCHEMA.TABLES
    WHERE table_name LIKE 'events_%'
    ORDER BY latest DESC
    LIMIT 1
    """
    df = query_to_df(client, sql)
    
    # 결과가 없거나 null인 경우 None 반환
    if df.empty or df["latest"].isna().all():
        return None
    
    # 첫 번째 결과를 문자열로 반환
    return str(df["latest"].iloc[0])

def fetch_events(client: bigquery.Client, dataset: str, from_str: str, to_str: str) -> pd.DataFrame:
    """
    지정된 기간의 사용자 이벤트 데이터를 조회하여 반환합니다.
    
    Google Analytics 4에서 수집된 이벤트를 분석하기 쉬운 wide 형태로 변환합니다.
    news_* 와 toon_* 이벤트를 모두 포함하며, 각 이벤트의 매개변수들을 컬럼으로 풀어냅니다.
    
    Args:
        client: BigQuery 클라이언트 객체
        dataset: 조회할 데이터셋 이름
        from_str: 조회 시작일 (YYYYMMDD 형식)
        to_str: 조회 종료일 (YYYYMMDD 형식)
    
    Returns:
        pd.DataFrame: 이벤트 데이터 (user_id, news_id, event_name, ts, dwell_ms, scroll_pct 등)
    """
    sql = f"""
    SELECT
      SAFE_CAST(e.user_id AS INT64) AS user_id,                      -- 사용자 ID
      COALESCE(ep_item.value.int_value, SAFE_CAST(ep_item.value.string_value AS INT64)) AS news_id, -- 뉴스 ID
      e.event_name,                                                   -- 이벤트 이름
      TIMESTAMP_MICROS(e.event_timestamp) AS ts,                      -- 이벤트 발생 시간
      SAFE_CAST(ep_dwell.value.int_value AS INT64)  AS dwell_ms,      -- 체류 시간 (밀리초)
      SAFE_CAST(ep_scroll.value.int_value AS INT64) AS scroll_pct,    -- 스크롤 비율
      ep_action.value.string_value                  AS action,        -- 액션 종류 (add, remove 등)
      ep_feed.value.string_value                    AS feed_source,   -- 피드 소스
      ep_from.value.string_value                    AS from_source    -- 유입 소스
    FROM `{PROJECT_ID}.{dataset}.events_*` e
    -- 이벤트 매개변수를 컬럼으로 풀어내기 위한 LEFT JOIN
    LEFT JOIN UNNEST(e.event_params) ep_item   ON ep_item.key  = 'news_id'
    LEFT JOIN UNNEST(e.event_params) ep_dwell  ON ep_dwell.key = 'dwell_ms'
    LEFT JOIN UNNEST(e.event_params) ep_scroll ON ep_scroll.key = 'scroll_pct'
    LEFT JOIN UNNEST(e.event_params) ep_action ON ep_action.key = 'action'
    LEFT JOIN UNNEST(e.event_params) ep_feed   ON ep_feed.key   = 'feed_source'
    LEFT JOIN UNNEST(e.event_params) ep_from   ON ep_from.key   = 'from_source'
    WHERE _TABLE_SUFFIX BETWEEN @from AND @to                         -- 날짜 범위 필터
      AND e.user_id IS NOT NULL                                       -- 사용자 ID 필수
      AND (ep_item.value.int_value IS NOT NULL OR ep_item.value.string_value IS NOT NULL) -- 뉴스 ID 필수
      AND (STARTS_WITH(e.event_name, 'news_') OR STARTS_WITH(e.event_name, 'toon_')) -- news_ 또는 toon_ 이벤트만
    ORDER BY ts
    """
    # 쿼리 매개변수 설정 (SQL 인젝션 방지)
    job_config = bigquery.QueryJobConfig(query_parameters=[
        bigquery.ScalarQueryParameter("from","STRING", from_str),
        bigquery.ScalarQueryParameter("to",  "STRING", to_str),
    ])
    return query_to_df(client, sql, job_config)

