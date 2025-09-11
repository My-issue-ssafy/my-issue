# 크롤링 관련 API 엔드포인트
from fastapi import APIRouter
from app.core.crawler.crawler import run_crawl_job

router = APIRouter()

@router.get("/crawl-now")
def crawl_now():
    """수동 크롤링 시작"""
    run_crawl_job()
    return {"status": "ok", "message": "Manual crawl started"}