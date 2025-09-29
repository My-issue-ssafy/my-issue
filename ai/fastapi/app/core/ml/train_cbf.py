# Content-Based Filtering (CBF) 추천 모델 학습 모듈
# 사용자 상호작용 데이터와 뉴스 임베딩을 활용한 콘텐츠 기반 추천 시스템

import os
import pickle
import json
import numpy as np
import pandas as pd
import psutil
from datetime import datetime, timezone
from pathlib import Path
from typing import Dict, List, Tuple, Optional
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sklearn.metrics.pairwise import cosine_similarity
from sklearn.preprocessing import normalize
from tqdm import tqdm
from loguru import logger

from app.config import PROJECT_ID, DEFAULT_DATASET, LOOKBACK_DAYS
from app.core.analytics.bq import get_client, get_latest_date
from app.core.ml.train_cf import today_kst_str
from app.db.models.news import News
from app.utils.config import settings

# 모델 저장 경로 설정
MODELS_DIR = Path(__file__).resolve().parent.parent.parent / "models"
CBF_MODEL_PATH = MODELS_DIR / "cbf_model.pkl"
CBF_METADATA_PATH = MODELS_DIR / "cbf_metadata.json"

# 마지막 CBF 학습 날짜를 기록하는 파일 경로
CBF_STATE_PATH = os.path.join(os.path.dirname(__file__), "..", "..", "..", "cbf_last_trained.txt")

# 데이터베이스 연결 설정
engine = create_engine(settings.DATABASE_URL, pool_pre_ping=True)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

def already_trained_cbf_today() -> bool:
    """오늘 이미 CBF 모델 학습을 완료했는지 확인"""
    if not os.path.exists(CBF_STATE_PATH):
        return False
    with open(CBF_STATE_PATH, "r", encoding="utf-8") as f:
        last = f.read().strip()
    return last == today_kst_str()

def mark_cbf_trained_today():
    """오늘 CBF 모델 학습을 완료했다는 표시를 파일에 기록"""
    os.makedirs(os.path.dirname(CBF_STATE_PATH), exist_ok=True)
    with open(CBF_STATE_PATH, "w", encoding="utf-8") as f:
        f.write(today_kst_str())

def load_news_embeddings() -> Tuple[pd.DataFrame, np.ndarray]:
    """
    PostgreSQL에서 최근 24시간 뉴스 데이터와 임베딩 벡터를 로드
    
    Returns:
        news_df: 뉴스 메타데이터 DataFrame
        embeddings_matrix: 뉴스 임베딩 행렬 (n_news x embedding_dim)
    """
    from datetime import datetime, timedelta
    from zoneinfo import ZoneInfo
    
    db = SessionLocal()
    try:
        # 24시간 이내의 임베딩이 있는 뉴스 조회
        kst = ZoneInfo("Asia/Seoul")
        now_kst = datetime.now(kst)
        yesterday_kst = now_kst - timedelta(hours=24)

        logger.info(f"최근 24시간 임베딩 뉴스 로드 중 ({yesterday_kst.strftime('%Y-%m-%d %H:%M')} ~ {now_kst.strftime('%Y-%m-%d %H:%M')})")

        # 24시간 이내 + 임베딩이 있는 뉴스 조회
        news_query = db.query(News).filter(
            News.embedding.isnot(None),
            News.created_at >= yesterday_kst
        ).order_by(News.created_at.desc()).all()
        
        if not news_query:
            logger.error("임베딩이 있는 뉴스를 찾을 수 없습니다")
            return pd.DataFrame(), np.array([])
        
        # 뉴스 메타데이터 DataFrame 생성 (최적화된 방식)
        logger.info(f"{len(news_query)}개 뉴스 임베딩 처리 중 (최적화됨)...")

        # 리스트 컴프리헨션으로 한 번에 처리 (메모리 효율성 개선)
        news_data = [
            {
                'news_id': news.id,
                'title': news.title,
                'category': news.category,
                'news_paper': news.news_paper,
                'created_at': news.created_at,
                'views': news.views
            }
            for news in news_query
        ]

        # 임베딩도 한 번에 추출 (tqdm 제거로 오버헤드 감소)
        embeddings = [news.embedding for news in news_query]

        news_df = pd.DataFrame(news_data)
        embeddings_matrix = np.array(embeddings)
        
        logger.info(f"{len(news_df)}개 임베딩 뉴스 로드 완료")
        logger.info(f"임베딩 차원: {embeddings_matrix.shape[1] if len(embeddings_matrix) > 0 else 0}")
        logger.info(f"날짜 범위: {news_df['created_at'].min()} ~ {news_df['created_at'].max()}")

        # 메모리 사용량 출력
        memory_mb = embeddings_matrix.nbytes / (1024 * 1024)
        logger.info(f"임베딩 행렬 메모리 사용량: {memory_mb:.1f}MB")
        process = psutil.Process()
        total_memory_mb = process.memory_info().rss / (1024 * 1024)
        logger.info(f"현재 프로세스 총 메모리: {total_memory_mb:.1f}MB")
        
        return news_df, embeddings_matrix
        
    except Exception as e:
        logger.error(f"뉴스 임베딩 로드 실패: {e}")
        return pd.DataFrame(), np.array([])
    finally:
        db.close()

