# BigQuery 연결 및 데이터 조회 기능을 테스트하는 스크립트
from datetime import datetime, timedelta
from loguru import logger

from app.config import PROJECT_ID, DEFAULT_DATASET, LOOKBACK_DAYS
from app.core.analytics.bq import get_client, get_latest_date, fetch_events

# 결과 출력 시 표시할 최대 행 수 (너무 많은 데이터가 출력되는 것을 방지)
SHOW = 50

def main():
    """
    BigQuery 연결 및 데이터 조회 기능을 테스트하는 메인 함수
    
    1. BigQuery 클라이언트 생성
    2. 최신 events_ 테이블 날짜 조회
    3. 지정된 기간 동안의 이벤트 데이터 조회
    4. 결과 통계 및 샘플 데이터 출력
    """
    # BigQuery 클라이언트 생성
    client = get_client()
    dataset = DEFAULT_DATASET

    # 데이터셋에서 가장 최신 events_ 테이블의 날짜 찾기
    latest = get_latest_date(client, dataset)
    if latest is None:
        logger.warning(f"`{PROJECT_ID}.{dataset}`에 events_* 테이블이 없습니다.")
        return

    # 조회 날짜 범위 계산 (최신 날짜부터 LOOKBACK_DAYS일 전까지)
    d = datetime.strptime(latest, "%Y%m%d").date()
    from_str = (d - timedelta(days=LOOKBACK_DAYS)).strftime("%Y%m%d")
    to_str = latest

    # 조회 정보 출력
    logger.info(f"project={PROJECT_ID}")
    logger.info(f"dataset={dataset}")
    logger.info(f"date-range: {from_str} ~ {to_str}")
    logger.info(f"include: news_* + toon_*")

    # 지정된 기간의 이벤트 데이터 조회
    df = fetch_events(client, dataset, from_str, to_str)
    logger.info(f"rows: {len(df)}")

    if df.empty:
        logger.info("결과 없음.")
        return

    # 이벤트 종류별 개수 집계 및 출력
    by_event = df["event_name"].value_counts().sort_index()
    logger.info("\n[COUNT by event_name]")
    for k, v in by_event.items():
        logger.info(f"{k:18s} : {v}")

    # 상위 N개 행의 상세 데이터 출력 (디버깅 및 데이터 확인용)
    logger.info("\n[WIDE VIEW: top rows]")
    logger.info(df.head(SHOW).to_string(index=False))

if __name__ == "__main__":
    # 스크립트가 직접 실행될 때만 main 함수 호출
    main()
