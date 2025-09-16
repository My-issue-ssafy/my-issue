# main.py
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.utils.scheduler import start_scheduler
from app.api.endpoints.crawler import router as crawler_router
from app.api.endpoints.recommendation import router as recommendation_router
from app.api.endpoints.tts import router as tts_router
import os

app = FastAPI(
  title="News Recommendation & TTS System",
  version="1.0.0",
  root_path="/fastapi",
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# API 라우터 등록
app.include_router(crawler_router, prefix="/api", tags=["crawler"])
app.include_router(recommendation_router, prefix="/api", tags=["recommendation"])
app.include_router(tts_router, prefix="/api", tags=["TTS"])

@app.get("/health")
def health():
    return {"status": "ok"}

@app.on_event("startup")
def startup_event():
    """애플리케이션 시작 시 스케줄러 실행"""
    start_scheduler()

    # Create necessary directories for TTS
    os.makedirs("temp_audio", exist_ok=True)
    os.makedirs("logs", exist_ok=True)
