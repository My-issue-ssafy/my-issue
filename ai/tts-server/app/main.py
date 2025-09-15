from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from api.tts_routes import router as tts_router
from models.schemas import HealthResponse
from models.tts_models import tts_manager
import uvicorn
import os

app = FastAPI(
    title="Korean TTS Server",
    description="FastAPI server for Korean Text-to-Speech with multiple voices",
    version="1.0.0",
    docs_url="/",
    redoc_url="/redoc"
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include routers
app.include_router(tts_router)


@app.get("/health", response_model=HealthResponse)
async def health_check():
    """서버 상태 및 사용 가능한 모델 확인"""
    try:
        available_models = tts_manager.get_available_models()
        available_voices = ["voice1", "voice2"]

        return HealthResponse(
            status="healthy",
            available_models=available_models,
            available_voices=available_voices
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Health check failed: {str(e)}")


@app.on_event("startup")
async def startup_event():
    """서버 시작시 실행"""
    print("Starting Korean TTS Server...")

    # Create necessary directories
    os.makedirs("temp_audio", exist_ok=True)
    os.makedirs("logs", exist_ok=True)

    print("Server started successfully!")


@app.on_event("shutdown")
async def shutdown_event():
    """서버 종료시 실행"""
    print("Shutting down Korean TTS Server...")


if __name__ == "__main__":
    uvicorn.run(
        "app.main:app",
        host="0.0.0.0",
        port=8000,
        reload=True,
        log_level="info"
    )