#!/usr/bin/env python3
"""
추천 시스템 성능 측정 테스트
- 모델 로딩 시간
- 추천 생성 시간
- 메모리 사용량
"""

import time
import pickle
import numpy as np
import pandas as pd
from pathlib import Path
import psutil
import os

def measure_memory():
    """현재 프로세스의 메모리 사용량 (MB)"""
    process = psutil.Process(os.getpid())
    return process.memory_info().rss / 1024 / 1024

def test_model_loading():
    """모델 로딩 시간 측정"""
    MODEL_PATH = Path("models/als_model.pkl")
    
    if not MODEL_PATH.exists():
        print("모델 파일이 없습니다. 먼저 모델을 학습하세요.")
        return None
    
    print("=== 모델 로딩 성능 테스트 ===")
    
    # 메모리 사용량 (로딩 전)
    memory_before = measure_memory()
    print(f"로딩 전 메모리 사용량: {memory_before:.1f} MB")
    
    # 모델 로딩 시간 측정
    start_time = time.time()
    
    with open(MODEL_PATH, 'rb') as f:
        model_data = pickle.load(f)
    
    loading_time = time.time() - start_time
    
    # 메모리 사용량 (로딩 후)
    memory_after = measure_memory()
    memory_used = memory_after - memory_before
    
    print(f"모델 로딩 시간: {loading_time:.3f}초")
    print(f"로딩 후 메모리 사용량: {memory_after:.1f} MB")
    print(f"모델이 사용하는 메모리: {memory_used:.1f} MB")
    
    return model_data

def test_recommendation_speed(model_data, num_tests=10):
    """추천 생성 시간 측정"""
    print(f"\n=== 추천 생성 성능 테스트 (총 {num_tests}회) ===")
    
    model = model_data['model']
    user_categories = model_data['user_categories']
    item_categories = model_data['item_categories']
    interaction_matrix = model_data['interaction_matrix']
    
    # implicit이 사용자/아이템을 바꿔 학습했으므로 변수명을 명확하게
    actual_user_embeddings = model.item_factors     # (50000, 64)
    actual_item_embeddings = model.user_factors     # (200000, 64)
    
    print(f"사용자 수: {len(user_categories)}")
    print(f"아이템 수: {len(item_categories)}")
    print(f"사용자 임베딩 크기: {actual_user_embeddings.shape}")
    print(f"아이템 임베딩 크기: {actual_item_embeddings.shape}")
    
    # 랜덤 사용자들로 테스트
    test_user_indices = np.random.choice(len(user_categories), num_tests, replace=False)
    
    recommendation_times = []
    
    for i, user_idx in enumerate(test_user_indices):
        start_time = time.time()
        
        # 추천 생성 (실제 FastAPI에서 사용할 로직과 동일)
        try:
            user_embedding = actual_user_embeddings[user_idx]
            scores_all = actual_item_embeddings.dot(user_embedding)
            
            # 이미 상호작용한 아이템 제외
            user_items = interaction_matrix[user_idx]
            if hasattr(user_items, 'toarray'):
                user_items_dense = user_items.toarray().flatten()
            else:
                user_items_dense = user_items
            
            interacted_items = np.where(user_items_dense > 0)[0]
            scores_all[interacted_items] = -np.inf
            
            # 상위 10개 추천
            top_indices = np.argsort(-scores_all)[:10]
            top_scores = scores_all[top_indices]
            
            rec_time = time.time() - start_time
            recommendation_times.append(rec_time)
            
            if i < 3:  # 처음 3개만 상세 출력
                user_id = user_categories[user_idx]
                print(f"\n[테스트 {i+1}] 사용자 {user_id}:")
                print(f"  추천 생성 시간: {rec_time*1000:.2f}ms")
                print(f"  상호작용 아이템 수: {len(interacted_items)}")
                print(f"  추천 아이템: {[item_categories[idx] for idx in top_indices[:3]]}")
                print(f"  점수: {top_scores[:3]}")
        
        except Exception as e:
            print(f"사용자 {user_idx} 추천 실패: {e}")
    
    if recommendation_times:
        avg_time = np.mean(recommendation_times)
        min_time = np.min(recommendation_times)
        max_time = np.max(recommendation_times)
        
        print(f"\n=== 추천 성능 요약 ===")
        print(f"평균 추천 시간: {avg_time*1000:.2f}ms")
        print(f"최소 추천 시간: {min_time*1000:.2f}ms")
        print(f"최대 추천 시간: {max_time*1000:.2f}ms")
        print(f"초당 처리 가능 사용자: {1/avg_time:.1f}명")
        
        return avg_time
    else:
        print("모든 추천이 실패했습니다.")
        return None

def main():
    """메인 테스트 실행"""
    print("추천 시스템 성능 측정 시작...")
    
    # 1. 모델 로딩 테스트
    model_data = test_model_loading()
    if model_data is None:
        return
    
    # 2. 추천 생성 테스트
    avg_time = test_recommendation_speed(model_data, num_tests=20)
    
    if avg_time:
        print(f"\n=== 최종 결론 ===")
        print(f"FastAPI에서 예상 응답 시간: {avg_time*1000:.2f}ms + 네트워크 지연")
        if avg_time < 0.1:  # 100ms 미만
            print("✅ 실시간 서비스에 적합한 성능")
        elif avg_time < 0.5:  # 500ms 미만
            print("⚠️  양호한 성능, 캐싱 고려")
        else:
            print("❌ 성능 최적화 필요")

if __name__ == "__main__":
    main()