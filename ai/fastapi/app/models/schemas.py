from pydantic import BaseModel, Field
from typing import Optional
from enum import Enum


class VoiceType(str, Enum):
    VOICE1 = "voice1"
    VOICE2 = "voice2"


class TTSRequest(BaseModel):
    text: str = Field(..., description="Text to convert to speech", max_length=1000)
    voice: VoiceType = Field(default=VoiceType.VOICE1, description="Voice type to use")
    language: str = Field(default="ko", description="Language code (ko for Korean)")
    filename: str = Field(default="", description="Custom filename (without extension). If empty, random filename will be used")

    class Config:
        json_schema_extra = {
            "example": {
                "text": "안녕하세요, 한국어 음성합성 테스트입니다.",
                "voice": "voice1",
                "language": "ko",
                "filename": "my_tts_audio"
            }
        }


class TTSResponse(BaseModel):
    success: bool
    message: str
    audio_url: Optional[str] = None
    file_path: Optional[str] = None


class HealthResponse(BaseModel):
    status: str
    available_models: list
    available_voices: list