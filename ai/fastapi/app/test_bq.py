from datetime import datetime, timedelta
import argparse
import pandas as pd

from app.config import PROJECT_ID, DEFAULT_DATASET, LOOKBACK_DAYS
from app.bq import get_client, get_latest_date, fetch_events

def parse_args():
    p = argparse.ArgumentParser(description="BQ 연결/조회 테스트")
    p.add_argument("--dataset", default=DEFAULT_DATASET, help="BQ dataset name")
    p.add_argument("--from-date", dest="from_date", default=None, help="YYYYMMDD")
    p.add_argument("--to-date",   dest="to_date",   default=None, help="YYYYMMDD")
    p.add_argument("--days", type=int, default=LOOKBACK_DAYS, help="from이 없으면 (latest - days)")
    p.add_argument("--limit", type=int, default=5, help="표시할 행 수")
    return p.parse_args()

def main():
    args = parse_args()
    client = get_client()

    latest = get_latest_date(client, args.dataset)
    if latest is None:
        print(f"[WARN] `{PROJECT_ID}.{args.dataset}`에 events_* 테이블이 없습니다.")
        return

    to_str = args.to_date or latest
    if args.from_date:
        from_str = args.from_date
    else:
        d = datetime.strptime(to_str, "%Y%m%d").date()
        from_str = (d - timedelta(days=args.days)).strftime("%Y%m%d")

    print(f"[INFO] project={PROJECT_ID}")
    print(f"[INFO] dataset={args.dataset}")
    print(f"[INFO] date-range: {from_str} ~ {to_str}")

    df = fetch_events(client, args.dataset, from_str, to_str)
    print(f"[INFO] rows: {len(df)}")

    if not df.empty:
        print(df.head(args.limit).to_string(index=False))
    else:
        print("[INFO] 결과 없음.")

if __name__ == "__main__":
    main()