def create_user_profiles(interactions_df: pd.DataFrame, news_df: pd.DataFrame,
                        embeddings_matrix: np.ndarray) -> Tuple[Dict, Dict]:
    """
    사용자별 프로필 벡터 생성 (최적화된 가중 평균 임베딩)

    Args:
        interactions_df: 사용자-뉴스 상호작용 데이터
        news_df: 뉴스 메타데이터
        embeddings_matrix: 뉴스 임베딩 행렬

    Returns:
        user_profiles: {user_id: profile_vector}
        user_stats: {user_id: {'total_interactions': int, 'avg_strength': float}}
    """
    if embeddings_matrix.size == 0:
        return {}, {}

    # 뉴스 ID를 인덱스로 매핑
    news_id_to_idx = {news_id: idx for idx, news_id in enumerate(news_df['news_id'].values)}

    # 상호작용 데이터에 임베딩 인덱스 추가 (벡터화를 위한 전처리)
    logger.info("벡터화를 위한 상호작용 데이터 전처리 중...")
    interactions_df_copy = interactions_df.copy()
    interactions_df_copy['embedding_idx'] = interactions_df_copy['news_id'].map(news_id_to_idx)

    # 임베딩이 없는 뉴스 제거
    valid_interactions = interactions_df_copy.dropna(subset=['embedding_idx'])
    valid_interactions['embedding_idx'] = valid_interactions['embedding_idx'].astype(int)

    user_profiles = {}
    user_stats = {}

    unique_users = valid_interactions['user_id'].unique()
    logger.info(f"{len(unique_users)}명 사용자 프로필 생성 중 (최적화됨)...")

    # 사용자별로 그룹화하여 한번에 처리
    grouped = valid_interactions.groupby('user_id')

    for user_id, user_data in tqdm(grouped, desc="Creating user profiles"):
        # 해당 사용자의 모든 임베딩과 가중치를 한 번에 추출
        embedding_indices = user_data['embedding_idx'].values
        weights = user_data['strength'].values

        if len(embedding_indices) > 0:
            # 벡터화된 임베딩 추출 (fancy indexing)
            user_embeddings = embeddings_matrix[embedding_indices]

            # 가중 평균으로 사용자 프로필 생성
            profile_vector = np.average(user_embeddings, axis=0, weights=weights)

            # 정규화
            profile_vector = normalize([profile_vector])[0]

            user_profiles[user_id] = profile_vector
            user_stats[user_id] = {
                'total_interactions': len(user_data),
                'avg_strength': user_data['strength'].mean(),
                'interacted_news_count': len(embedding_indices)
            }

    logger.info(f"{len(user_profiles)}명 사용자 프로필 생성 완료")
    return user_profiles, user_stats

