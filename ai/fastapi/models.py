# models.py
from sqlalchemy import Column, Integer, String, DateTime
# from sqlalchemy.dialects.postgresql import VECTOR
from sqlalchemy.ext.declarative import declarative_base
from pgvector.sqlalchemy import VECTOR

Base = declarative_base()

class News(Base):
    __tablename__ = "news"

    id = Column(Integer, primary_key=True, autoincrement=True)
    title = Column(String, nullable=False)
    content = Column(String)      # 본문
    category = Column(String)     # 섹션 (정치, 경제 등)
    author = Column(String)       # 기자
    newsPaper = Column(String)    # 언론사
    created_at = Column(DateTime) # 발행일
    views = Column(Integer, default=0)
    embedding = Column(VECTOR(768))  # pgvector 확장
