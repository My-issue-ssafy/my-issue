# 협업 필터링 (Collaborative Filtering) 모델을 학습하고 실시간 추천을 위한 모델을 저장하는 모듈
# ALS (Alternating Least Squares) 알고리즘을 사용하여 사용자-뉴스 상호작용 데이터로부터 학습된 모델을 생성

import os
import pickle
import json
from datetime import datetime, timedelta, timezone
from zoneinfo import ZoneInfo
from pathlib import Path

import numpy as np
import pandas as pd
import scipy.sparse as sp

from google.cloud import bigquery
from google.cloud.bigquery import ScalarQueryParameter, QueryJobConfig

# implicit 라이브러리의 ALS (Alternating Least Squares) 협업 필터링 알고리즘
from implicit.als import AlternatingLeastSquares

from app.config import PROJECT_ID, DEFAULT_DATASET, LOOKBACK_DAYS
from app.bq import get_client, get_latest_date

# BigQuery 뷰 대신 events_* 테이블 직접 쿼리 사용
# VIEW = f"`{PROJECT_ID}.{DEFAULT_DATASET}`.events_wide_v1"  # 비활성화: 뷰 대신 직접 쿼리

# 모델 저장 경로 설정
MODELS_DIR = Path(__file__).resolve().parent.parent / "models"
MODEL_PATH = MODELS_DIR / "als_model.pkl"
METADATA_PATH = MODELS_DIR / "model_metadata.json"

# 마지막 학습 날짜를 기록하는 파일 경로 (중복 학습 방지용)
STATE_PATH = os.path.join(os.path.dirname(__file__), "..", "last_trained.txt")

# 1) 오늘 날짜를 한국 시간 기준으로 YYYYMMDD 형태 문자열로 반환
def today_kst_str():
    """현재 날짜를 한국 시간대 기준으로 YYYYMMDD 형식 문자열로 반환"""
    kst = ZoneInfo("Asia/Seoul")
    return datetime.now(kst).strftime("%Y%m%d")

# 2) 오늘 날짜의 GA4 이벤트 테이블이 BigQuery에 생성되었는지 확인
def has_today_table(client: bigquery.Client, dataset: str) -> bool:
    """오늘 날짜의 events_YYYYMMDD 테이블이 존재하는지 확인"""
    latest = get_latest_date(client, dataset)  # 최신 events_ 테이블 날짜 조회
    return latest == today_kst_str()

# 3) 오늘 이미 학습했는지 확인하고 학습 완료 상태를 기록하는 함수들
def already_trained_today() -> bool:
    """오늘 이미 모델 학습을 완료했는지 확인 (중복 학습 방지)"""
    if not os.path.exists(STATE_PATH):
        return False
    with open(STATE_PATH, "r", encoding="utf-8") as f:
        last = f.read().strip()
    return last == today_kst_str()

def mark_trained_today():
    """오늘 모델 학습을 완료했다는 표시를 파일에 기록"""
    os.makedirs(os.path.dirname(STATE_PATH), exist_ok=True)
    with open(STATE_PATH, "w", encoding="utf-8") as f:
        f.write(today_kst_str())