def generate_recommendations(user_profiles: Dict, news_df: pd.DataFrame,
                           embeddings_matrix: np.ndarray, top_k: int = 50) -> Dict:
    """
    사용자별 CBF 추천 생성 (벡터화 연산으로 최적화된 코사인 유사도 기반)

    Args:
        user_profiles: 사용자 프로필 벡터들
        news_df: 뉴스 메타데이터
        embeddings_matrix: 뉴스 임베딩 행렬
        top_k: 사용자별 추천할 뉴스 수

    Returns:
        recommendations: {user_id: [{'news_id': int, 'score': float, 'title': str}, ...]}
    """
    if embeddings_matrix.size == 0:
        return {}

    # 뉴스 임베딩 정규화 (한 번만 수행)
    logger.info(f"임베딩 행렬 정규화 중 ({embeddings_matrix.shape})...")
    normalized_embeddings = normalize(embeddings_matrix)

    # 사용자 프로필을 행렬로 변환 (벡터화 연산을 위해)
    user_ids = list(user_profiles.keys())
    user_profiles_matrix = np.array(list(user_profiles.values()))

    logger.info(f"{len(user_profiles)}명 사용자 추천 생성 중 (벡터화 연산)...")

    # 모든 사용자-뉴스 간 유사도를 한 번에 계산 (벡터화 연산)
    all_similarities = cosine_similarity(user_profiles_matrix, normalized_embeddings)

    # 각 사용자별로 추천 결과 생성
    recommendations = {}
    for i, user_id in enumerate(tqdm(user_ids, desc="Processing user recommendations")):
        similarities = all_similarities[i]

        # 유사도 순으로 정렬
        similar_indices = np.argsort(-similarities)[:top_k]

        user_recommendations = []
        for idx in similar_indices:
            news_id = news_df.iloc[idx]['news_id']
            score = similarities[idx]
            title = news_df.iloc[idx]['title']
            category = news_df.iloc[idx]['category']

            user_recommendations.append({
                'news_id': int(news_id),
                'score': float(score),
                'title': title,
                'category': category
            })

        recommendations[user_id] = user_recommendations

    logger.info(f"{len(recommendations)}명 사용자 추천 생성 완료")
    logger.info(f"사용자당 평균 추천 수: {top_k}개")

    return recommendations

def save_cbf_model(recommendations: Dict, user_profiles: Dict, user_stats: Dict, 
                   news_count: int, embedding_dim: int):
    """
    CBF 모델 결과를 파일로 저장
    
    Args:
        recommendations: 사용자별 추천 결과
        user_profiles: 사용자 프로필 벡터들
        user_stats: 사용자별 통계 정보
        news_count: 전체 뉴스 수
        embedding_dim: 임베딩 차원
    """
    # models 디렉토리 생성
    MODELS_DIR.mkdir(exist_ok=True)
    
    # CBF 모델 데이터 패키징
    cbf_model_data = {
        'recommendations': recommendations,
        'user_profiles': user_profiles,
        'user_stats': user_stats,
        'trained_at': datetime.now(timezone.utc),
        'train_date': today_kst_str(),
        'model_params': {
            'total_users': len(user_profiles),
            'total_news': news_count,
            'embedding_dim': embedding_dim,
            'top_k_recommendations': 50,
            'similarity_metric': 'cosine'
        }
    }
    
    # 모델을 pickle 파일로 저장
    with open(CBF_MODEL_PATH, 'wb') as f:
        pickle.dump(cbf_model_data, f)
    
    # 메타데이터를 JSON 파일로 저장 (디버깅 및 확인용)
    metadata = {
        'trained_at': cbf_model_data['trained_at'].isoformat(),
        'train_date': cbf_model_data['train_date'],
        'total_users': int(len(user_profiles)),
        'total_news': int(news_count),
        'embedding_dim': int(embedding_dim),
        'model_file': str(CBF_MODEL_PATH.name),
        'sample_users': [int(uid) for uid in list(recommendations.keys())[:5]] if recommendations else []
    }
    
    with open(CBF_METADATA_PATH, 'w', encoding='utf-8') as f:
        json.dump(metadata, f, indent=2, ensure_ascii=False)
    
    logger.info(f"CBF 모델 저장 완료: {CBF_MODEL_PATH}")
    logger.info(f"CBF 메타데이터 저장 완료: {CBF_METADATA_PATH}")
    logger.info(f"사용자: {len(user_profiles)}명, 뉴스: {news_count}개")

