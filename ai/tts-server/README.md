# Korean TTS Server

FastAPI 기반 한국어 Text-to-Speech 서버입니다. 2개의 다른 목소리로 한국어 텍스트를 음성으로 변환할 수 있습니다.

## 기능

- **한국어 TTS**: 한국어 텍스트를 자연스러운 음성으로 변환
- **2개 목소리 지원**: voice1, voice2 두 가지 목소리 선택 가능
- **음성 클로닝**: 참조 음성을 업로드하여 해당 목소리로 변환
- **REST API**: 간단한 HTTP API로 TTS 기능 사용
- **실시간 처리**: 빠른 음성 생성 및 스트리밍

## 설치 및 실행

### 1. 가상환경 설정
```bash
python3.11 -m venv .venv
source .venv/bin/activate  # Linux/Mac
```

### 2. 의존성 설치
```bash
pip install -r requirements.txt
```

### 3. 서버 실행
```bash
# 방법 1: 직접 실행
python run.py

# 방법 2: uvicorn으로 실행
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

### 4. API 문서 확인
서버 실행 후 다음 URL에서 API 문서를 확인할 수 있습니다:
- **Swagger UI**: http://localhost:8000/
- **ReDoc**: http://localhost:8000/redoc

## API 사용법

### 1. 기본 TTS
```bash
curl -X POST "http://localhost:8000/tts/synthesize" \\
     -H "Content-Type: application/json" \\
     -d '{
       "text": "안녕하세요, 한국어 음성합성 테스트입니다.",
       "voice": "voice1",
       "language": "ko"
     }'
```

### 2. 참조 음성을 사용한 음성 클로닝
```bash
curl -X POST "http://localhost:8000/tts/synthesize-with-reference" \\
     -F "text=안녕하세요, 목소리를 클론했습니다." \\
     -F "voice=voice1" \\
     -F "language=ko" \\
     -F "reference_audio=@reference_voice.wav"
```

### 3. 음성 파일 다운로드
```bash
# 응답에서 받은 filename 사용
curl -O "http://localhost:8000/tts/audio/{filename}.wav"
```

### 4. 사용 가능한 목소리 확인
```bash
curl "http://localhost:8000/tts/voices"
```

### 5. 서버 상태 확인
```bash
curl "http://localhost:8000/health"
```

## 프로젝트 구조

```
tts-server/
├── app/
│   ├── __init__.py
│   └── main.py              # FastAPI 앱 메인 파일
├── api/
│   ├── __init__.py
│   └── tts_routes.py        # TTS API 라우터
├── models/
│   ├── __init__.py
│   ├── schemas.py           # Pydantic 모델
│   └── tts_models.py        # TTS 모델 관리
├── utils/
│   ├── __init__.py
│   └── file_utils.py        # 파일 유틸리티
├── temp_audio/              # 생성된 음성 파일 임시 저장
├── logs/                    # 로그 파일
├── requirements.txt         # 의존성 목록
├── run.py                   # 서버 실행 스크립트
└── README.md               # 프로젝트 문서
```

## 지원하는 기능

### 목소리 종류
- **voice1**: YourTTS 모델 기반 첫 번째 목소리
- **voice2**: YourTTS 모델 기반 두 번째 목소리

### 지원 언어
- **ko**: 한국어 (기본값)
- 다른 언어도 모델에 따라 지원 가능

### 파일 형식
- **입력**: 텍스트 (최대 1000자)
- **출력**: WAV 파일
- **참조 음성**: WAV, MP3 등 일반적인 오디오 형식

## 주의사항

- 생성된 음성 파일은 1시간 후 자동으로 삭제됩니다
- 첫 실행시 TTS 모델 다운로드로 시간이 소요될 수 있습니다
- 한국어 텍스트에 최적화되어 있습니다

## 라이센스

이 프로젝트는 Coqui TTS의 라이센스를 따릅니다. 상업적 사용시 별도 라이센스가 필요할 수 있습니다.