# 4) 협업 필터링 모델 학습을 위한 사용자-뉴스 상호작용 데이터 추출
def fetch_interactions(client: bigquery.Client) -> pd.DataFrame:
    """
    최근 LOOKBACK_DAYS 일간의 사용자 상호작용 데이터를 추출하여 모델 학습용 데이터로 반환
    
    사용자의 뉴스 읽기, 스크롤, 북마크 등의 행동을 점수화하여
    사용자-뉴스 간의 선호도 강도를 산출합니다.
    """
    # 한국 시간 기준으로 날짜 범위 계산
    kst = ZoneInfo("Asia/Seoul")
    to_date = datetime.now(kst).date()  # 오늘 날짜
    from_date = to_date - timedelta(days=LOOKBACK_DAYS)  # LOOKBACK_DAYS일 전

    # 사용자 상호작용을 점수화하는 SQL 쿼리 (실제 테이블 구조에 맞게 수정)
    # 이벤트 종류별로 가중치를 다르게 적용하여 선호도 강도를 산출
    sql = f"""
    WITH base AS (
      -- 전처리된 events_* 테이블에서 직접 데이터 추출
      SELECT 
        user_id,
        news_id,
        event_name,
        ts,
        event_date,
        dwell_ms,
        scroll_pct,
        action,
        feed_source,
        from_source
      FROM `{PROJECT_ID}.{DEFAULT_DATASET}.events_*`
      WHERE user_id IS NOT NULL
        AND news_id IS NOT NULL
        AND (STARTS_WITH(event_name, 'news_') OR STARTS_WITH(event_name, 'toon_'))
    ), scored AS (
      -- 이벤트 종류에 따른 점수 부여
      SELECT
        user_id, news_id,
        CASE event_name
          WHEN 'news_bookmark' THEN IF(action='add', 3.0, NULL)  -- 북마크 추가: 최고 선호도 (3.0)
          WHEN 'news_click'     THEN 2.0                         -- 클릭: 높은 선호도 (2.0)
          WHEN 'news_view_end'  THEN IF(COALESCE(dwell_ms,0) >= 15000 OR COALESCE(scroll_pct,0) >= 70, 1.2, 0.6)  -- 읽기 완료: 체류시간/스크롤로 차등 점수
          WHEN 'toon_expand_news' THEN 1.0                       -- 툰 전개: 보통 선호도 (1.0)
          WHEN 'toon_positive'  THEN 0.8                         -- 긍정 반응: 약간의 선호도 (0.8)
          ELSE NULL
        END AS w
      FROM base
    )
    -- 사용자-뉴스별 점수 합계
    SELECT user_id, news_id, SUM(w) AS strength
    FROM scored
    WHERE w IS NOT NULL
    GROUP BY user_id, news_id
    HAVING strength > 0  -- 양수 점수만 유지
    """
    # 쿼리 매개변수 설정 (SQL 인젝션 방지) - STRING으로 설정  
    job_config = QueryJobConfig(
        query_parameters=[
            ScalarQueryParameter("from","STRING", from_date.strftime("%Y-%m-%d")),
            ScalarQueryParameter("to",  "STRING", to_date.strftime("%Y-%m-%d")),
        ]
    )
    
    # 디버깅: 생성된 SQL 출력
    print(f"[DEBUG] Generated SQL:")
    print(sql)
    print(f"[DEBUG] Date range: {from_date} to {to_date}")
    
    # 쿼리 실행 및 DataFrame으로 변환
    df = client.query(sql, job_config=job_config).result().to_dataframe(create_bqstorage_client=True)
    return df

