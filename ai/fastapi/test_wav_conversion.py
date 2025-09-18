#!/usr/bin/env python3
import asyncio
import sys
sys.path.append('.')
from app.models.edge_tts_engine import edge_tts_engine

def analyze_wav_format(audio_data):
    """Analyze WAV format"""
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

    # Bits per sample (bytes 34-35)
    bits_per_sample = int.from_bytes(audio_data[34:36], byteorder='little')

    return {
        'riff': riff,
        'wave': wave,
        'format_code': format_code,
        'sample_rate': sample_rate,
        'channels': channels,
        'bits_per_sample': bits_per_sample,
        'is_pcm': format_code == 1,
        'is_mp3': format_code == 85
    }

async def test_conversion():
    print("Testing MP3 to WAV conversion...")

    try:
        # Generate audio with new conversion
        audio_data, filename = await edge_tts_engine.synthesize_speech_async(
            text="This is a test for WAV conversion",
            voice_name="voice1"
        )

        print(f"Audio size: {len(audio_data)} bytes")
        print(f"Filename: {filename}")

        # Analyze format
        result = analyze_wav_format(audio_data)
        print(f"\nFormat Analysis:")
        print(f"RIFF header: {result['riff']}")
        print(f"WAVE format: {result['wave']}")
        print(f"Format code: {result['format_code']}")
        print(f"Sample rate: {result['sample_rate']} Hz")
        print(f"Channels: {result['channels']}")
        print(f"Bits per sample: {result['bits_per_sample']}")

        if result['is_pcm']:
            print("✅ SUCCESS: This is pure PCM WAV!")
        elif result['is_mp3']:
            print("❌ FAILED: Still MP3 format")
        else:
            print(f"❓ UNKNOWN: Format code {result['format_code']}")

        # Save for verification
        with open("converted_test.wav", "wb") as f:
            f.write(audio_data)
        print("\nSaved as converted_test.wav")

        # Hex dump first 50 bytes
        print("\nFirst 50 bytes (hex):")
        hex_data = audio_data[:50].hex()
        print(' '.join([hex_data[i:i+2] for i in range(0, len(hex_data), 2)]))

    except Exception as e:
        print(f"Error: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    asyncio.run(test_conversion())