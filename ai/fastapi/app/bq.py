import os
from typing import Optional
import pandas as pd
from google.cloud import bigquery
from google.oauth2 import service_account

from app.config import PROJECT_ID, SERVICE_ACCOUNT_JSON

def get_client() -> bigquery.Client:
    """
    인증 우선순위:
      1) 환경변수 GOOGLE_APPLICATION_CREDENTIALS
      2) fastapi/service-account.json (기본)
      3) 로컬/런타임 ADC (있다면)
    """
    key_path = os.getenv("GOOGLE_APPLICATION_CREDENTIALS", SERVICE_ACCOUNT_JSON)
    if os.path.exists(key_path):
        creds = service_account.Credentials.from_service_account_file(key_path)
        return bigquery.Client(project=PROJECT_ID, credentials=creds)
    return bigquery.Client(project=PROJECT_ID)

def query_to_df(client: bigquery.Client, sql: str, job_config: Optional[bigquery.QueryJobConfig]=None) -> pd.DataFrame:
    """
    BigQuery → pandas DataFrame (Storage API 사용)
    """
    job = client.query(sql, job_config=job_config)
    return job.result().to_dataframe(create_bqstorage_client=True)

def get_latest_date(client: bigquery.Client, dataset: str) -> Optional[str]:
    """
    INFORMATION_SCHEMA에서 events_* 테이블 중 최신 YYYYMMDD 추출
    """
    sql = f"""
    SELECT REGEXP_EXTRACT(table_name, r'events_(\\d{{8}})') AS latest
    FROM `{PROJECT_ID}.{dataset}`.INFORMATION_SCHEMA.TABLES
    WHERE table_name LIKE 'events_%'
    ORDER BY latest DESC
    LIMIT 1
    """
    df = query_to_df(client, sql)
    if df.empty or df["latest"].isna().all():
        return None
    return str(df["latest"].iloc[0])

def fetch_events(client: bigquery.Client, dataset: str, from_str: str, to_str: str) -> pd.DataFrame:
    """
    기간[from_str, to_str]의 이벤트를 필요한 파라미터만 UNNEST하여 가져오기.
    news_id는 int/string 혼재 대비 → STRING 통일.
    """
    sql = f"""
    SELECT
      e.user_id,
      COALESCE(CAST(ep_item.value.int_value AS STRING), ep_item.value.string_value) AS news_id,
      e.event_name,
      TIMESTAMP_MICROS(e.event_timestamp) AS ts,
      SAFE_CAST(ep_dwell.value.int_value AS INT64) AS dwell_ms,
      ep_action.value.string_value AS action,
      ep_feed.value.string_value   AS feed_source,
      ep_from.value.string_value   AS from_source
    FROM `{PROJECT_ID}.{dataset}.events_*` e
    LEFT JOIN UNNEST(e.event_params) ep_item   ON ep_item.key  = 'news_id'
    LEFT JOIN UNNEST(e.event_params) ep_dwell  ON ep_dwell.key = 'dwell_ms'
    LEFT JOIN UNNEST(e.event_params) ep_action ON ep_action.key = 'action'
    LEFT JOIN UNNEST(e.event_params) ep_feed   ON ep_feed.key   = 'feed_source'
    LEFT JOIN UNNEST(e.event_params) ep_from   ON ep_from.key   = 'from_source'
    WHERE _TABLE_SUFFIX BETWEEN @from AND @to
      AND e.user_id IS NOT NULL
      AND (ep_item.value.int_value IS NOT NULL OR ep_item.value.string_value IS NOT NULL)
      AND e.event_name IN ('news_click','news_view_end','news_bookmark',
                           'news_impression','news_view_start')
    ORDER BY ts
    """
    job_config = bigquery.QueryJobConfig(query_parameters=[
        bigquery.ScalarQueryParameter("from","STRING", from_str),
        bigquery.ScalarQueryParameter("to",  "STRING", to_str),
    ])
    return query_to_df(client, sql, job_config)
