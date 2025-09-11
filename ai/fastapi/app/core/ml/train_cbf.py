# Content-Based Filtering (CBF) 추천 모델 학습 모듈
# 사용자 상호작용 데이터와 뉴스 임베딩을 활용한 콘텐츠 기반 추천 시스템

import os
import pickle
import json
import numpy as np
import pandas as pd
from datetime import datetime, timezone
from pathlib import Path
from typing import Dict, List, Tuple, Optional
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sklearn.metrics.pairwise import cosine_similarity
from sklearn.preprocessing import normalize

from app.config import PROJECT_ID, DEFAULT_DATASET, LOOKBACK_DAYS
from app.core.analytics.bq import get_client, get_latest_date
from app.core.ml.train_cf import fetch_interactions, today_kst_str, already_trained_today as cf_already_trained_today
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
        # 24시간 이내 뉴스만 조회 (한국 시간 기준)
        kst = ZoneInfo("Asia/Seoul")
        now = datetime.now(kst)
        yesterday = now - timedelta(days=1)
        
        print(f"[INFO] Loading news from {yesterday.strftime('%Y-%m-%d %H:%M')} to {now.strftime('%Y-%m-%d %H:%M')} (KST)")
        
        # 임베딩이 있고 24시간 이내 작성된 뉴스만 조회
        news_query = db.query(News).filter(
            News.embedding.isnot(None),
            News.created_at >= yesterday,
            News.created_at <= now
        ).all()
        
        if not news_query:
            print("[WARNING] No recent news with embeddings found in database")
            print(f"[DEBUG] Trying to load any news with embeddings for testing...")
            
            # 24시간 제한 없이 최신 1000개만 조회 (테스트용)
            news_query = db.query(News).filter(
                News.embedding.isnot(None)
            ).order_by(News.created_at.desc()).limit(1000).all()
            
            if not news_query:
                print("[ERROR] No news with embeddings found at all")
                return pd.DataFrame(), np.array([])
            else:
                print(f"[INFO] Loaded latest {len(news_query)} news for testing")
        
        # 뉴스 메타데이터 DataFrame 생성
        news_data = []
        embeddings = []
        
        for news in news_query:
            news_data.append({
                'news_id': news.id,
                'title': news.title,
                'category': news.category,
                'news_paper': news.news_paper,
                'created_at': news.created_at,
                'views': news.views
            })
            embeddings.append(news.embedding)
        
        news_df = pd.DataFrame(news_data)
        embeddings_matrix = np.array(embeddings)
        
        print(f"[INFO] Loaded {len(news_df)} news with embeddings")
        print(f"[INFO] Embedding dimension: {embeddings_matrix.shape[1] if len(embeddings_matrix) > 0 else 0}")
        print(f"[INFO] Date range: {news_df['created_at'].min()} ~ {news_df['created_at'].max()}")
        
        return news_df, embeddings_matrix
        
    except Exception as e:
        print(f"[ERROR] Failed to load news embeddings: {e}")
        return pd.DataFrame(), np.array([])
    finally:
        db.close()

