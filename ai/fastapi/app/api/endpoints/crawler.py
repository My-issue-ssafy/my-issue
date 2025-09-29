# 크롤링 및 모델 학습 관련 API 엔드포인트
from fastapi import APIRouter, HTTPException
from app.core.crawler.crawler import run_crawl_job
from app.core.ml.train_cf import main as train_cf_model
from app.core.ml.train_cbf import main as train_cbf_model
from loguru import logger
import traceback
from datetime import datetime

router = APIRouter()

@router.get("/crawl-now")
def crawl_now():
    """수동 크롤링 시작"""
    start_time = datetime.now()
    logger.info(f"[API-CRAWL] Manual crawling started at {start_time}")

    try:
        logger.info("[API-CRAWL] Executing run_crawl_job()")
        run_crawl_job()

        end_time = datetime.now()
        duration = end_time - start_time
        logger.success(f"[API-CRAWL] Manual crawl completed successfully in {duration}")

        return {
            "status": "success",
            "message": "Manual crawl completed",
            "start_time": start_time.isoformat(),
            "end_time": end_time.isoformat(),
            "duration_seconds": duration.total_seconds()
        }
    except Exception as e:
        end_time = datetime.now()
        duration = end_time - start_time
        error_msg = str(e)

        logger.error(f"[API-CRAWL] Crawling failed after {duration}: {error_msg}")
        logger.error(f"[API-CRAWL] Full traceback:\n{traceback.format_exc()}")

        raise HTTPException(
            status_code=500,
            detail={
                "error": "Crawling failed",
                "message": error_msg,
                "start_time": start_time.isoformat(),
                "end_time": end_time.isoformat(),
                "duration_seconds": duration.total_seconds()
            }
        )

@router.get("/train-cf")
def train_cf_now():
    """CF 모델 수동 학습"""
    start_time = datetime.now()
    logger.info(f"[API-CF] CF model training started at {start_time}")

    try:
        logger.info("[API-CF] Executing train_cf_model()")
        train_cf_model()

        end_time = datetime.now()
        duration = end_time - start_time
        logger.success(f"[API-CF] CF model training completed successfully in {duration}")

        return {
            "status": "success",
            "message": "CF model training completed",
            "start_time": start_time.isoformat(),
            "end_time": end_time.isoformat(),
            "duration_seconds": duration.total_seconds()
        }
    except Exception as e:
        end_time = datetime.now()
        duration = end_time - start_time
        error_msg = str(e)

        logger.error(f"[API-CF] CF training failed after {duration}: {error_msg}")
        logger.error(f"[API-CF] Full traceback:\n{traceback.format_exc()}")

        raise HTTPException(
            status_code=500,
            detail={
                "error": "CF training failed",
                "message": error_msg,
                "start_time": start_time.isoformat(),
                "end_time": end_time.isoformat(),
                "duration_seconds": duration.total_seconds()
            }
        )

@router.get("/train-cbf")
def train_cbf_now():
    """CBF 모델 수동 학습"""
    start_time = datetime.now()
    logger.info(f"[API-CBF] CBF model training started at {start_time}")

    try:
        logger.info("[API-CBF] Executing train_cbf_model()")
        train_cbf_model()

        end_time = datetime.now()
        duration = end_time - start_time
        logger.success(f"[API-CBF] CBF model training completed successfully in {duration}")

        return {
            "status": "success",
            "message": "CBF model training completed",
            "start_time": start_time.isoformat(),
            "end_time": end_time.isoformat(),
            "duration_seconds": duration.total_seconds()
        }
    except Exception as e:
        end_time = datetime.now()
        duration = end_time - start_time
        error_msg = str(e)

        logger.error(f"[API-CBF] CBF training failed after {duration}: {error_msg}")
        logger.error(f"[API-CBF] Full traceback:\n{traceback.format_exc()}")

        raise HTTPException(
            status_code=500,
            detail={
                "error": "CBF training failed",
                "message": error_msg,
                "start_time": start_time.isoformat(),
                "end_time": end_time.isoformat(),
                "duration_seconds": duration.total_seconds()
            }
        )

@router.get("/crawl-and-retrain")
def crawl_and_retrain_now():
    """크롤링 후 모델 재학습 (전체 파이프라인)"""
    start_time = datetime.now()
    logger.info(f"[API-PIPELINE] Full pipeline started at {start_time}")

    steps_completed = []
    step_times = {}

    try:
        # 크롤링
        logger.info("[API-PIPELINE] Step 1/3: Starting crawling")
        crawl_start = datetime.now()
        run_crawl_job()
        crawl_end = datetime.now()
        steps_completed.append("crawling")
        step_times["crawling"] = (crawl_end - crawl_start).total_seconds()
        logger.success(f"[API-PIPELINE] Crawling completed in {crawl_end - crawl_start}")

        # CF 모델 재학습
        logger.info("[API-PIPELINE] Step 2/3: Starting CF model training")
        cf_start = datetime.now()
        train_cf_model()
        cf_end = datetime.now()
        steps_completed.append("cf_training")
        step_times["cf_training"] = (cf_end - cf_start).total_seconds()
        logger.success(f"[API-PIPELINE] CF training completed in {cf_end - cf_start}")

        # CBF 모델 재학습
        logger.info("[API-PIPELINE] Step 3/3: Starting CBF model training")
        cbf_start = datetime.now()
        train_cbf_model()
        cbf_end = datetime.now()
        steps_completed.append("cbf_training")
        step_times["cbf_training"] = (cbf_end - cbf_start).total_seconds()
        logger.success(f"[API-PIPELINE] CBF training completed in {cbf_end - cbf_start}")

        end_time = datetime.now()
        total_duration = end_time - start_time
        logger.success(f"[API-PIPELINE] Full pipeline completed successfully in {total_duration}")

        return {
            "status": "success",
            "message": "Crawling and model retraining completed successfully",
            "steps_completed": steps_completed,
            "start_time": start_time.isoformat(),
            "end_time": end_time.isoformat(),
            "total_duration_seconds": total_duration.total_seconds(),
            "step_durations": step_times
        }
    except Exception as e:
        end_time = datetime.now()
        duration = end_time - start_time
        error_msg = str(e)

        logger.error(f"[API-PIPELINE] Pipeline failed after {duration} at step: {len(steps_completed) + 1}")
        logger.error(f"[API-PIPELINE] Steps completed: {steps_completed}")
        logger.error(f"[API-PIPELINE] Error: {error_msg}")
        logger.error(f"[API-PIPELINE] Full traceback:\n{traceback.format_exc()}")

        raise HTTPException(
            status_code=500,
            detail={
                "error": "Pipeline failed",
                "message": error_msg,
                "steps_completed": steps_completed,
                "failed_at_step": len(steps_completed) + 1,
                "start_time": start_time.isoformat(),
                "end_time": end_time.isoformat(),
                "duration_seconds": duration.total_seconds(),
                "step_durations": step_times
            }
        )