# 5) ALS (Alternating Least Squares) 알고리즘으로 모델 학습 (실시간 추천을 위한 모델만 생성)
def train_model(df: pd.DataFrame):
    """
    사용자-뉴스 상호작용 데이터로부터 ALS 모델을 학습 (실시간 추천을 위해 모델만 저장)
    
    Args:
        df: 사용자-뉴스-선호도 데이터프레임 (user_id, news_id, strength)
    
    Returns:
        model: 학습된 ALS 모델
        users: 사용자 카테고리 인덱스
        items: 뉴스 카테고리 인덱스
        interaction_matrix: 사용자-아이템 상호작용 행렬
    """
    if df.empty:
        print("[경고] 빈 데이터프레임으로 인해 모델 학습을 건너뜁니다.")
        return None, None, None, None

    # 데이터 유효성 검사
    if 'user_id' not in df.columns or 'news_id' not in df.columns or 'strength' not in df.columns:
        print("[경고] 필수 컬럼(user_id, news_id, strength)이 누락되었습니다.")
        return None, None, None, None
        
    # null 값 제거
    df_clean = df.dropna(subset=['user_id', 'news_id', 'strength'])
    if df_clean.empty:
        print("[경고] null 값 제거 후 데이터가 비어있습니다.")
        return None, None, None, None

    # 뉔자/뉴스 ID를 숫자 인덱스로 변환 (효율적인 행렬 연산을 위해)
    users = df_clean["user_id"].astype("category")
    items = df_clean["news_id"].astype("category")
    
    # 빈 카테고리 체크
    if len(users.cat.categories) == 0 or len(items.cat.categories) == 0:
        print("[경고] 유효한 사용자나 아이템이 없습니다.")
        return None, None, None, None
        
    user_index = users.cat.codes.values  # 사용자 ID -> 숫자 인덱스
    item_index = items.cat.codes.values  # 뉴스 ID -> 숫자 인덱스

    # 사용자-뉴스 상호작용 희소 행렬 생성 (user x item 형태)
    # 대부분의 사용자가 전체 뉴스의 일부만 읽으므로 희소행렬이 효율적
    mat = sp.coo_matrix(
        (df_clean["strength"].values, (user_index, item_index)),  # (선호도 값, (사용자 인덱스, 뉴스 인덱스))
        shape=(users.cat.categories.size, items.cat.categories.size)  # 사용자 수 x 뉴스 수
    ).tocsr()  # Compressed Sparse Row 형태로 변환 (빠른 연산을 위해)

    # implicit 라이브러리의 ALS 모델 학습
    # factors: 잠재 요인 개수 (64차원), regularization: 과적합 방지 정규화, iterations: 반복 횟수
    model = AlternatingLeastSquares(factors=64, regularization=0.05, iterations=20)
    
    try:
        # implicit ALS는 item x user 형태를 기대하므로 행렬을 전치
        # 하지만 recommend 시에는 user x item 형태를 사용해야 하므로
        # 모델 학습과 추천 시 일관성을 맞춰야 함
        print(f"[INFO] 모델 학습 중 - 행렬 크기: {mat.shape} -> 전치 후: {mat.T.shape}")
        model.fit(mat.T)  # item x user 형태로 학습
        print(f"[INFO] 모델 학습 완료 - Users: {len(users.cat.categories)}, Items: {len(items.cat.categories)}")
        print(f"[INFO] 모델 내부 차원 - item_factors: {model.item_factors.shape}, user_factors: {model.user_factors.shape}")
    except Exception as e:
        print(f"[에러] 모델 학습 실패: {e}")
        return None, None, None, None

    # 실시간 추천에 필요한 데이터 반환 (미리 계산된 추천은 생성하지 않음)
    return model, users, items, mat

# 6) 학습된 모델을 파일 시스템에 저장 (실시간 추천을 위해)
def save_model(model, users, items, interaction_matrix):
    """
    학습된 ALS 모델과 관련 데이터를 파일로 저장
    
    Args:
        model: 학습된 ALS 모델
        users: 사용자 카테고리 인덱스
        items: 뉴스 카테고리 인덱스  
        interaction_matrix: 사용자-아이템 상호작용 행렬
    """
    # models 디렉토리 생성
    MODELS_DIR.mkdir(exist_ok=True)
    
    # 모델 데이터 패키징
    model_data = {
        'model': model,
        'user_categories': users.cat.categories,
        'item_categories': items.cat.categories,
        'interaction_matrix': interaction_matrix,
        'trained_at': datetime.now(timezone.utc),
        'train_date': today_kst_str(),
        'model_params': {
            'factors': model.factors,
            'regularization': model.regularization,
            'iterations': model.iterations
        }
    }
    
    # 모델을 pickle 파일로 저장
    with open(MODEL_PATH, 'wb') as f:
        pickle.dump(model_data, f)
    
    # 메타데이터를 JSON 파일로 저장 (디버깅 및 확인용)
    metadata = {
        'trained_at': model_data['trained_at'].isoformat(),
        'train_date': model_data['train_date'],
        'num_users': len(users.cat.categories),
        'num_items': len(items.cat.categories),
        'model_params': model_data['model_params'],
        'model_file': str(MODEL_PATH.name)
    }
    
    with open(METADATA_PATH, 'w') as f:
        json.dump(metadata, f, indent=2)
    
    print(f"[INFO] Model saved to {MODEL_PATH}")
    print(f"[INFO] Metadata saved to {METADATA_PATH}")
    print(f"[INFO] Users: {len(users.cat.categories)}, Items: {len(items.cat.categories)}")

