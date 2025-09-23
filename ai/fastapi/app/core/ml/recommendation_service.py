# 추천 서비스 모듈
# CF(Collaborative Filtering)와 CBF(Content-Based Filtering) 모델을 로드하여 통합 추천 제공

import os
import pickle
import json
import numpy as np
from pathlib import Path
from typing import List, Dict, Optional, Tuple
from datetime import datetime
from loguru import logger
from dateutil import parser

# 모델 저장 경로
MODELS_DIR = Path(__file__).resolve().parent.parent.parent / "models"
CF_MODEL_PATH = MODELS_DIR / "als_model.pkl"
CBF_MODEL_PATH = MODELS_DIR / "cbf_model.pkl"
CF_METADATA_PATH = MODELS_DIR / "model_metadata.json"
CBF_METADATA_PATH = MODELS_DIR / "cbf_metadata.json"

class RecommendationService:
    """CF와 CBF 모델을 활용한 통합 추천 서비스"""
    
    def __init__(self):
        self.cf_model_data = None
        self.cbf_model_data = None
        self.cf_loaded_at = None
        self.cbf_loaded_at = None
        self.cf_trained_at = None
        self.cbf_trained_at = None

    def _load_cf_trained_time(self):
        """CF 모델의 학습 시간을 메타데이터에서 로드"""
        try:
            if CF_METADATA_PATH.exists():
                with open(CF_METADATA_PATH, 'r', encoding='utf-8') as f:
                    metadata = json.load(f)
                    trained_at_str = metadata.get('trained_at')
                    if trained_at_str:
                        self.cf_trained_at = parser.parse(trained_at_str)
                        logger.debug(f"CF 모델 학습 시간: {self.cf_trained_at}")
        except Exception as e:
            logger.warning(f"CF 모델 학습 시간 로드 실패: {e}")

    def _load_cbf_trained_time(self):
        """CBF 모델의 학습 시간을 메타데이터에서 로드"""
        try:
            if CBF_METADATA_PATH.exists():
                with open(CBF_METADATA_PATH, 'r', encoding='utf-8') as f:
                    metadata = json.load(f)
                    trained_at_str = metadata.get('trained_at')
                    if trained_at_str:
                        self.cbf_trained_at = parser.parse(trained_at_str)
                        logger.debug(f"CBF 모델 학습 시간: {self.cbf_trained_at}")
            elif hasattr(self.cbf_model_data, 'get') and self.cbf_model_data:
                # CBF 모델 데이터에서 직접 로드 시도
                trained_at = self.cbf_model_data.get('trained_at')
                if trained_at:
                    if isinstance(trained_at, str):
                        self.cbf_trained_at = parser.parse(trained_at)
                    else:
                        self.cbf_trained_at = trained_at
                    logger.debug(f"CBF 모델 학습 시간 (모델 데이터): {self.cbf_trained_at}")
        except Exception as e:
            logger.warning(f"CBF 모델 학습 시간 로드 실패: {e}")

    def get_latest_model_trained_time(self) -> Optional[datetime]:
        """두 모델 중 가장 최신 학습 시간 반환"""
        times = []
        if self.cf_trained_at:
            times.append(self.cf_trained_at)
        if self.cbf_trained_at:
            times.append(self.cbf_trained_at)

        if times:
            latest = max(times)
            logger.debug(f"최신 모델 학습 시간: {latest}")
            return latest
        return None
    
    def load_cf_model(self) -> bool:
        """
        저장된 CF 모델을 로드
        
        Returns:
            bool: 로드 성공 여부
        """
        try:
            if not CF_MODEL_PATH.exists():
                logger.warning(f"CF 모델 파일이 없습니다: {CF_MODEL_PATH}")
                return False
            
            with open(CF_MODEL_PATH, 'rb') as f:
                self.cf_model_data = pickle.load(f)
            
            self.cf_loaded_at = datetime.now()
            # CF 모델 학습 시간 로드
            self._load_cf_trained_time()
            logger.info(f"CF 모델 로드 완료 - Users: {len(self.cf_model_data['user_categories'])}, Items: {len(self.cf_model_data['item_categories'])}")
            return True
            
        except Exception as e:
            logger.error(f"CF 모델 로드 실패: {e}")
            return False
    
    def load_cbf_model(self) -> bool:
        """
        저장된 CBF 모델을 로드
        
        Returns:
            bool: 로드 성공 여부
        """
        try:
            if not CBF_MODEL_PATH.exists():
                logger.warning(f"CBF 모델 파일이 없습니다: {CBF_MODEL_PATH}")
                return False
            
            with open(CBF_MODEL_PATH, 'rb') as f:
                self.cbf_model_data = pickle.load(f)
            
            self.cbf_loaded_at = datetime.now()
            # CBF 모델 학습 시간 로드
            self._load_cbf_trained_time()
            logger.info(f"CBF 모델 로드 완료 - Users: {len(self.cbf_model_data['user_profiles'])}, Recommendations: {len(self.cbf_model_data['recommendations'])}")
            return True
            
        except Exception as e:
            logger.error(f"CBF 모델 로드 실패: {e}")
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
            logger.warning("CF 모델이 로드되지 않았습니다")
            return []
        
        try:
            model = self.cf_model_data['model']
            user_categories = self.cf_model_data['user_categories']
            item_categories = self.cf_model_data['item_categories']
            interaction_matrix = self.cf_model_data['interaction_matrix']
            
            # 사용자 ID가 학습 데이터에 있는지 확인
            if user_id not in user_categories:
                logger.warning(f"사용자 {user_id}가 CF 모델에 없습니다")
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
            
            logger.info(f"CF 추천 생성: 사용자 {user_id} -> {len(recommendations)}개")
            return recommendations
            
        except Exception as e:
            logger.error(f"CF 추천 생성 실패: {e}")
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
            logger.warning("CBF 모델이 로드되지 않았습니다")
            return []
        
        try:
            recommendations_data = self.cbf_model_data['recommendations']
            
            # 사용자 ID가 CBF 추천에 있는지 확인
            if user_id not in recommendations_data:
                logger.warning(f"사용자 {user_id}가 CBF 모델에 없습니다")
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
            
            logger.info(f"CBF 추천 가져오기: 사용자 {user_id} -> {len(recommendations)}개")
            return recommendations
            
        except Exception as e:
            logger.error(f"CBF 추천 가져오기 실패: {e}")
            return []
    
    def diversify_by_category(self, recommendations: List[Dict], max_per_category: int = 15) -> List[Dict]:
        """
        카테고리별로 추천 수를 제한하여 다양성 확보
        
        Args:
            recommendations: 원본 추천 리스트
            max_per_category: 카테고리별 최대 추천 수
            
        Returns:
            다양성이 적용된 추천 리스트
        """
        category_counts = {}
        diversified = []
        
        for rec in recommendations:
            category = rec.get('category', '기타')
            if category_counts.get(category, 0) < max_per_category:
                diversified.append(rec)
                category_counts[category] = category_counts.get(category, 0) + 1
        
        logger.info(f"카테고리 다양성 적용: {len(recommendations)} -> {len(diversified)}개")
        return diversified
    
    def add_balanced_perspectives(self, recommendations: List[Dict], ratio: float = 0.05) -> List[Dict]:
        """
        균형잡힌 반대 관점 뉴스를 소량 추가
        
        Args:
            recommendations: 기존 추천 리스트
            ratio: 반대 관점 추가 비율 (0.05 = 5%)
            
        Returns:
            반대 관점이 추가된 추천 리스트
        """
        if not self.cbf_model_data:
            return recommendations
        
        try:
            # 현재 추천에서 제외할 뉴스 ID들
            current_news_ids = {rec['news_id'] for rec in recommendations}
            
            # CBF 전체 추천에서 반대 관점 후보 찾기 (낮은 유사도지만 극단적이지 않은)
            all_cbf_recs = self.cbf_model_data['recommendations'].get(user_id, [])
            opposite_candidates = [
                rec for rec in all_cbf_recs 
                if (-0.2 <= rec['score'] <= 0.1 and 
                    rec['news_id'] not in current_news_ids)
            ]
            
            # 추가할 반대 관점 뉴스 수 계산
            target_count = max(1, int(len(recommendations) * ratio))
            opposite_to_add = opposite_candidates[:target_count]
            
            # 반대 관점 뉴스에 method 표시
            for rec in opposite_to_add:
                rec['method'] = 'CBF_OPPOSITE'
            
            result = recommendations + opposite_to_add
            logger.info(f"반대 관점 추가: {len(opposite_to_add)}개 ({ratio*100:.1f}%)")
            return result
            
        except Exception as e:
            logger.warning(f"반대 관점 추가 실패: {e}")
            return recommendations
    
    def normalize_scores(self, recommendations: List[Dict]) -> List[Dict]:
        """
        CF와 CBF 점수를 Min-Max 정규화로 0-1 범위로 통일
        
        Args:
            recommendations: CF와 CBF가 섞인 추천 리스트
            
        Returns:
            정규화된 점수가 추가된 추천 리스트
        """
        cf_recs = [r for r in recommendations if r['method'] == 'CF']
        cbf_recs = [r for r in recommendations if r['method'] in ['CBF', 'CBF_OPPOSITE']]
        
        # CF 점수 정규화 (0-1)
        if cf_recs:
            cf_scores = [r['score'] for r in cf_recs]
            cf_min, cf_max = min(cf_scores), max(cf_scores)
            if cf_max > cf_min:
                for rec in cf_recs:
                    rec['normalized_score'] = (rec['score'] - cf_min) / (cf_max - cf_min)
            else:
                for rec in cf_recs:
                    rec['normalized_score'] = 0.5
        
        # CBF 점수 정규화 (이미 0-1 범위이지만 재정규화)
        if cbf_recs:
            cbf_scores = [r['score'] for r in cbf_recs]
            cbf_min, cbf_max = min(cbf_scores), max(cbf_scores)
            if cbf_max > cbf_min:
                for rec in cbf_recs:
                    rec['normalized_score'] = (rec['score'] - cbf_min) / (cbf_max - cbf_min)
            else:
                for rec in cbf_recs:
                    rec['normalized_score'] = rec['score']  # 이미 0-1
        
        logger.info(f"점수 정규화 완료: CF {len(cf_recs)}개, CBF {len(cbf_recs)}개")
        return recommendations

    def calculate_time_weight(self, news_created_at, current_time):
        """
        뉴스 생성 시간을 기반으로 시간 가중치 계산
        
        Args:
            news_created_at: 뉴스 생성 시간
            current_time: 현재 시간
            
        Returns:
            시간 가중치 (0.4 ~ 1.0)
        """
        if news_created_at is None:
            return 0.5  # 시간 정보 없으면 중간값
        
        try:
            # 시간 차이 계산 (시간 단위)
            if isinstance(news_created_at, str):
                from dateutil import parser
                news_time = parser.parse(news_created_at)
            else:
                news_time = news_created_at
                
            time_diff_hours = (current_time - news_time).total_seconds() / 3600
            
            # 시간 가중치 계산
            if time_diff_hours <= 24:
                return 1.0      # 24시간 이내 최고 점수
            elif time_diff_hours <= 72:
                return 0.8      # 3일 이내
            elif time_diff_hours <= 168:
                return 0.6      # 1주일 이내
            else:
                return 0.4      # 1주일 이후
                
        except Exception as e:
            logger.warning(f"시간 가중치 계산 실패: {e}")
            return 0.5

    def weighted_shuffle(self, recommendations: List[Dict]) -> List[Dict]:
        """
        점수 + 시간 가중치 기반 셔플
        높은 점수와 최신 뉴스일수록 앞에 올 확률 높음
        
        Args:
            recommendations: 정규화된 점수가 있는 추천 리스트
            
        Returns:
            가중 셔플된 추천 리스트
        """
        import random
        import numpy as np
        
        if len(recommendations) <= 1:
            return recommendations
        
        current_time = datetime.now()
        
        # 각 추천의 최종 가중 점수 계산
        weighted_recs = []
        for rec in recommendations:
            # 정규화된 추천 점수 (70%)
            base_score = rec.get('normalized_score', rec.get('score', 0.5))
            
            # 시간 가중치 (30%)
            time_weight = self.calculate_time_weight(
                rec.get('created_at'), current_time
            )
            
            # 최종 가중 점수
            final_score = (base_score * 0.7) + (time_weight * 0.3)
            weighted_recs.append((rec, final_score))
        
        # 가중 확률 기반 셔플
        shuffled = []
        remaining = list(weighted_recs)
        
        while remaining:
            # 현재 남은 아이템들의 가중치
            weights = np.array([score for _, score in remaining])
            if weights.sum() > 0:
                weights = weights / weights.sum()  # 정규화
            else:
                weights = np.ones(len(remaining)) / len(remaining)  # 균등 분배
            
            # 가중 확률로 선택
            try:
                idx = np.random.choice(len(remaining), p=weights)
                selected_rec, selected_score = remaining.pop(idx)
                shuffled.append(selected_rec)
            except Exception as e:
                # 예외 발생 시 첫 번째 아이템 선택
                logger.warning(f"가중 셔플 오류: {e}")
                selected_rec, _ = remaining.pop(0)
                shuffled.append(selected_rec)
        
        logger.info(f"가중 셔플 완료: {len(shuffled)}개 추천")
        return shuffled

    def apply_diversity_strategy(self, recommendations: List[Dict], 
                               diversity_mode: str,
                               include_opposite_views: bool,
                               max_per_category: int) -> List[Dict]:
        """
        다양성 전략을 추천 리스트에 적용
        
        Args:
            recommendations: 원본 추천 리스트
            diversity_mode: 다양성 모드 ("none", "balanced", "high")
            include_opposite_views: 반대 관점 포함 여부
            max_per_category: 카테고리별 최대 추천 수
            
        Returns:
            다양성이 적용된 추천 리스트
        """
        if diversity_mode == "none":
            return recommendations
        
        elif diversity_mode == "balanced":
            # 카테고리 균형 적용
            diversified = self.diversify_by_category(recommendations, max_per_category)
            
            # 반대 관점 추가 (5%)
            if include_opposite_views:
                diversified = self.add_balanced_perspectives(diversified, ratio=0.05)
            
            return diversified
        
        elif diversity_mode == "high":
            # 높은 다양성: 카테고리당 더 적게, 반대 관점 더 많이
            high_diversity_limit = max(5, max_per_category // 2)
            diversified = self.diversify_by_category(recommendations, high_diversity_limit)
            
            if include_opposite_views:
                diversified = self.add_balanced_perspectives(diversified, ratio=0.1)  # 10%
            
            return diversified
        
        return recommendations

    def get_hybrid_recommendations(self, user_id: str, cf_count: int = 50, cbf_count: int = 50,
                                 strategy: str = "balanced") -> Dict:
        """
        CF와 CBF를 결합한 하이브리드 추천 (전략 기반)

        Args:
            user_id: 사용자 ID
            cf_count: CF 추천 수
            cbf_count: CBF 추천 수
            strategy: 추천 전략 ("pure", "balanced", "diverse")

        Returns:
            Dict: {
                'user_id': str,
                'total_recommendations': int,
                'cf_recommendations': List[Dict],
                'cbf_recommendations': List[Dict],
                'combined_recommendations': List[Dict],
                'diversity_applied': Dict,
                'timestamp': str
            }
        """
        logger.info(f"하이브리드 추천 시작: user_id={user_id}, cf_count={cf_count}, cbf_count={cbf_count}, strategy={strategy}")

        # 최신 모델 학습 시간 가져오기
        latest_trained_time = self.get_latest_model_trained_time()
        timestamp = latest_trained_time.isoformat() if latest_trained_time else ""

        result = {
            'user_id': user_id,
            'total_recommendations': 0,
            'recommendations': [],
            'diversity_applied': {
                'strategy': strategy,
                'before_diversity_count': 0,
                'after_diversity_count': 0
            },
            'timestamp': timestamp
        }

        # 모델들이 로드되지 않았다면 로드 시도
        logger.debug(f"모델 상태 확인 - CF: {'로드됨' if self.cf_model_data else '미로드'}, CBF: {'로드됨' if self.cbf_model_data else '미로드'}")

        if not self.cf_model_data:
            logger.info("CF 모델 로드 시도...")
            cf_loaded = self.load_cf_model()
            logger.info(f"CF 모델 로드 결과: {'성공' if cf_loaded else '실패'}")

        if not self.cbf_model_data:
            logger.info("CBF 모델 로드 시도...")
            cbf_loaded = self.load_cbf_model()
            logger.info(f"CBF 모델 로드 결과: {'성공' if cbf_loaded else '실패'}")

        # CF 추천 가져오기
        logger.info(f"CF 추천 요청: user_id={user_id}, count={cf_count}")
        try:
            cf_recs = self.get_cf_recommendations(user_id, cf_count)
            logger.info(f"CF 추천 결과: {len(cf_recs)}개")
        except Exception as e:
            logger.error(f"CF 추천 생성 실패: {e}")
            cf_recs = []

        # CBF 추천 가져오기
        logger.info(f"CBF 추천 요청: user_id={user_id}, count={cbf_count}")
        try:
            cbf_recs = self.get_cbf_recommendations(user_id, cbf_count)
            logger.info(f"CBF 추천 결과: {len(cbf_recs)}개")
        except Exception as e:
            logger.error(f"CBF 추천 생성 실패: {e}")
            cbf_recs = []
        
        # 결합된 추천 리스트 생성 (중복 제거)
        logger.info("추천 결과 결합 시작...")
        combined = []
        seen_news_ids = set()

        # CF 추천을 먼저 추가
        logger.debug(f"CF 추천 추가 중: {len(cf_recs)}개")
        try:
            for rec in cf_recs:
                if rec.get('news_id') not in seen_news_ids:
                    combined.append(rec)
                    seen_news_ids.add(rec['news_id'])
        except Exception as e:
            logger.error(f"CF 추천 결합 중 오류: {e}")

        # CBF 추천을 추가 (중복 제거)
        logger.debug(f"CBF 추천 추가 중: {len(cbf_recs)}개")
        try:
            for rec in cbf_recs:
                if rec.get('news_id') not in seen_news_ids:
                    combined.append(rec)
                    seen_news_ids.add(rec['news_id'])
        except Exception as e:
            logger.error(f"CBF 추천 결합 중 오류: {e}")

        logger.info(f"추천 결합 완료: CF {len(cf_recs)}개 + CBF {len(cbf_recs)}개 -> 결합 {len(combined)}개")

        # 다양성 전략 적용 전 개수 기록
        result['diversity_applied']['before_diversity_count'] = len(combined)

        # 전략별 다양성 적용
        logger.info(f"다양성 전략 적용: {strategy}")
        try:
            if strategy == "pure":
                # 순수 추천: 다양성 적용 없음
                logger.debug("순수 전략: 다양성 적용 안함")
                pass
            elif strategy == "balanced":
                # 균형 추천: 카테고리 다양성 + 5% 반대 관점
                logger.debug("균형 전략: 카테고리 다양성 적용")
                combined = self.diversify_by_category(combined, max_per_category=15)
                combined = self.add_balanced_perspectives(combined, ratio=0.05)
            elif strategy == "diverse":
                # 다양성 추천: 엄격한 카테고리 제한 + 10% 반대 관점
                logger.debug("다양성 전략: 엄격한 카테고리 제한 적용")
                combined = self.diversify_by_category(combined, max_per_category=8)
                combined = self.add_balanced_perspectives(combined, ratio=0.1)
        except Exception as e:
            logger.error(f"다양성 전략 적용 중 오류: {e}")

        # 점수 정규화 (CF와 CBF 스케일 통일)
        logger.debug("점수 정규화 시작")
        try:
            combined = self.normalize_scores(combined)
            logger.debug("점수 정규화 완료")
        except Exception as e:
            logger.error(f"점수 정규화 중 오류: {e}")

        # 가중 셔플 적용 (점수 + 시간 가중치)
        logger.debug("가중 셔플 시작")
        try:
            combined = self.weighted_shuffle(combined)
            logger.debug("가중 셔플 완료")
        except Exception as e:
            logger.error(f"가중 셔플 중 오류: {e}")

        # 다양성 적용 후 개수 기록
        result['diversity_applied']['after_diversity_count'] = len(combined)

        result['recommendations'] = combined
        result['total_recommendations'] = len(combined)

        logger.info(f"하이브리드 추천 완료: 사용자 {user_id} -> CF:{len(cf_recs)}, CBF:{len(cbf_recs)}, 최종:{len(combined)}개 (전략: {strategy})")

        return result

# 전역 추천 서비스 인스턴스
recommendation_service = RecommendationService()