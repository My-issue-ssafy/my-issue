# main.py
from fastapi import FastAPI
from app.utils.scheduler import start_scheduler
from app.api.endpoints.crawler import router as crawler_router

app = FastAPI(title="News Recommendation System", version="1.0.0")

# API 라우터 등록
app.include_router(crawler_router, prefix="/api", tags=["crawler"])

@app.get("/health")
def health():
    return {"status": "ok"}

@app.on_event("startup")
def startup_event():
    """애플리케이션 시작 시 스케줄러 실행"""
    start_scheduler()
