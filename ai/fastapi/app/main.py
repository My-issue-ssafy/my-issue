# main.py
from fastapi import FastAPI
from app.utils.scheduler import start_scheduler
from app.crawler.crawler import run_crawl_job

app = FastAPI()

@app.on_event("startup")
def startup_event():
    start_scheduler()

@app.get("/crawl-now")
def crawl_now():
    run_crawl_job()
    return {"status": "ok", "message": "Manual crawl started"}