def create_user_profiles(interactions_df: pd.DataFrame, news_df: pd.DataFrame, 
                        embeddings_matrix: np.ndarray) -> Tuple[Dict, Dict]:
    """
    사용자별 프로필 벡터 생성 (상호작용한 뉴스들의 가중 평균 임베딩)
    
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
    
    user_profiles = {}
    user_stats = {}
    
    for user_id in interactions_df['user_id'].unique():
        user_interactions = interactions_df[interactions_df['user_id'] == user_id]
        
        # 해당 사용자가 상호작용한 뉴스들의 임베딩과 강도
        user_embeddings = []
        user_weights = []
        
        for _, row in user_interactions.iterrows():
            news_id = row['news_id']
            strength = row['strength']
            
            if news_id in news_id_to_idx:
                idx = news_id_to_idx[news_id]
                user_embeddings.append(embeddings_matrix[idx])
                user_weights.append(strength)
        
        if user_embeddings:
            user_embeddings = np.array(user_embeddings)
            user_weights = np.array(user_weights)
            
            # 가중 평균으로 사용자 프로필 생성
            profile_vector = np.average(user_embeddings, axis=0, weights=user_weights)
            
            # 정규화
            profile_vector = normalize([profile_vector])[0]
            
            user_profiles[user_id] = profile_vector
            user_stats[user_id] = {
                'total_interactions': len(user_interactions),
                'avg_strength': user_interactions['strength'].mean(),
                'interacted_news_count': len(user_embeddings)
            }
    
    print(f"[INFO] Created profiles for {len(user_profiles)} users")
    return user_profiles, user_stats

def generate_recommendations(user_profiles: Dict, news_df: pd.DataFrame, 
                           embeddings_matrix: np.ndarray, top_k: int = 50) -> Dict:
    """
    사용자별 CBF 추천 생성 (코사인 유사도 기반)
    
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
    
    recommendations = {}
    
    # 뉴스 임베딩 정규화 (코사인 유사도 계산 최적화)
    normalized_embeddings = normalize(embeddings_matrix)
    
    for user_id, profile_vector in user_profiles.items():
        # 사용자 프로필과 모든 뉴스 간의 코사인 유사도 계산
        similarities = cosine_similarity([profile_vector], normalized_embeddings)[0]
        
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
    
    print(f"[INFO] Generated recommendations for {len(recommendations)} users")
    print(f"[INFO] Average recommendations per user: {top_k}")
    
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
        'total_users': len(user_profiles),
        'total_news': news_count,
        'embedding_dim': embedding_dim,
        'model_file': str(CBF_MODEL_PATH.name),
        'sample_users': list(recommendations.keys())[:5] if recommendations else []
    }
    
    with open(CBF_METADATA_PATH, 'w', encoding='utf-8') as f:
        json.dump(metadata, f, indent=2, ensure_ascii=False)
    
    print(f"[INFO] CBF model saved to {CBF_MODEL_PATH}")
    print(f"[INFO] CBF metadata saved to {CBF_METADATA_PATH}")
    print(f"[INFO] Users: {len(user_profiles)}, News: {news_count}")

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
        print("[WARNING] No recommendations to test")
        return
    
    print(f"\n{'='*60}")
    print(f"🎯 CBF 추천 결과 테스트 ({num_test_users}명의 샘플 사용자)")
    print(f"{'='*60}")
    
    # 랜덤하게 사용자 선택
    import random
    all_users = list(recommendations.keys())
    sample_users = random.sample(all_users, min(num_test_users, len(all_users)))
    
    for idx, user_id in enumerate(sample_users, 1):
        user_recs = recommendations[user_id]
        stats = user_stats.get(user_id, {})
        
        print(f"\n📊 [{idx}] User ID: {user_id}")
        print(f"   💬 총 상호작용: {stats.get('total_interactions', 0)}회")
        print(f"   🎯 상호작용 뉴스 수: {stats.get('interacted_news_count', 0)}개")
        print(f"   📈 평균 상호작용 강도: {stats.get('avg_strength', 0):.3f}")
        
        # 사용자가 실제로 상호작용한 뉴스들 표시
        user_interactions = interactions_df[interactions_df['user_id'] == user_id]
        if not user_interactions.empty:
            top_interactions = user_interactions.nlargest(3, 'strength')
            print(f"\n   📖 최고 상호작용 뉴스:")
            for i, (_, row) in enumerate(top_interactions.iterrows(), 1):
                print(f"      {i}. News ID {row['news_id']} (강도: {row['strength']:.2f})")
        
        # CBF 추천 결과
        print(f"\n   🤖 CBF 추천 뉴스 (코사인 유사도 기반):")
        for i, rec in enumerate(user_recs[:5], 1):  # 상위 5개
            title_short = rec['title'][:45] + "..." if len(rec['title']) > 45 else rec['title']
            category_display = f"[{rec['category']}]" if rec['category'] else "[기타]"
            print(f"      {i}. {category_display} {title_short}")
            print(f"          → 유사도: {rec['score']:.4f} | News ID: {rec['news_id']}")
        
        print(f"   {'─'*50}")
    
    # 전체 통계
    total_users = len(recommendations)
    avg_recs_per_user = sum(len(recs) for recs in recommendations.values()) / total_users
    
    print(f"\n📈 CBF 모델 전체 통계:")
    print(f"   • 프로필이 생성된 사용자: {total_users:,}명")
    print(f"   • 사용자당 평균 추천 수: {avg_recs_per_user:.1f}개")
    print(f"   • 유사도 범위: {min([min([r['score'] for r in recs]) for recs in recommendations.values()]):.4f} ~ {max([max([r['score'] for r in recs]) for recs in recommendations.values()]):.4f}")
    
    # 카테고리별 추천 분포 
    all_categories = {}
    for recs in recommendations.values():
        for rec in recs[:10]:  # 상위 10개만 분석
            cat = rec['category'] or '기타'
            all_categories[cat] = all_categories.get(cat, 0) + 1
    
    print(f"\n📊 추천 카테고리 분포 (상위 5개):")
    top_categories = sorted(all_categories.items(), key=lambda x: x[1], reverse=True)[:5]
    for cat, count in top_categories:
        percentage = (count / sum(all_categories.values())) * 100
        print(f"   • {cat}: {count:,}회 ({percentage:.1f}%)")
    
    print(f"\n✅ CBF 추천 테스트 완료!")

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
    print(f"[START] CBF 모델 학습 시작 - {datetime.now()}")
    
    # 1. 이미 오늘 CBF 학습을 완료했는지 확인
    if already_trained_cbf_today():
        print("[SKIP] CBF model already trained today.")
        return
    
    # 2. BigQuery에서 사용자 상호작용 데이터 로드 (CF와 동일한 데이터 사용)
    try:
        client = get_client()
        interactions_df = fetch_interactions(client)
        print(f"[INFO] Loaded {len(interactions_df)} user interactions")
        
        if interactions_df.empty:
            print("[SKIP] No interaction data available for CBF training")
            return
            
    except Exception as e:
        print(f"[ERROR] Failed to load interaction data: {e}")
        return
    
    # 3. PostgreSQL에서 뉴스 임베딩 데이터 로드
    news_df, embeddings_matrix = load_news_embeddings()
    
    if news_df.empty or embeddings_matrix.size == 0:
        print("[SKIP] No news embeddings available for CBF training")
        return
    
    # 4. 사용자별 프로필 벡터 생성
    user_profiles, user_stats = create_user_profiles(interactions_df, news_df, embeddings_matrix)
    
    if not user_profiles:
        print("[SKIP] No user profiles created for CBF training")
        return
    
    # 5. CBF 추천 생성
    recommendations = generate_recommendations(user_profiles, news_df, embeddings_matrix, top_k=50)
    
    if not recommendations:
        print("[SKIP] No recommendations generated")
        return
    
    # 6. CBF 모델 저장
    save_cbf_model(recommendations, user_profiles, user_stats, 
                   len(news_df), embeddings_matrix.shape[1])
    
    # 7. 테스트용 추천 결과 출력 (train_cf 스타일)
    test_cbf_recommendations(recommendations, user_stats, interactions_df, num_test_users=3)
    
    # 8. 오늘 CBF 학습 완료 상태 기록
    mark_cbf_trained_today()
    
    print(f"[SUCCESS] CBF 모델 학습 완료 - {datetime.now()}")

if __name__ == "__main__":
    main()