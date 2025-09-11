# 크롤링 및 모델 학습 관련 API 엔드포인트
from fastapi import APIRouter, HTTPException
from app.core.crawler.crawler import run_crawl_job
from app.core.ml.train_cf import main as train_cf_model
from app.core.ml.train_cbf import main as train_cbf_model

router = APIRouter()

@router.get("/crawl-now")
def crawl_now():
    """수동 크롤링 시작"""
    try:
        run_crawl_job()
        return {"status": "success", "message": "Manual crawl completed"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Crawling failed: {str(e)}")

@router.get("/train-cf")
def train_cf_now():
    """CF 모델 수동 학습"""
    try:
        train_cf_model()
        return {"status": "success", "message": "CF model training completed"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"CF training failed: {str(e)}")

@router.get("/train-cbf") 
def train_cbf_now():
    """CBF 모델 수동 학습"""
    try:
        train_cbf_model()
        return {"status": "success", "message": "CBF model training completed"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"CBF training failed: {str(e)}")

@router.get("/crawl-and-retrain")
def crawl_and_retrain_now():
    """크롤링 후 모델 재학습 (전체 파이프라인)"""
    try:
        # 크롤링
        run_crawl_job()
        
        # CF 모델 재학습
        train_cf_model()
        
        # CBF 모델 재학습
        train_cbf_model()
        
        return {
            "status": "success", 
            "message": "Crawling and model retraining completed successfully",
            "steps": ["crawling", "cf_training", "cbf_training"]
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Pipeline failed: {str(e)}")