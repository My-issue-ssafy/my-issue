#!/usr/bin/env python3
"""
WAV 파일의 실제 인코딩 방식을 확인하는 테스트 스크립트
"""

import asyncio
import io
from app.models.edge_tts_engine import edge_tts_engine

def analyze_wav_header(audio_data):
    """WAV 파일 헤더를 분석하여 실제 인코딩 방식 확인"""
    if len(audio_data) < 44:
        return "Audio data too short to analyze"

    # WAV 파일 헤더 분석
    header_info = {}

    # RIFF 헤더 (0-3 bytes)
    riff_header = audio_data[0:4]
    header_info['riff_header'] = riff_header.decode('ascii', errors='ignore')

    # 파일 크기 (4-7 bytes)
    file_size = int.from_bytes(audio_data[4:8], byteorder='little')
    header_info['file_size'] = file_size

    # WAVE 포맷 (8-11 bytes)
    wave_format = audio_data[8:12]
    header_info['wave_format'] = wave_format.decode('ascii', errors='ignore')

    # fmt 청크 (12-15 bytes)
    fmt_chunk = audio_data[12:16]
    header_info['fmt_chunk'] = fmt_chunk.decode('ascii', errors='ignore')

    # fmt 청크 크기 (16-19 bytes)
    fmt_size = int.from_bytes(audio_data[16:20], byteorder='little')
    header_info['fmt_size'] = fmt_size

    # 오디오 포맷 (20-21 bytes)
    # 1 = PCM, 85 = MP3, 등
    audio_format = int.from_bytes(audio_data[20:22], byteorder='little')
    header_info['audio_format'] = audio_format

    # 채널 수 (22-23 bytes)
    channels = int.from_bytes(audio_data[22:24], byteorder='little')
    header_info['channels'] = channels

    # 샘플 레이트 (24-27 bytes)
    sample_rate = int.from_bytes(audio_data[24:28], byteorder='little')
    header_info['sample_rate'] = sample_rate

    # 바이트 레이트 (28-31 bytes)
    byte_rate = int.from_bytes(audio_data[28:32], byteorder='little')
    header_info['byte_rate'] = byte_rate

    # 블록 정렬 (32-33 bytes)
    block_align = int.from_bytes(audio_data[32:34], byteorder='little')
    header_info['block_align'] = block_align

    # 비트 레이트 (34-35 bytes)
    bits_per_sample = int.from_bytes(audio_data[34:36], byteorder='little')
    header_info['bits_per_sample'] = bits_per_sample

    return header_info

def format_code_to_name(format_code):
    """오디오 포맷 코드를 이름으로 변환"""
    format_names = {
        1: "PCM (Uncompressed)",
        2: "Microsoft ADPCM",
        3: "IEEE Float",
        6: "A-law",
        7: "μ-law",
        17: "IMA ADPCM",
        20: "ITU G.723 ADPCM",
        49: "GSM 6.10",
        64: "ITU G.721 ADPCM",
        80: "MPEG",
        85: "MP3",
        353: "Windows Media Audio 9",
        354: "Windows Media Audio 9 Professional",
        65534: "Extensible"
    }
    return format_names.get(format_code, f"Unknown ({format_code})")

async def test_audio_encoding():
    """Edge TTS로 생성된 오디오의 실제 인코딩 방식 테스트"""

    test_text = "안녕하세요, 이것은 오디오 인코딩 테스트입니다."

    print("Edge TTS로 오디오 생성 중...")
    audio_data, filename = await edge_tts_engine.synthesize_speech_async(
        text=test_text,
        voice_name="voice1"
    )

    print(f"생성된 오디오 크기: {len(audio_data)} bytes")
    print(f"파일명: {filename}")

    # WAV 헤더 분석
    print("\n=== WAV 헤더 분석 ===")
    header_info = analyze_wav_header(audio_data)

    if isinstance(header_info, dict):
        print(f"RIFF 헤더: {header_info['riff_header']}")
        print(f"파일 크기: {header_info['file_size']} bytes")
        print(f"WAVE 포맷: {header_info['wave_format']}")
        print(f"fmt 청크: {header_info['fmt_chunk']}")
        print(f"fmt 크기: {header_info['fmt_size']} bytes")
        print(f"오디오 포맷 코드: {header_info['audio_format']}")
        print(f"오디오 포맷: {format_code_to_name(header_info['audio_format'])}")
        print(f"채널 수: {header_info['channels']}")
        print(f"샘플 레이트: {header_info['sample_rate']} Hz")
        print(f"바이트 레이트: {header_info['byte_rate']} bytes/sec")
        print(f"블록 정렬: {header_info['block_align']}")
        print(f"비트 레이트: {header_info['bits_per_sample']} bits/sample")

        # MP3 인코딩 여부 판단
        if header_info['audio_format'] == 85:
            print("\n⚠️  이 WAV 파일은 MP3 인코딩을 사용하고 있습니다!")
        elif header_info['audio_format'] == 1:
            print("\n✅ 이 WAV 파일은 표준 PCM 인코딩을 사용하고 있습니다.")
        else:
            print(f"\n🤔 이 WAV 파일은 {format_code_to_name(header_info['audio_format'])} 인코딩을 사용하고 있습니다.")
    else:
        print(f"헤더 분석 실패: {header_info}")

    # 첫 50바이트의 헥스 덤프
    print(f"\n=== 첫 50바이트 헥스 덤프 ===")
    hex_dump = audio_data[:50].hex()
    formatted_hex = ' '.join([hex_dump[i:i+2] for i in range(0, len(hex_dump), 2)])
    print(formatted_hex)

    # 임시 파일로 저장해서 추가 분석
    temp_filename = "temp_test_audio.wav"
    with open(temp_filename, "wb") as f:
        f.write(audio_data)
    print(f"\n임시 파일 저장: {temp_filename}")
    print("ffprobe로 추가 분석을 원하면 다음 명령어를 실행하세요:")
    print(f"ffprobe -i {temp_filename}")

if __name__ == "__main__":
    asyncio.run(test_audio_encoding())