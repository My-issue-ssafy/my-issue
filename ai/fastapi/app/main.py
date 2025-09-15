# main.py
from fastapi import FastAPI
from app.utils.scheduler import start_scheduler
from app.api.endpoints.crawler import router as crawler_router
from app.api.endpoints.recommendation import router as recommendation_router

app = FastAPI(
  title="News Recommendation System",
  version="1.0.0",
  root_path="/fastapi",
)

# API 라우터 등록
app.include_router(crawler_router, prefix="/api", tags=["crawler"])
app.include_router(recommendation_router, prefix="/api", tags=["recommendation"])

@app.get("/health")
def health():
    return {"status": "ok"}

@app.on_event("startup")
def startup_event():
    """애플리케이션 시작 시 스케줄러 실행"""
    start_scheduler()
