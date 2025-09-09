from pathlib import Path

# GCP 프로젝트 ID
PROJECT_ID = "myssue-7e186"

# 기본 데이터셋 (더미/샌드박스). 실데이터 전환 시 "analytics_504093405" 로 변경
DEFAULT_DATASET = "analytics_504093405_sandbox"

# latest 기준 조회 기간(일)
LOOKBACK_DAYS = 14

# service-account.json 경로 (fastapi 폴더 바로 아래에 있다고 가정)
SERVICE_ACCOUNT_JSON = str((Path(__file__).resolve().parent.parent / "service-account.json"))
