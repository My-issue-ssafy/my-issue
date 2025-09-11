# app/init_db.py
from sqlalchemy import create_engine
from app.db.models import Base
from sqlalchemy.orm import sessionmaker
from app.utils.config import settings

engine = create_engine(settings.DATABASE_URL, pool_pre_ping=True)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

# 모든 모델 기반 테이블 생성
Base.metadata.create_all(engine)