# 7) 테스트용: 샘플 사용자에게 추천 생성 및 출력
def test_recommendations(model, users, items, interaction_matrix, num_test_users=3, topn=5):
    """
    테스트용으로 몇 명의 사용자에게 추천을 생성하고 결과를 출력
    
    Args:
        model: 학습된 ALS 모델
        users: 사용자 카테고리 인덱스
        items: 뉴스 카테고리 인덱스
        interaction_matrix: 사용자-아이템 상호작용 행렬
        num_test_users: 테스트할 사용자 수
        topn: 사용자당 추천할 아이템 수
    """
    print(f"\n[테스트] {num_test_users}명의 샘플 사용자에게 추천 생성 중...")
    print(f"[디버그] 전체 사용자 수: {len(users.cat.categories)}")
    print(f"[디버그] 전체 아이템 수: {len(items.cat.categories)}")
    print(f"[디버그] 상호작용 행렬 크기: {interaction_matrix.shape}")
    print(f"[디버그] 행렬 형태: users x items = {interaction_matrix.shape[0]} x {interaction_matrix.shape[1]}")
    
    # 랜덤하게 몇 명의 사용자 선택 (실제 사용자 인덱스 범위 내에서)
    import random
    total_users = len(users.cat.categories)
    # interaction_matrix 행 개수로 제한
    max_user_idx = interaction_matrix.shape[0] - 1
    
    # 안전성 검사: 유효한 사용자가 있는지 확인
    if total_users == 0 or max_user_idx < 0:
        print("[경고] 유효한 사용자가 없습니다.")
        return
        
    valid_range = min(total_users, max_user_idx + 1)
    if valid_range <= 0:
        print("[경고] 유효한 사용자 범위가 없습니다.")
        return
        
    actual_test_users = min(num_test_users, valid_range)
    test_user_indices = random.sample(range(valid_range), actual_test_users)
    
    for user_idx in test_user_indices:
        # 안전성 체크
        if user_idx >= len(users.cat.categories):
            print(f"[경고] 사용자 인덱스 {user_idx} >= 사용자 카테고리 수 {len(users.cat.categories)}")
            continue
        if user_idx >= interaction_matrix.shape[0]:
            print(f"[경고] 사용자 인덱스 {user_idx} >= 행렬 행 수 {interaction_matrix.shape[0]}")
            continue
            
        user_id = users.cat.categories[user_idx]
        
        try:
            print(f"[디버그] 사용자 처리 중 - 인덱스: {user_idx}, ID: {user_id}")
            print(f"[디버그] 사용자 행렬 범위 체크: user_idx={user_idx} < matrix_rows={interaction_matrix.shape[0]}? {user_idx < interaction_matrix.shape[0]}")
            
            # 해당 사용자에게 추천 생성 - implicit ALS recommend 메소드 올바른 사용법  
            try:
                # implicit 라이브러리 recommend 함수의 올바른 사용법:
                # user_items는 해당 사용자의 아이템 선호도를 나타내는 행 벡터 (1 x items)
                user_items = interaction_matrix[user_idx]  # 사용자 행렬에서 해당 사용자 행 추출 (1 x 200000)
                print(f"[디버그] user_items shape: {user_items.shape}")
                print(f"[디버그] user_items type: {type(user_items)}")
                print(f"[디버그] interaction_matrix shape: {interaction_matrix.shape}")
                
                # user_items가 올바른 형태인지 확인
                if hasattr(user_items, 'toarray'):
                    non_zero_count = (user_items.toarray() > 0).sum()
                    print(f"[디버그] user_items 비영값 개수: {non_zero_count}")
                
                rec_items, scores = model.recommend(
                    user_idx, 
                    user_items,  # item 관점에서의 사용자 선호도 벡터
                    N=topn, 
                    filter_already_liked_items=True
                )
                print(f"[디버그] 추천된 아이템 인덱스: {rec_items}")
                print(f"[디버그] 아이템 인덱스 범위: max={max(rec_items) if len(rec_items) > 0 else 'N/A'}, items_count={len(items.cat.categories)}")
            except (IndexError, ValueError) as idx_error:
                print(f"[경고] 사용자 {user_id} 인덱스/값 오류: {idx_error}")
                try:
                    # 대안 1: filter_already_liked_items=False로 다시 시도 (원래 행렬 사용)
                    user_items = interaction_matrix[user_idx]
                    rec_items, scores = model.recommend(
                        user_idx, 
                        user_items, 
                        N=topn, 
                        filter_already_liked_items=False
                    )
                except Exception as e:
                    print(f"[에러] 사용자 {user_id} 추천 생성 완전 실패: {e}")
                    # 대안 2: 빈 추천 결과로 처리
                    rec_items, scores = [], []
            
            print(f"\n--- User: {user_id} ---")
            
            # 사용자가 이미 상호작용한 아이템들 출력
            user_interactions = interaction_matrix[user_idx].nonzero()[1]
            if len(user_interactions) > 0:
                print("이전에 상호작용한 뉴스:")
                for i, item_idx in enumerate(user_interactions[:3]):  # 상위 3개만
                    # 아이템 인덱스 안전성 검사
                    if item_idx < 0 or item_idx >= len(items.cat.categories):
                        print(f"  - [잘못된 아이템 인덱스 {item_idx}]")
                        continue
                    try:
                        item_id = items.cat.categories[item_idx]
                        score = interaction_matrix[user_idx, item_idx]
                        print(f"  - {item_id} (score: {score:.2f})")
                    except IndexError as e:
                        print(f"  - [인덱스 에러 {item_idx}] - {e}")
                if len(user_interactions) > 3:
                    print(f"  ... and {len(user_interactions) - 3} more")
            
            # 추천 결과 출력
            print("추천 뉴스:")
            for rank, (item_idx, score) in enumerate(zip(rec_items, scores), 1):
                # 아이템 인덱스가 범위를 벗어나면 건너뛰기
                if item_idx < 0 or item_idx >= len(items.cat.categories):
                    print(f"  {rank}. [잘못된 아이템 인덱스 {item_idx}/{len(items.cat.categories)}] (점수: {score:.4f})")
                    continue
                try:
                    item_id = items.cat.categories[item_idx]
                    print(f"  {rank}. {item_id} (점수: {score:.4f})")
                except IndexError as e:
                    print(f"  {rank}. [인덱스 에러 {item_idx}] (점수: {score:.4f}) - {e}")
                
        except Exception as e:
            print(f"Error generating recommendations for {user_id}: {e}")
    
    print(f"\n[TEST] Recommendation test completed!")

