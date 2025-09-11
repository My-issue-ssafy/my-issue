# scheduler.py - 통합된 스케줄러: 크롤링 + 모델 학습
from apscheduler.schedulers.background import BackgroundScheduler
from app.core.crawler.crawler import run_crawl_job
from app.core.ml.train_cf import main as train_model

def crawl_job():
    """크롤링 작업 실행"""
    try:
        run_crawl_job()
        print("[OK] Crawling completed")
    except Exception as e:
        print(f"[ERR] Crawling failed: {e}")

def train_job():
    """모델 학습 작업 실행"""
    try:
        train_model()
        print("[OK] Model training completed")
    except Exception as e:
        print(f"[ERR] Model training failed: {e}")

def start_scheduler():
    """통합 스케줄러 시작 - 크롤링과 모델 학습을 각각 스케줄링"""
    scheduler = BackgroundScheduler(timezone="Asia/Seoul")
    
    # 크롤링: 매일 5시, 17시 실행
    scheduler.add_job(crawl_job, "cron", hour="5,17", minute=0, id="crawl_job")
    
    # 모델 학습: 30분마다 실행
    scheduler.add_job(train_job, "interval", minutes=30, id="train_job")
    
    scheduler.start()
    print("[INFO] Scheduler started:")
    print("  - Crawling: Daily at 05:00, 17:00 KST")
    print("  - Model training: Every 30 minutes")
