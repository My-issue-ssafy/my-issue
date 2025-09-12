FROM python:3.11-slim

# OpenMP 런타임(필수) + 표준 C++ 런타임
RUN apt-get update && \
    apt-get install -y --no-install-recommends libgomp1 libstdc++6 && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

# 종속성 설치
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 앱 복사
COPY . .

# uvicorn 실행 (포트 8000)
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8085"]