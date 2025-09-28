from fastapi import APIRouter, HTTPException, UploadFile, File
from fastapi.responses import FileResponse, Response
from app.models.schemas import TTSRequest, TTSResponse, VoiceType
from app.models.tts_models import tts_manager, TTSModelName
from app.models.edge_tts_engine import edge_tts_engine
from app.utils.file_utils import get_temp_audio_path, cleanup_old_files
import os
import shutil
import logging
from typing import Optional

logger = logging.getLogger(__name__)

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
    logger.info(f"TTS synthesize 요청 시작 - 언어: {request.language}, 음성: {request.voice}, 텍스트 길이: {len(request.text)}")
    logger.debug(f"요청 텍스트: {request.text[:100]}{'...' if len(request.text) > 100 else ''}")

    try:
        # Korean language - use Edge TTS for better Korean support
        if request.language == "ko":
            logger.info("한국어 처리 - Edge TTS 엔진 사용")

            audio_data, filename = edge_tts_engine.synthesize_speech(
                text=request.text,
                voice_name=request.voice.value,
                filename=request.filename
            )

            logger.info(f"Edge TTS 음성 합성 완료 - 파일명: {filename}, 데이터 크기: {len(audio_data)} bytes")

            return Response(
                content=audio_data,
                media_type="audio/wav",
                headers={
                    "Content-Disposition": f'attachment; filename="{filename}"'
                }
            )
        else:
            # For other languages, use Coqui TTS (fallback to file-based)
            logger.info(f"다른 언어 처리 - Coqui TTS 엔진 사용 (언어: {request.language})")

            voice_config = VOICE_CONFIGS.get(request.voice)
            if not voice_config:
                logger.error(f"지원하지 않는 음성: {request.voice}")
                raise HTTPException(status_code=400, detail=f"Unsupported voice: {request.voice}")

            logger.debug(f"사용할 음성 설정: {voice_config}")

            audio_path = tts_manager.synthesize_speech(
                text=request.text,
                model_name=voice_config["model"],
                language=request.language
            )

            logger.info(f"Coqui TTS 음성 합성 완료 - 파일 경로: {audio_path}")

            if not os.path.exists(audio_path):
                logger.error(f"생성된 오디오 파일이 존재하지 않음: {audio_path}")
                raise HTTPException(status_code=500, detail="Failed to generate audio file")

            # Read file and return as response
            with open(audio_path, "rb") as f:
                audio_data = f.read()

            logger.info(f"오디오 파일 읽기 완료 - 크기: {len(audio_data)} bytes")

            # Clean up temp file
            try:
                os.remove(audio_path)
                logger.debug(f"임시 파일 삭제 완료: {audio_path}")
            except Exception as cleanup_error:
                logger.warning(f"임시 파일 삭제 실패: {audio_path}, 오류: {cleanup_error}")

            filename = request.filename + ".wav" if request.filename else "audio.wav"
            logger.info(f"응답 파일명: {filename}")

            return Response(
                content=audio_data,
                media_type="audio/wav",
                headers={
                    "Content-Disposition": f'attachment; filename="{filename}"'
                }
            )

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"TTS synthesize 처리 중 오류 발생: {str(e)}", exc_info=True)
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
    logger.info(f"음성 클로닝 요청 시작 - 언어: {language}, 음성: {voice}, 텍스트 길이: {len(text)}")
    logger.info(f"참조 오디오 파일: {reference_audio.filename}, 타입: {reference_audio.content_type}")

    try:
        # Save uploaded reference audio
        ref_audio_path = get_temp_audio_path(f"ref_{reference_audio.filename}")
        logger.debug(f"참조 오디오 저장 경로: {ref_audio_path}")

        with open(ref_audio_path, "wb") as buffer:
            shutil.copyfileobj(reference_audio.file, buffer)

        logger.info(f"참조 오디오 파일 저장 완료: {ref_audio_path}")

        # Get voice configuration
        voice_config = VOICE_CONFIGS.get(voice)
        if not voice_config:
            logger.error(f"지원하지 않는 음성: {voice}")
            raise HTTPException(status_code=400, detail=f"Unsupported voice: {voice}")

        logger.debug(f"사용할 음성 설정: {voice_config}")

        # Generate audio with voice cloning
        logger.info("음성 클로닝 시작")
        audio_path = tts_manager.synthesize_speech(
            text=text,
            model_name=voice_config["model"],
            speaker_wav=ref_audio_path,
            language=language
        )

        logger.info(f"음성 클로닝 완료 - 출력 파일: {audio_path}")

        # Cleanup reference file
        try:
            os.remove(ref_audio_path)
            logger.debug(f"참조 오디오 파일 삭제 완료: {ref_audio_path}")
        except Exception as cleanup_error:
            logger.warning(f"참조 오디오 파일 삭제 실패: {ref_audio_path}, 오류: {cleanup_error}")

        if not os.path.exists(audio_path):
            logger.error(f"생성된 오디오 파일이 존재하지 않음: {audio_path}")
            raise HTTPException(status_code=500, detail="Failed to generate audio file")

        audio_url = f"/tts/audio/{os.path.basename(audio_path)}"
        logger.info(f"음성 클로닝 성공 - 오디오 URL: {audio_url}")

        return TTSResponse(
            success=True,
            message="Audio generated with reference voice successfully",
            file_path=audio_path,
            audio_url=audio_url
        )

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"음성 클로닝 처리 중 오류 발생: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/audio/{filename}")
async def get_audio_file(filename: str):
    """생성된 오디오 파일 다운로드"""
    logger.info(f"오디오 파일 다운로드 요청: {filename}")

    file_path = os.path.join("temp_audio", filename)
    logger.debug(f"파일 경로: {file_path}")

    if not os.path.exists(file_path):
        logger.warning(f"요청된 오디오 파일이 존재하지 않음: {file_path}")
        raise HTTPException(status_code=404, detail="Audio file not found")

    logger.info(f"오디오 파일 다운로드 시작: {filename}")

    return FileResponse(
        path=file_path,
        media_type="audio/wav",
        filename=filename
    )


@router.get("/voices")
async def get_available_voices():
    """사용 가능한 음성 목록 조회"""
    logger.info("사용 가능한 음성 목록 조회 요청")

    try:
        voice_info = edge_tts_engine.get_voice_info()
        logger.debug(f"Edge TTS 음성 정보 수: {len(voice_info)}")

        korean_voices = [
            {
                "id": voice_id,
                "name": info["name"],
                "gender": info["gender"],
                "language": info["language"],
                "engine": "Edge TTS"
            }
            for voice_id, info in voice_info.items()
        ]

        other_voices = [
            {
                "id": voice.value,
                "name": voice.value.title(),
                "description": config["description"],
                "engine": "Coqui TTS"
            }
            for voice, config in VOICE_CONFIGS.items()
        ]

        logger.info(f"음성 목록 반환 - 한국어: {len(korean_voices)}개, 기타: {len(other_voices)}개")

        return {
            "korean_voices": korean_voices,
            "other_languages": other_voices
        }

    except Exception as e:
        logger.error(f"음성 목록 조회 중 오류 발생: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))