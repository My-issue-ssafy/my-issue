# 추천 API 엔드포인트
# CF와 CBF 모델을 활용한 하이브리드 추천 시스템 API

from fastapi import APIRouter, HTTPException, Query
from pydantic import BaseModel
from typing import List, Dict, Optional
from datetime import datetime

from app.core.ml.recommendation_service import recommendation_service

router = APIRouter()

class RecommendationResponse(BaseModel):
    """추천 응답 모델"""
    user_id: str
    total_recommendations: int
    cf_recommendations: List[Dict]
    cbf_recommendations: List[Dict]  
    combined_recommendations: List[Dict]
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
    cbf_count: int = Query(50, ge=1, le=100, description="CBF 추천 수 (1-100)")
):
    """
    사용자별 하이브리드 추천 (CF + CBF)
    
    - **user_id**: 추천을 받을 사용자 ID
    - **cf_count**: CF(협업 필터링) 추천 수 (기본값: 50)
    - **cbf_count**: CBF(콘텐츠 기반) 추천 수 (기본값: 50)
    
    Returns:
        CF 50개 + CBF 50개를 결합한 총 최대 100개의 뉴스 추천
        (중복 제거 후 실제 개수는 달라질 수 있음)
    """
    try:
        # 하이브리드 추천 생성
        result = recommendation_service.get_hybrid_recommendations(
            user_id=user_id,
            cf_count=cf_count,
            cbf_count=cbf_count
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