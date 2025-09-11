# 협업 필터링 모델을 주기적으로 재학습하는 스케줄러
from apscheduler.schedulers.blocking import BlockingScheduler
from app.train_cf import main as train_once

def job():
    """
    스케줄러에서 실행될 작업 함수
    
    train_cf.py의 main 함수를 실행하여 협업 필터링 모델을 재학습합니다.
    예외 발생 시 로그를 출력하고 계속 실행됩니다.
    """
    try:
        # 협업 필터링 모델 학습 실행
        train_once()
    except Exception as e:
        # 에러 발생 시 로그 출력 (스케줄러는 계속 실행됨)
        print("[ERR]", e)

if __name__ == "__main__":
    # 한국 시간대로 스케줄러 생성 (블로킹 모드: 메인 스레드에서 실행)
    sched = BlockingScheduler(timezone="Asia/Seoul")
    
    # 30분마다 job 함수 실행하도록 스케줄 등록
    sched.add_job(job, "interval", minutes=30, id="retrain")
    
    print("[RUN] scheduler started (every 30 min)")
    # 스케줄러 시작 (무한 루프로 실행, Ctrl+C로 종료 가능)
    sched.start()
