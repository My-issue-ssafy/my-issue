# 추천 서비스 모듈
# CF(Collaborative Filtering)와 CBF(Content-Based Filtering) 모델을 로드하여 통합 추천 제공

import os
import pickle
import numpy as np
from pathlib import Path
from typing import List, Dict, Optional, Tuple
from datetime import datetime

# 모델 저장 경로
MODELS_DIR = Path(__file__).resolve().parent.parent.parent / "models"
CF_MODEL_PATH = MODELS_DIR / "als_model.pkl"
CBF_MODEL_PATH = MODELS_DIR / "cbf_model.pkl"

class RecommendationService:
    """CF와 CBF 모델을 활용한 통합 추천 서비스"""
    
    def __init__(self):
        self.cf_model_data = None
        self.cbf_model_data = None
        self.cf_loaded_at = None
        self.cbf_loaded_at = None
    
    def load_cf_model(self) -> bool:
        """
        저장된 CF 모델을 로드
        
        Returns:
            bool: 로드 성공 여부
        """
        try:
            if not CF_MODEL_PATH.exists():
                print(f"[WARNING] CF 모델 파일이 없습니다: {CF_MODEL_PATH}")
                return False
            
            with open(CF_MODEL_PATH, 'rb') as f:
                self.cf_model_data = pickle.load(f)
            
            self.cf_loaded_at = datetime.now()
            print(f"[INFO] CF 모델 로드 완료 - Users: {len(self.cf_model_data['user_categories'])}, Items: {len(self.cf_model_data['item_categories'])}")
            return True
            
        except Exception as e:
            print(f"[ERROR] CF 모델 로드 실패: {e}")
            return False
    
    def load_cbf_model(self) -> bool:
        """
        저장된 CBF 모델을 로드
        
        Returns:
            bool: 로드 성공 여부
        """
        try:
            if not CBF_MODEL_PATH.exists():
                print(f"[WARNING] CBF 모델 파일이 없습니다: {CBF_MODEL_PATH}")
                return False
            
            with open(CBF_MODEL_PATH, 'rb') as f:
                self.cbf_model_data = pickle.load(f)
            
            self.cbf_loaded_at = datetime.now()
            print(f"[INFO] CBF 모델 로드 완료 - Users: {len(self.cbf_model_data['user_profiles'])}, Recommendations: {len(self.cbf_model_data['recommendations'])}")
            return True
            
        except Exception as e:
            print(f"[ERROR] CBF 모델 로드 실패: {e}")
            return False
    
    def get_cf_recommendations(self, user_id: str, top_k: int = 50) -> List[Dict]:
        """
        CF 모델에서 사용자 추천 생성
        
        Args:
            user_id: 사용자 ID
            top_k: 추천할 아이템 수
            
        Returns:
            List[Dict]: 추천 뉴스 리스트 [{'news_id': int, 'score': float, 'method': 'CF'}, ...]
        """
        if not self.cf_model_data:
            print("[WARNING] CF 모델이 로드되지 않았습니다")
            return []
        
        try:
            model = self.cf_model_data['model']
            user_categories = self.cf_model_data['user_categories']
            item_categories = self.cf_model_data['item_categories']
            interaction_matrix = self.cf_model_data['interaction_matrix']
            
            # 사용자 ID가 학습 데이터에 있는지 확인
            if user_id not in user_categories:
                print(f"[WARNING] 사용자 {user_id}가 CF 모델에 없습니다")
                return []
            
            # 사용자 인덱스 찾기
            user_idx = list(user_categories).index(user_id)
            
            # implicit ALS 모델의 차원 문제 해결
            # 학습 시 transpose된 행렬을 사용했으므로 직접 계산
            actual_user_embeddings = model.item_factors  # 실제 사용자 임베딩
            actual_item_embeddings = model.user_factors  # 실제 아이템 임베딩
            
            user_embedding = actual_user_embeddings[user_idx]  # 해당 사용자 임베딩
            
            # 모든 아이템에 대한 점수 계산
            scores_all = actual_item_embeddings.dot(user_embedding)
            
            # 이미 상호작용한 아이템들은 제외
            user_items = interaction_matrix[user_idx]
            if hasattr(user_items, 'toarray'):
                user_items_dense = user_items.toarray().flatten()
            else:
                user_items_dense = user_items
            
            interacted_items = np.where(user_items_dense > 0)[0]
            scores_all[interacted_items] = -np.inf
            
            # 상위 N개 아이템 선택
            top_indices = np.argsort(-scores_all)[:top_k]
            
            recommendations = []
            for idx in top_indices:
                if idx < len(item_categories):
                    news_id = item_categories[idx]
                    score = float(scores_all[idx])
                    if score > -np.inf:  # 유효한 점수만
                        recommendations.append({
                            'news_id': int(news_id),
                            'score': score,
                            'method': 'CF'
                        })
            
            print(f"[INFO] CF 추천 생성: 사용자 {user_id} -> {len(recommendations)}개")
            return recommendations
            
        except Exception as e:
            print(f"[ERROR] CF 추천 생성 실패: {e}")
            return []
    
    def get_cbf_recommendations(self, user_id: str, top_k: int = 50) -> List[Dict]:
        """
        CBF 모델에서 사용자 추천 가져오기
        
        Args:
            user_id: 사용자 ID
            top_k: 추천할 아이템 수
            
        Returns:
            List[Dict]: 추천 뉴스 리스트 [{'news_id': int, 'score': float, 'method': 'CBF', 'title': str, 'category': str}, ...]
        """
        if not self.cbf_model_data:
            print("[WARNING] CBF 모델이 로드되지 않았습니다")
            return []
        
        try:
            recommendations_data = self.cbf_model_data['recommendations']
            
            # 사용자 ID가 CBF 추천에 있는지 확인
            if user_id not in recommendations_data:
                print(f"[WARNING] 사용자 {user_id}가 CBF 모델에 없습니다")
                return []
            
            user_recommendations = recommendations_data[user_id][:top_k]
            
            # CBF 추천에 method 필드 추가
            recommendations = []
            for rec in user_recommendations:
                recommendations.append({
                    'news_id': rec['news_id'],
                    'score': rec['score'],
                    'method': 'CBF',
                    'title': rec.get('title', ''),
                    'category': rec.get('category', '')
                })
            
            print(f"[INFO] CBF 추천 가져오기: 사용자 {user_id} -> {len(recommendations)}개")
            return recommendations
            
        except Exception as e:
            print(f"[ERROR] CBF 추천 가져오기 실패: {e}")
            return []
    
    def get_hybrid_recommendations(self, user_id: str, cf_count: int = 50, cbf_count: int = 50) -> Dict:
        """
        CF와 CBF를 결합한 하이브리드 추천
        
        Args:
            user_id: 사용자 ID
            cf_count: CF 추천 수
            cbf_count: CBF 추천 수
            
        Returns:
            Dict: {
                'user_id': str,
                'total_recommendations': int,
                'cf_recommendations': List[Dict],
                'cbf_recommendations': List[Dict],
                'combined_recommendations': List[Dict],
                'timestamp': str
            }
        """
        result = {
            'user_id': user_id,
            'total_recommendations': 0,
            'cf_recommendations': [],
            'cbf_recommendations': [],
            'combined_recommendations': [],
            'timestamp': datetime.now().isoformat()
        }
        
        # 모델들이 로드되지 않았다면 로드 시도
        if not self.cf_model_data:
            self.load_cf_model()
        if not self.cbf_model_data:
            self.load_cbf_model()
        
        # CF 추천 가져오기
        cf_recs = self.get_cf_recommendations(user_id, cf_count)
        result['cf_recommendations'] = cf_recs
        
        # CBF 추천 가져오기
        cbf_recs = self.get_cbf_recommendations(user_id, cbf_count)
        result['cbf_recommendations'] = cbf_recs
        
        # 결합된 추천 리스트 생성 (중복 제거)
        combined = []
        seen_news_ids = set()
        
        # CF 추천을 먼저 추가
        for rec in cf_recs:
            if rec['news_id'] not in seen_news_ids:
                combined.append(rec)
                seen_news_ids.add(rec['news_id'])
        
        # CBF 추천을 추가 (중복 제거)
        for rec in cbf_recs:
            if rec['news_id'] not in seen_news_ids:
                combined.append(rec)
                seen_news_ids.add(rec['news_id'])
        
        result['combined_recommendations'] = combined
        result['total_recommendations'] = len(combined)
        
        print(f"[INFO] 하이브리드 추천 완료: 사용자 {user_id} -> CF:{len(cf_recs)}, CBF:{len(cbf_recs)}, 총:{len(combined)}개")
        
        return result

# 전역 추천 서비스 인스턴스
recommendation_service = RecommendationService()