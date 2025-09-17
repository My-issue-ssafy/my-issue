from sqlalchemy import Column, Integer, String, DateTime
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.ext.declarative import declarative_base
from pgvector.sqlalchemy import Vector

Base = declarative_base()

class News(Base):
    __tablename__ = "news"

    id = Column(Integer, primary_key=True, autoincrement=True)
    title = Column(String, nullable=False, unique=True) # 제목
    content = Column(JSONB)      # 본문
    category = Column(String)     # 섹션 (정치, 경제 등)
    author = Column(String)       # 기자
    news_paper = Column(String)    # 언론사
    created_at = Column(DateTime) # 발행일
    views = Column(Integer, default=0)
    embedding = Column(Vector(768))  # pgvector 확장
    thumbnail = Column(String)
    scrap_count = Column(Integer, default=0)