def load_interactions_from_csv() -> pd.DataFrame:
    """
    data/ 디렉토리의 interactions_*.csv 파일들을 모두 읽어서 통합된 상호작용 데이터 반환
    
    Returns:
        interactions_df: 사용자-뉴스 상호작용 DataFrame (user_id, news_id, strength)
    """
    import glob
    
    # data 디렉토리의 모든 interactions_*.csv 파일 찾기
    data_dir = Path(__file__).resolve().parent.parent.parent.parent / "data"
    csv_pattern = str(data_dir / "interactions_*.csv")
    csv_files = glob.glob(csv_pattern)
    
    # 14일치 → 1일치로 변경: 가장 최신 파일만 사용
    if csv_files:
        # 파일명으로 정렬해서 가장 최신 파일 하나만 선택 (interactions_YYYYMMDD.csv 형식)
        csv_files = [sorted(csv_files)[-1]]
        logger.info("최신 CSV 파일 1개만 사용 (1일치 데이터)")
    
    if not csv_files:
        logger.error(f"{data_dir}에서 상호작용 CSV 파일을 찾을 수 없습니다")
        return pd.DataFrame()
    
    logger.info(f"{len(csv_files)}개 상호작용 CSV 파일 발견:")
    for f in sorted(csv_files):
        file_size = Path(f).stat().st_size / (1024*1024)  # MB
        logger.info(f"  - {Path(f).name} ({file_size:.1f}MB)")
    
    # 모든 CSV 파일을 읽어서 통합
    dfs = []
    total_rows = 0
    
    logger.info("CSV 파일 로드 중...")
    for csv_file in tqdm(sorted(csv_files), desc="Loading CSV files"):
        try:
            logger.info(f"Loading {Path(csv_file).name}...")
            df = pd.read_csv(csv_file, encoding='utf-8-sig')
            # 컬럼명 정리 (BOM 제거)
            df.columns = df.columns.str.strip()

            if not df.empty and all(col in df.columns for col in ['user_id', 'news_id', 'strength']):
                dfs.append(df)
                total_rows += len(df)
                logger.info(f"OK ({len(df):,} interactions)")
            else:
                logger.error("Missing required columns")
                
        except Exception as e:
            logger.error(f"{e}")
    
    if not dfs:
        logger.error("CSV 파일에서 유효한 상호작용 데이터를 로드할 수 없습니다")
        return pd.DataFrame()
    
    # 모든 데이터를 통합하고 중복 제거
    logger.info("데이터 통합 및 중복 제거 중...")
    combined_df = pd.concat(dfs, ignore_index=True)
    
    # 같은 user_id, news_id 조합이 있으면 strength 값을 합계 (상호작용 누적)
    logger.info("user_id와 news_id로 그룹화 중...")
    combined_df = combined_df.groupby(['user_id', 'news_id'], as_index=False)['strength'].sum()
    
    logger.info(f"총 상호작용 로드: {total_rows:,}개")
    logger.info(f"중복 제거 후: {len(combined_df):,}개")
    logger.info(f"고유 사용자: {combined_df['user_id'].nunique():,}명")
    logger.info(f"고유 뉴스: {combined_df['news_id'].nunique():,}개")
    logger.info(f"상호작용 강도 범위: {combined_df['strength'].min():.2f} ~ {combined_df['strength'].max():.2f}")
    
    return combined_df

def test_cbf_recommendations(recommendations: Dict, user_stats: Dict, interactions_df: pd.DataFrame, num_test_users: int = 3):
    """
    CBF 추천 결과 테스트 출력 (train_cf 스타일)
    
    Args:
        recommendations: 사용자별 추천 결과
        user_stats: 사용자별 통계 정보
        interactions_df: 원본 상호작용 데이터
        num_test_users: 테스트할 사용자 수
    """
    if not recommendations:
        logger.warning("테스트할 추천 결과가 없습니다")
        return
    
    logger.info(f"\n{'='*60}")
    logger.info(f"CBF 추천 결과 테스트 ({num_test_users}명의 샘플 사용자)")
    logger.info(f"{'='*60}")
    
    # 랜덤하게 사용자 선택
    import random
    all_users = list(recommendations.keys())
    sample_users = random.sample(all_users, min(num_test_users, len(all_users)))
    
    for idx, user_id in enumerate(sample_users, 1):
        user_recs = recommendations[user_id]
        stats = user_stats.get(user_id, {})
        
        logger.info(f"\n[{idx}] User ID: {user_id}")
        logger.info(f"   총 상호작용: {stats.get('total_interactions', 0)}회")
        logger.info(f"   상호작용 뉴스 수: {stats.get('interacted_news_count', 0)}개")
        logger.info(f"   평균 상호작용 강도: {stats.get('avg_strength', 0):.3f}")
        
        # 사용자가 실제로 상호작용한 뉴스들 표시
        user_interactions = interactions_df[interactions_df['user_id'] == user_id]
        if not user_interactions.empty:
            top_interactions = user_interactions.nlargest(3, 'strength')
            logger.info(f"\n   최고 상호작용 뉴스:")
            for i, (_, row) in enumerate(top_interactions.iterrows(), 1):
                logger.info(f"      {i}. News ID {row['news_id']} (강도: {row['strength']:.2f})")
        
        # CBF 추천 결과
        logger.info(f"\n   CBF 추천 뉴스 (코사인 유사도 기반):")
        for i, rec in enumerate(user_recs[:5], 1):  # 상위 5개
            title_short = rec['title'][:45] + "..." if len(rec['title']) > 45 else rec['title']
            category_display = f"[{rec['category']}]" if rec['category'] else "[기타]"
            logger.info(f"      {i}. {category_display} {title_short}")
            logger.info(f"          → 유사도: {rec['score']:.4f} | News ID: {rec['news_id']}")
        
        logger.info(f"   {'─'*50}")
    
    # 전체 통계
    total_users = len(recommendations)
    avg_recs_per_user = sum(len(recs) for recs in recommendations.values()) / total_users
    
    logger.info(f"\nCBF 모델 전체 통계:")
    logger.info(f"   • 프로필이 생성된 사용자: {total_users:,}명")
    logger.info(f"   • 사용자당 평균 추천 수: {avg_recs_per_user:.1f}개")
    logger.info(f"   • 유사도 범위: {min([min([r['score'] for r in recs]) for recs in recommendations.values()]):.4f} ~ {max([max([r['score'] for r in recs]) for recs in recommendations.values()]):.4f}")
    
    # 카테고리별 추천 분포 
    all_categories = {}
    for recs in recommendations.values():
        for rec in recs[:10]:  # 상위 10개만 분석
            cat = rec['category'] or '기타'
            all_categories[cat] = all_categories.get(cat, 0) + 1
    
    logger.info(f"\n추천 카테고리 분포 (상위 5개):")
    top_categories = sorted(all_categories.items(), key=lambda x: x[1], reverse=True)[:5]
    for cat, count in top_categories:
        percentage = (count / sum(all_categories.values())) * 100
        logger.info(f"   • {cat}: {count:,}회 ({percentage:.1f}%)")
    
    logger.info(f"\nCBF 추천 테스트 완료!")

