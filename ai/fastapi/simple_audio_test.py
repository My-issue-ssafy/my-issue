#!/usr/bin/env python3
import asyncio
import sys
sys.path.append('.')
from app.models.edge_tts_engine import edge_tts_engine

def analyze_wav_format(audio_data):
    """Simple WAV format analysis"""
    if len(audio_data) < 44:
        return "Data too short"

    # Check RIFF header
    riff = audio_data[0:4].decode('ascii', errors='ignore')
    wave = audio_data[8:12].decode('ascii', errors='ignore')

    # Audio format code (bytes 20-21)
    format_code = int.from_bytes(audio_data[20:22], byteorder='little')

    # Sample rate (bytes 24-27)
    sample_rate = int.from_bytes(audio_data[24:28], byteorder='little')

    # Channels (bytes 22-23)
    channels = int.from_bytes(audio_data[22:24], byteorder='little')

    return {
        'riff': riff,
        'wave': wave,
        'format_code': format_code,
        'sample_rate': sample_rate,
        'channels': channels,
        'is_mp3': format_code == 85,
        'is_pcm': format_code == 1
    }

async def main():
    print("Testing Edge TTS audio format...")

    try:
        audio_data, filename = await edge_tts_engine.synthesize_speech_async(
            text="Hello test",
            voice_name="voice1"
        )

        print(f"Audio size: {len(audio_data)} bytes")

        # Analyze format
        result = analyze_wav_format(audio_data)
        print(f"RIFF header: {result['riff']}")
        print(f"WAVE format: {result['wave']}")
        print(f"Format code: {result['format_code']}")
        print(f"Sample rate: {result['sample_rate']} Hz")
        print(f"Channels: {result['channels']}")

        if result['is_mp3']:
            print("*** THIS IS MP3 ENCODED IN WAV CONTAINER ***")
        elif result['is_pcm']:
            print("*** THIS IS STANDARD PCM WAV ***")
        else:
            print(f"*** UNKNOWN FORMAT CODE: {result['format_code']} ***")

        # Save for further analysis
        with open("test_audio.wav", "wb") as f:
            f.write(audio_data)
        print("Saved as test_audio.wav")

        # Hex dump first 50 bytes
        print("\nFirst 50 bytes (hex):")
        hex_data = audio_data[:50].hex()
        print(' '.join([hex_data[i:i+2] for i in range(0, len(hex_data), 2)]))

    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    asyncio.run(main())