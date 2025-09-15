# 추천 API 엔드포인트
# CF와 CBF 모델을 활용한 하이브리드 추천 시스템 API

from fastapi import APIRouter, HTTPException, Query
from pydantic import BaseModel
from typing import List, Dict, Optional
from datetime import datetime

from app.core.ml.recommendation_service import recommendation_service

router = APIRouter()

class DiversityInfo(BaseModel):
    """다양성 적용 정보"""
    strategy: str
    before_diversity_count: int
    after_diversity_count: int

class RecommendationResponse(BaseModel):
    """추천 응답 모델"""
    user_id: str
    total_recommendations: int
    recommendations: List[Dict]
    diversity_applied: DiversityInfo
    timestamp: str

class RecommendationItem(BaseModel):
    """개별 추천 아이템 모델"""
    news_id: int
    score: float
    method: str
    title: Optional[str] = None
    category: Optional[str] = None

@router.get("/recommendations/{user_id}", response_model=RecommendationResponse)
async def get_user_recommendations(
    user_id: str,
    cf_count: int = Query(50, ge=1, le=100, description="CF 추천 수 (1-100)"),
    cbf_count: int = Query(50, ge=1, le=100, description="CBF 추천 수 (1-100)"),
    strategy: str = Query("balanced", description="추천 전략: pure, balanced, diverse")
):
    """
    사용자별 하이브리드 추천 (CF + CBF + 전략 선택)
    
    - **user_id**: 추천을 받을 사용자 ID
    - **cf_count**: CF(협업 필터링) 추천 수 (기본값: 50)
    - **cbf_count**: CBF(콘텐츠 기반) 추천 수 (기본값: 50)
    - **strategy**: 추천 전략
        - "pure": 순수 추천 (다양성 없음, 점수 순)
        - "balanced": 균형 추천 (카테고리 다양성 + 5% 반대 관점)
        - "diverse": 다양성 추천 (엄격한 카테고리 제한 + 10% 반대 관점)
    
    Returns:
        전략이 적용된 하이브리드 뉴스 추천 + 적용 정보
    """
    try:
        # 전략 검증
        if strategy not in ["pure", "balanced", "diverse"]:
            raise HTTPException(
                status_code=400,
                detail="strategy는 'pure', 'balanced', 'diverse' 중 하나여야 합니다."
            )
        
        # 하이브리드 추천 생성 (전략 적용)
        result = recommendation_service.get_hybrid_recommendations(
            user_id=user_id,
            cf_count=cf_count,
            cbf_count=cbf_count,
            strategy=strategy
        )
        
        if result['total_recommendations'] == 0:
            raise HTTPException(
                status_code=404,
                detail=f"사용자 {user_id}에 대한 추천을 생성할 수 없습니다. 모델에 해당 사용자 데이터가 없을 수 있습니다."
            )
        
        return RecommendationResponse(**result)
        
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"추천 생성 중 오류가 발생했습니다: {str(e)}"
        )

@router.get("/recommendations/{user_id}/cf", response_model=List[RecommendationItem])
async def get_cf_recommendations(
    user_id: str,
    count: int = Query(50, ge=1, le=100, description="추천 수 (1-100)")
):
    """
    사용자별 CF(협업 필터링) 추천만 가져오기
    
    - **user_id**: 추천을 받을 사용자 ID
    - **count**: 추천 수 (기본값: 50)
    """
    try:
        if not recommendation_service.cf_model_data:
            recommendation_service.load_cf_model()
        
        recommendations = recommendation_service.get_cf_recommendations(user_id, count)
        
        if not recommendations:
            raise HTTPException(
                status_code=404,
                detail=f"사용자 {user_id}에 대한 CF 추천을 생성할 수 없습니다."
            )
        
        return [RecommendationItem(**rec) for rec in recommendations]
        
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"CF 추천 생성 중 오류가 발생했습니다: {str(e)}"
        )

@router.get("/recommendations/{user_id}/cbf", response_model=List[RecommendationItem])
async def get_cbf_recommendations(
    user_id: str,
    count: int = Query(50, ge=1, le=100, description="추천 수 (1-100)")
):
    """
    사용자별 CBF(콘텐츠 기반) 추천만 가져오기
    
    - **user_id**: 추천을 받을 사용자 ID
    - **count**: 추천 수 (기본값: 50)
    """
    try:
        if not recommendation_service.cbf_model_data:
            recommendation_service.load_cbf_model()
        
        recommendations = recommendation_service.get_cbf_recommendations(user_id, count)
        
        if not recommendations:
            raise HTTPException(
                status_code=404,
                detail=f"사용자 {user_id}에 대한 CBF 추천을 생성할 수 없습니다."
            )
        
        return [RecommendationItem(**rec) for rec in recommendations]
        
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"CBF 추천 생성 중 오류가 발생했습니다: {str(e)}"
        )

@router.get("/health")
async def health_check():
    """
    추천 서비스 상태 확인
    """
    try:
        # 모델 상태 확인
        cf_status = "loaded" if recommendation_service.cf_model_data else "not_loaded"
        cbf_status = "loaded" if recommendation_service.cbf_model_data else "not_loaded"
        
        return {
            "status": "healthy",
            "timestamp": datetime.now().isoformat(),
            "models": {
                "cf_model": {
                    "status": cf_status,
                    "loaded_at": recommendation_service.cf_loaded_at.isoformat() if recommendation_service.cf_loaded_at else None
                },
                "cbf_model": {
                    "status": cbf_status,
                    "loaded_at": recommendation_service.cbf_loaded_at.isoformat() if recommendation_service.cbf_loaded_at else None
                }
            }
        }
        
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"Health check 실패: {str(e)}"
        )