def main():
    """
    CBF 추천 모델 학습 및 저장 메인 함수
    
    1. 오늘 이미 CBF 학습을 완료했는지 확인
    2. BigQuery에서 사용자 상호작용 데이터 로드
    3. PostgreSQL에서 뉴스 임베딩 데이터 로드
    4. 사용자별 프로필 벡터 생성
    5. 코사인 유사도 기반 CBF 추천 생성
    6. 모델 결과를 파일 시스템에 저장
    """
    logger.info(f"CBF 모델 학습 시작 - {datetime.now()}")
    
    # 1. 이미 오늘 CBF 학습을 완료했는지 확인
    if already_trained_cbf_today():
        logger.info("오늘 이미 CBF 모델 학습을 완료했습니다.")
        return
    
    # 2. CSV 파일에서 사용자 상호작용 데이터 로드
    try:
        interactions_df = load_interactions_from_csv()
        logger.info(f"CSV 파일에서 {len(interactions_df)}개 사용자 상호작용 로드 완료")
        
        if interactions_df.empty:
            logger.info("CBF 학습에 사용할 상호작용 데이터가 없습니다")
            return
            
    except Exception as e:
        logger.error(f"CSV에서 상호작용 데이터 로드 실패: {e}")
        return
    
    # 3. PostgreSQL에서 뉴스 임베딩 데이터 로드
    news_df, embeddings_matrix = load_news_embeddings()
    
    if news_df.empty or embeddings_matrix.size == 0:
        logger.info("CBF 학습에 사용할 뉴스 임베딩이 없습니다")
        return
    
    # 4. 사용자별 프로필 벡터 생성
    user_profiles, user_stats = create_user_profiles(interactions_df, news_df, embeddings_matrix)
    
    if not user_profiles:
        logger.info("CBF 학습을 위한 사용자 프로필이 생성되지 않았습니다")
        return
    
    # 5. CBF 추천 생성
    recommendations = generate_recommendations(user_profiles, news_df, embeddings_matrix, top_k=50)
    
    if not recommendations:
        logger.info("추천 결과가 생성되지 않았습니다")
        return
    
    # 6. CBF 모델 저장
    save_cbf_model(recommendations, user_profiles, user_stats, 
                   len(news_df), embeddings_matrix.shape[1])
    
    # 7. 테스트용 추천 결과 출력 (train_cf 스타일)
    test_cbf_recommendations(recommendations, user_stats, interactions_df, num_test_users=3)
    
    # 8. 오늘 CBF 학습 완료 상태 기록
    mark_cbf_trained_today()
    
    logger.info(f"CBF 모델 학습 완료 - {datetime.now()}")

if __name__ == "__main__":
    main()