def main():
    """
    협업 필터링 모델 학습 및 파일 저장 메인 함수
    
    1. 오늘 날짜의 GA4 데이터가 준비되었는지 확인
    2. 이미 오늘 학습을 완료했는지 확인 (중복 방지)
    3. 사용자 상호작용 데이터 추출 및 전처리
    4. ALS 모델 학습 (실시간 추천을 위해 모델만 생성)
    5. 학습된 모델을 파일 시스템에 저장
    6. 오늘 학습 완료 상태 기록
    """
    # BigQuery 클라이언트 초기화
    client = get_client()
    dataset = DEFAULT_DATASET

    # 1. 오늘 날짜의 GA4 이벤트 테이블이 존재하는지 확인
    # GA4 데이터는 일반적으로 1일 지연되어 BigQuery에 도착
    if not has_today_table(client, dataset):
        print("[SKIP] today's GA table not ready yet.")
        return

    # 2. 이미 오늘 모델 학습을 완료했는지 확인 (중복 학습 방지)
    if already_trained_today():
        print("[SKIP] already trained today.")
        return

    # 3. 협업 필터링에 사용할 사용자-뉴스 상호작용 데이터 추출
    df = fetch_interactions(client)
    print(f"[INFO] interactions rows={len(df)}")

    if df.empty:
        print("[SKIP] no interactions to train.")
        return

    # 4. ALS 모델 학습 (실시간 추천을 위해 미리 계산된 추천은 생성하지 않음)
    model, users, items, interaction_matrix = train_model(df)
    if model is None:
        print("[SKIP] no model generated.")
        return

    # 5. 학습된 모델과 관련 데이터를 파일 시스템에 저장
    save_model(model, users, items, interaction_matrix)
    print(f"[OK] Model training completed and saved")

    # 6. 테스트용: 샘플 사용자들에게 추천 생성 및 결과 출력
    test_recommendations(model, users, items, interaction_matrix, num_test_users=3, topn=5)

    # 7. 오늘 모델 학습 완료 상태를 파일에 기록
    mark_trained_today()

if __name__ == "__main__":
    # 스크립트가 직접 실행될 때만 main 함수 호출
    # 스케줄러나 다른 모듈에서 import 할 때는 실행되지 않음
    main()
