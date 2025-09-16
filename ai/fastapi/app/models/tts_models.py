from enum import Enum
from TTS.api import TTS
import os
from typing import Optional, List


class TTSModelName(str, Enum):
    YOURTTS = "tts_models/multilingual/multi-dataset/your_tts"
    BARK = "tts_models/multilingual/multi-dataset/bark"


class TTSManager:
    def __init__(self):
        self.models = {}
        self.current_model = None

    def load_model(self, model_name: TTSModelName) -> TTS:
        """Load TTS model if not already loaded"""
        if model_name not in self.models:
            print(f"Loading model: {model_name}")
            self.models[model_name] = TTS(model_name=model_name.value)

        return self.models[model_name]

    def get_available_models(self) -> List[str]:
        """Get list of available TTS models"""
        return [model.value for model in TTSModelName]

    def synthesize_speech(
        self,
        text: str,
        model_name: TTSModelName = TTSModelName.YOURTTS,
        speaker_wav: Optional[str] = None,
        language: str = "ko",
        speaker_idx: Optional[int] = None
    ) -> str:
        """
        Synthesize speech from text

        Args:
            text: Text to synthesize
            model_name: TTS model to use
            speaker_wav: Reference speaker audio file path (for voice cloning)
            language: Language code (ko for Korean)
            speaker_idx: Speaker index for multi-speaker models

        Returns:
            Path to generated audio file
        """
        model = self.load_model(model_name)

        # Generate unique filename
        import uuid
        output_path = f"temp_audio/{uuid.uuid4().hex}.wav"
        os.makedirs("temp_audio", exist_ok=True)

        try:
            if model_name == TTSModelName.YOURTTS:
                if speaker_wav:
                    # Voice cloning with reference audio
                    model.tts_to_file(
                        text=text,
                        file_path=output_path,
                        speaker_wav=speaker_wav,
                        language=language
                    )
                else:
                    # Use default speaker or speaker index
                    # YourTTS는 화자 임베딩이 필요하므로 기본 화자 사용
                    try:
                        # 기본 화자로 시도
                        model.tts_to_file(
                            text=text,
                            file_path=output_path,
                            language=language
                        )
                    except:
                        # 기본 화자가 없으면 더미 화자 wav 생성
                        import numpy as np
                        import soundfile as sf
                        dummy_wav_path = "temp_audio/dummy_speaker.wav"
                        # 1초짜리 더미 오디오 생성 (무음)
                        dummy_audio = np.zeros(16000)  # 16kHz, 1초
                        sf.write(dummy_wav_path, dummy_audio, 16000)

                        model.tts_to_file(
                            text=text,
                            file_path=output_path,
                            speaker_wav=dummy_wav_path,
                            language=language
                        )

                        # 더미 파일 삭제
                        try:
                            os.remove(dummy_wav_path)
                        except:
                            pass
            else:
                # Other models
                model.tts_to_file(
                    text=text,
                    file_path=output_path,
                    language=language
                )

            return output_path
        except Exception as e:
            raise Exception(f"Failed to synthesize speech: {str(e)}")


# Global TTS manager instance
tts_manager = TTSManager()