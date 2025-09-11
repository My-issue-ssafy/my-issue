# scheduler.py - 통합된 스케줄러: 크롤링 + 모델 학습
from apscheduler.schedulers.background import BackgroundScheduler
from app.core.crawler.crawler import run_crawl_job
from app.core.ml.train_cf import main as train_cf_model
from app.core.ml.train_cbf import main as train_cbf_model

def crawl_job():
    """크롤링 작업 실행 후 CBF 모델 재학습"""
    try:
        # 1. 크롤링 실행 (PostgreSQL에 새 뉴스 추가)
        print("[START] Running crawling job...")
        run_crawl_job()
        print("[OK] Crawling completed")
        
        # 2. CBF 모델 재학습 (새로운 뉴스 임베딩 반영)
        print("[START] Retraining CBF model...")
        train_cbf_model()
        print("[OK] CBF model training completed")
        
        print("[SUCCESS] Crawling and CBF retraining completed successfully")
        
    except Exception as e:
        print(f"[ERR] Crawling job failed: {e}")

def cf_train_job():
    """CF 모델 학습 작업 (BigQuery 데이터 기반)"""
    try:
        # CF 모델 학습 (BigQuery GA4 이벤트 데이터 기반)
        print("[START] Training CF model...")
        train_cf_model()
        print("[OK] CF model training completed")
    except Exception as e:
        print(f"[ERR] CF training failed: {e}")

def start_scheduler():
    """통합 스케줄러 시작 - 크롤링과 CF 모델 학습 분리"""
    scheduler = BackgroundScheduler(timezone="Asia/Seoul")
    
    # 크롤링 + CBF 재학습: 매일 5시, 17시 실행
    scheduler.add_job(crawl_job, "cron", hour="5,17", minute=0, id="crawl_job")
    
    # CF 모델 학습: 30분마다 실행 (BigQuery GA4 데이터 기반)
    scheduler.add_job(cf_train_job, "interval", minutes=30, id="cf_train_job")
    
    scheduler.start()
    print("[INFO] Scheduler started:")
    print("  - Crawling + CBF Training: Daily at 05:00, 17:00 KST (PostgreSQL → CBF)")
    print("  - CF Training: Every 30 minutes (BigQuery GA4 → CF)")
    print("  - Models are trained with different data sources")
