# 프로젝트 전반에서 사용되는 설정 정보를 정의하는 모듈
from pathlib import Path

# Google Cloud Platform 프로젝트 ID
# BigQuery, 서비스 계정 등 모든 GCP 리소스가 속한 프로젝트
PROJECT_ID = "myssue-7e186"

# 기본 BigQuery 데이터셋 이름
# 개발/테스트용 샌드박스 데이터셋. 실제 운영 시에는 "analytics_504093405"로 변경
DEFAULT_DATASET = "analytics_504093405"

# 데이터 조회 시 최신 날짜 기준 과거 몇 일까지 조회할지 결정하는 기간
# 예: 14일이면 최신 날짜부터 14일 전까지의 데이터를 조회
LOOKBACK_DAYS = 14

# Google Cloud 서비스 계정 키 파일 경로
# fastapi 폴더 바로 아래에 위치한다고 가정하고 절대 경로로 생성
# 예: /path/to/project/fastapi/service-account.json
SERVICE_ACCOUNT_JSON = str(
    (Path(__file__).resolve().parent.parent / "service-account.json"))
