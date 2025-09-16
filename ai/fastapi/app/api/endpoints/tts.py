from fastapi import APIRouter, HTTPException, UploadFile, File
from fastapi.responses import FileResponse, Response
from app.models.schemas import TTSRequest, TTSResponse, VoiceType
from app.models.tts_models import tts_manager, TTSModelName
from app.models.edge_tts_engine import edge_tts_engine
from app.utils.file_utils import get_temp_audio_path, cleanup_old_files
import os
import shutil
from typing import Optional

router = APIRouter(prefix="/tts", tags=["TTS"])

# Voice configurations - 서로 다른 모델이나 설정으로 2개 목소리 구현
VOICE_CONFIGS = {
    VoiceType.VOICE1: {
        "model": TTSModelName.YOURTTS,
        "description": "Voice 1 - YourTTS Model"
    },
    VoiceType.VOICE2: {
        "model": TTSModelName.YOURTTS,  # 같은 모델이지만 다른 설정으로 사용
        "description": "Voice 2 - YourTTS Model (Alternative)"
    }
}


@router.post("/synthesize")
async def synthesize_speech(request: TTSRequest):
    """
    텍스트를 음성으로 변환 - 바로 WAV 파일 반환

    - **text**: 변환할 텍스트 (최대 1000자)
    - **voice**: 사용할 음성 (voice1 또는 voice2)
    - **language**: 언어 코드 (기본값: ko)
    - **filename**: 다운로드 파일명 (선택사항)
    """
    try:
        # Korean language - use Edge TTS for better Korean support
        if request.language == "ko":
            audio_data, filename = edge_tts_engine.synthesize_speech(
                text=request.text,
                voice_name=request.voice.value,
                filename=request.filename
            )

            return Response(
                content=audio_data,
                media_type="audio/wav",
                headers={
                    "Content-Disposition": f'attachment; filename="{filename}"'
                }
            )
        else:
            # For other languages, use Coqui TTS (fallback to file-based)
            voice_config = VOICE_CONFIGS.get(request.voice)
            if not voice_config:
                raise HTTPException(status_code=400, detail=f"Unsupported voice: {request.voice}")

            audio_path = tts_manager.synthesize_speech(
                text=request.text,
                model_name=voice_config["model"],
                language=request.language
            )

            if not os.path.exists(audio_path):
                raise HTTPException(status_code=500, detail="Failed to generate audio file")

            # Read file and return as response
            with open(audio_path, "rb") as f:
                audio_data = f.read()

            # Clean up temp file
            try:
                os.remove(audio_path)
            except:
                pass

            filename = request.filename + ".wav" if request.filename else "audio.wav"
            return Response(
                content=audio_data,
                media_type="audio/wav",
                headers={
                    "Content-Disposition": f'attachment; filename="{filename}"'
                }
            )

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/synthesize-with-reference", response_model=TTSResponse)
async def synthesize_with_reference_voice(
    text: str,
    voice: VoiceType = VoiceType.VOICE1,
    language: str = "ko",
    reference_audio: UploadFile = File(...)
):
    """
    참조 음성을 사용하여 음성 클로닝으로 텍스트를 변환

    - **text**: 변환할 텍스트
    - **voice**: 기본 음성 모델
    - **language**: 언어 코드
    - **reference_audio**: 참조할 음성 파일 (.wav, .mp3 등)
    """
    try:
        # Save uploaded reference audio
        ref_audio_path = get_temp_audio_path(f"ref_{reference_audio.filename}")
        with open(ref_audio_path, "wb") as buffer:
            shutil.copyfileobj(reference_audio.file, buffer)

        # Get voice configuration
        voice_config = VOICE_CONFIGS.get(voice)
        if not voice_config:
            raise HTTPException(status_code=400, detail=f"Unsupported voice: {voice}")

        # Generate audio with voice cloning
        audio_path = tts_manager.synthesize_speech(
            text=text,
            model_name=voice_config["model"],
            speaker_wav=ref_audio_path,
            language=language
        )

        # Cleanup reference file
        try:
            os.remove(ref_audio_path)
        except:
            pass

        if not os.path.exists(audio_path):
            raise HTTPException(status_code=500, detail="Failed to generate audio file")

        return TTSResponse(
            success=True,
            message="Audio generated with reference voice successfully",
            file_path=audio_path,
            audio_url=f"/tts/audio/{os.path.basename(audio_path)}"
        )

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/audio/{filename}")
async def get_audio_file(filename: str):
    """생성된 오디오 파일 다운로드"""
    file_path = os.path.join("temp_audio", filename)

    if not os.path.exists(file_path):
        raise HTTPException(status_code=404, detail="Audio file not found")

    return FileResponse(
        path=file_path,
        media_type="audio/wav",
        filename=filename
    )


@router.get("/voices")
async def get_available_voices():
    """사용 가능한 음성 목록 조회"""
    voice_info = edge_tts_engine.get_voice_info()

    return {
        "korean_voices": [
            {
                "id": voice_id,
                "name": info["name"],
                "gender": info["gender"],
                "language": info["language"],
                "engine": "Edge TTS"
            }
            for voice_id, info in voice_info.items()
        ],
        "other_languages": [
            {
                "id": voice.value,
                "name": voice.value.title(),
                "description": config["description"],
                "engine": "Coqui TTS"
            }
            for voice, config in VOICE_CONFIGS.items()
        ]
    }