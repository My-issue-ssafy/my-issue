# app/init_db.py
from sqlalchemy import create_engine
from models import Base

DATABASE_URL = "postgresql+psycopg2://postgres:1234@localhost:5432/newsdb"
engine = create_engine(DATABASE_URL)

# 모든 모델 기반 테이블 생성
Base.metadata.create_all(engine)
