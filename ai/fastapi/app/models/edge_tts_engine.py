import edge_tts
import asyncio
import uuid
import os
from typing import List, Dict


class EdgeTTSEngine:
    """Microsoft Edge TTS Engine for Korean voices"""

    def __init__(self):
        self.korean_voices = {
            "voice1": {
                "voice": "ko-KR-InJoonNeural",    # 남성 목소리
                "rate": "+20%",                   # 빠르게
                "pitch": "+0Hz"                   # 기본 음높이
            },
            "voice2": {
                "voice": "ko-KR-SunHiNeural",     # 여성 목소리
                "rate": "+20%",                   # 빠르게
                "pitch": "+5Hz"                   # 약간 높게
            }
        }

    async def get_available_voices(self) -> List[Dict[str, str]]:
        """Get available Korean voices"""
        voices = await edge_tts.list_voices()
        korean_voices = [v for v in voices if v['Locale'].startswith('ko-')]
        return korean_voices

    async def synthesize_speech_async(
        self,
        text: str,
        voice_name: str = "voice1",
        rate: str = "+0%",
        pitch: str = "+0Hz",
        filename: str = ""
    ) -> tuple[bytes, str]:
        """
        Synthesize speech using Edge TTS

        Args:
            text: Text to synthesize
            voice_name: Voice identifier (voice1 or voice2)
            rate: Speech rate adjustment
            pitch: Pitch adjustment
            filename: Custom filename (for response headers)

        Returns:
            Tuple of (audio_data as bytes, suggested_filename)
        """
        # Get the voice configuration
        voice_config = self.korean_voices.get(
            voice_name, self.korean_voices["voice1"])
        edge_voice = voice_config["voice"]
        voice_rate = rate if rate != "+0%" else voice_config["rate"]
        voice_pitch = pitch if pitch != "+0Hz" else voice_config["pitch"]

        # Generate filename for response headers
        if filename.strip():
            import re
            safe_filename = re.sub(r'[^\w\-_\.]', '_', filename.strip()) + ".wav"
        else:
            safe_filename = f"{uuid.uuid4().hex}.wav"

        # Create TTS communication
        communicate = edge_tts.Communicate(
            text, edge_voice, rate=voice_rate, pitch=voice_pitch)

        # Collect audio data
        audio_data = b""
        async for chunk in communicate.stream():
            if chunk["type"] == "audio":
                audio_data += chunk["data"]

        return audio_data, safe_filename

    def synthesize_speech(
        self,
        text: str,
        voice_name: str = "voice1",
        rate: str = "+0%",
        pitch: str = "+0Hz",
        filename: str = ""
    ) -> tuple[bytes, str]:
        """
        Synchronous wrapper for speech synthesis

        Args:
            text: Text to synthesize
            voice_name: Voice identifier (voice1 or voice2)
            rate: Speech rate adjustment
            pitch: Pitch adjustment
            filename: Custom filename

        Returns:
            Tuple of (audio_data as bytes, suggested_filename)
        """
        try:
            # Check if there's already an event loop running
            loop = asyncio.get_running_loop()
        except RuntimeError:
            # No event loop running, create new one
            return asyncio.run(
                self.synthesize_speech_async(text, voice_name, rate, pitch, filename)
            )
        else:
            # Event loop is running, need to use different approach
            import concurrent.futures
            import threading

            def run_in_thread():
                # Create new event loop for the thread
                new_loop = asyncio.new_event_loop()
                asyncio.set_event_loop(new_loop)
                try:
                    return new_loop.run_until_complete(
                        self.synthesize_speech_async(
                            text, voice_name, rate, pitch, filename)
                    )
                finally:
                    new_loop.close()

            with concurrent.futures.ThreadPoolExecutor() as executor:
                future = executor.submit(run_in_thread)
                return future.result()

    def get_voice_info(self) -> Dict[str, Dict[str, str]]:
        """Get information about available voices"""
        return {
            "voice1": {
                "name": "InJoon (Male, Fast)",
                "gender": "Male",
                "language": "Korean",
                "rate": self.korean_voices["voice1"]["rate"],
                "pitch": self.korean_voices["voice1"]["pitch"],
                "edge_voice": self.korean_voices["voice1"]["voice"]
            },
            "voice2": {
                "name": "SunHi (Female, Fast)",
                "gender": "Female",
                "language": "Korean",
                "rate": self.korean_voices["voice2"]["rate"],
                "pitch": self.korean_voices["voice2"]["pitch"],
                "edge_voice": self.korean_voices["voice2"]["voice"]
            }
        }


# Global Edge TTS engine instance
edge_tts_engine = EdgeTTSEngine()
