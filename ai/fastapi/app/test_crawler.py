# fetch_and_parse 함수 테스트 스크립트

from app.core.crawler.naver_crawler import fetch_and_parse
import json
from loguru import logger

def test_single_url():
    """단일 URL 크롤링 테스트"""
    
    # 테스트할 네이버 뉴스 URL (최신 뉴스로 바꿔서 테스트)
    test_url = "https://n.news.naver.com/mnews/article/052/0002247302"  # 예시 URL    
    
    logger.info(f"[TEST] 크롤링 시작: {test_url}")
    logger.info("=" * 60)
    
    try:
        # fetch_and_parse 함수 호출
        result = fetch_and_parse(test_url, sid1="100")
        
        if result:
            logger.info("크롤링 성공!")
            logger.info("크롤링 결과:")
            logger.info("-" * 40)
            
            # 주요 정보 출력
            logger.info(f"URL: {result.get('url', 'N/A')}")
            logger.info(f"제목: {result.get('title', 'N/A')}")
            logger.info(f"발행일: {result.get('published_at', 'N/A')}")
            logger.info(f"언론사: {result.get('press', 'N/A')}")
            logger.info(f"기자: {result.get('reporter', 'N/A')}")
            logger.info(f"카테고리: {result.get('category', [])}")
            logger.info(f"크롤링 시간: {result.get('crawled_at', 'N/A')}")
            
            # 본문 정보
            body = result.get('body', [])
            if isinstance(body, list):
                logger.info(f"본문 블록 수: {len(body)}개")
                
                # 첫 3개 블록만 미리보기
                for i, block in enumerate(body[:3]):
                    block_type = block.get('type', 'unknown')
                    content = block.get('content', '')
                    
                    if block_type == 'text':
                        preview = content[:100] + "..." if len(content) > 100 else content
                        logger.info(f"  {i+1}. [텍스트] {preview}")
                    elif block_type == 'image':
                        logger.info(f"  {i+1}. [이미지] {content}")
                    else:
                        logger.info(f"  {i+1}. [{block_type}] {str(content)[:50]}...")
                
                if len(body) > 3:
                    logger.info(f"  ... 외 {len(body)-3}개 블록")
            
            # 임베딩 정보 (있다면)
            embedding = result.get('title_embedding')
            if embedding:
                logger.info("임베딩 정보:")
                logger.info(f"  - 모델: {embedding.get('model', 'N/A')}")
                logger.info(f"  - 차원: {embedding.get('dim', 'N/A')}")
                logger.info(f"  - 정규화: {embedding.get('normalized', 'N/A')}")
                vector = embedding.get('vector', [])
                if vector:
                    logger.info(f"  - 벡터 미리보기: [{vector[0]:.4f}, {vector[1]:.4f}, ..., {vector[-1]:.4f}]")
            
            logger.info("=" * 60)
            logger.info("전체 JSON 결과:")
            logger.info(json.dumps(result, ensure_ascii=False, indent=2))
            
        else:
            logger.error("크롤링 실패 - 결과가 None입니다")
            logger.info("가능한 원인:")
            logger.info("- 잘못된 URL")
            logger.info("- 네트워크 오류")
            logger.info("- 네이버 차단")
            logger.info("- 기사가 삭제됨")
            
    except Exception as e:
        logger.error(f"크롤링 오류: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    test_single_url()