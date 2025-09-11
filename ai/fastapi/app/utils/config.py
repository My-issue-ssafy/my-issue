from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    DATABASE_URL: str

    class Config:
        env_file = ".env"   # 루트 디렉토리에 있는 .env 파일 자동 로드
        env_file_encoding = "utf-8"

settings = Settings()
