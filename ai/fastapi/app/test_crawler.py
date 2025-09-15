# fetch_and_parse 함수 테스트 스크립트

from app.core.crawler.naver_crawler import fetch_and_parse
import json

def test_single_url():
    """단일 URL 크롤링 테스트"""
    
    # 테스트할 네이버 뉴스 URL (최신 뉴스로 바꿔서 테스트)
    test_url = "https://n.news.naver.com/mnews/article/052/0002247302"  # 예시 URL    
    
    print(f"[TEST] 크롤링 시작: {test_url}")
    print("=" * 60)
    
    try:
        # fetch_and_parse 함수 호출
        result = fetch_and_parse(test_url, sid1="100")
        
        if result:
            print("✅ 크롤링 성공!")
            print("\n📋 크롤링 결과:")
            print("-" * 40)
            
            # 주요 정보 출력
            print(f"🔗 URL: {result.get('url', 'N/A')}")
            print(f"📰 제목: {result.get('title', 'N/A')}")
            print(f"📅 발행일: {result.get('published_at', 'N/A')}")
            print(f"🏢 언론사: {result.get('press', 'N/A')}")
            print(f"👤 기자: {result.get('reporter', 'N/A')}")
            print(f"📂 카테고리: {result.get('category', [])}")
            print(f"🕐 크롤링 시간: {result.get('crawled_at', 'N/A')}")
            
            # 본문 정보
            body = result.get('body', [])
            if isinstance(body, list):
                print(f"\n📝 본문 블록 수: {len(body)}개")
                
                # 첫 3개 블록만 미리보기
                for i, block in enumerate(body[:3]):
                    block_type = block.get('type', 'unknown')
                    content = block.get('content', '')
                    
                    if block_type == 'text':
                        preview = content[:100] + "..." if len(content) > 100 else content
                        print(f"  {i+1}. [텍스트] {preview}")
                    elif block_type == 'image':
                        print(f"  {i+1}. [이미지] {content}")
                    else:
                        print(f"  {i+1}. [{block_type}] {str(content)[:50]}...")
                
                if len(body) > 3:
                    print(f"  ... 외 {len(body)-3}개 블록")
            
            # 임베딩 정보 (있다면)
            embedding = result.get('title_embedding')
            if embedding:
                print(f"\n🔢 임베딩 정보:")
                print(f"  - 모델: {embedding.get('model', 'N/A')}")
                print(f"  - 차원: {embedding.get('dim', 'N/A')}")
                print(f"  - 정규화: {embedding.get('normalized', 'N/A')}")
                vector = embedding.get('vector', [])
                if vector:
                    print(f"  - 벡터 미리보기: [{vector[0]:.4f}, {vector[1]:.4f}, ..., {vector[-1]:.4f}]")
            
            print("\n" + "=" * 60)
            print("📄 전체 JSON 결과:")
            print(json.dumps(result, ensure_ascii=False, indent=2))
            
        else:
            print("❌ 크롤링 실패 - 결과가 None입니다")
            print("가능한 원인:")
            print("- 잘못된 URL")
            print("- 네트워크 오류") 
            print("- 네이버 차단")
            print("- 기사가 삭제됨")
            
    except Exception as e:
        print(f"❌ 크롤링 오류: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    test_single_url()