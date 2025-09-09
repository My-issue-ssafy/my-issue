# scheduler.py
from apscheduler.schedulers.background import BackgroundScheduler
from crawler import run_crawl_job

def start_scheduler():
    scheduler = BackgroundScheduler(timezone="Asia/Seoul")
    # 매일 5시, 17시 실행
    scheduler.add_job(run_crawl_job, "cron", hour="5,17", minute=0)
    scheduler.start()
