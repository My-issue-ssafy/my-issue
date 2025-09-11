package com.ssafy.myissue.news.infrastructure;

import com.ssafy.myissue.news.domain.News;
import com.ssafy.myissue.news.domain.NewsCategory;
import com.ssafy.myissue.news.domain.NewsScrap;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** 복잡 조회용 커스텀 레포 인터페이스 (QueryDSL 구현은 NewsRepositoryImpl에서) */
public interface NewsCustomRepository {

    List<News> findLatestPage(LocalDateTime lastCreatedAt, Long lastNewsId, int size); // 최신 (created_at desc, news_id desc) 키셋
    List<News> findHotPage(Integer lastViews, LocalDateTime lastCreatedAt, Long lastNewsId, int size); // HOT (views desc, created_at desc, news_id desc) 키셋
    List<News> findCategoryLatestPage(NewsCategory category, LocalDateTime lastCreatedAt, Long lastNewsId, int size); // 카테고리 최신 (category = ?, created_at desc, news_id desc) 키셋


    // 검색(키워드/카테고리) + 최신 정렬 키셋
    List<News> searchPage(String keyword, NewsCategory category, LocalDateTime lastCreatedAt, Long lastNewsId, int size);
    // 이미지: 상세(단건) + 목록 배치(다건)
//    List<NewsImage> findImagesByNewsId(Long newsId);
//    List<NewsImage> findImagesByNewsIds(Collection<Long> newsIds);

    // 스크랩 중복 확인(토글용) + 내 스크랩 목록: 스크랩시각(=scrap_id) 역순 목록
    Optional<NewsScrap> findScrapByUserIdAndNewsId(Long userId, Long newsId);
    List<NewsScrap> findScrapsWithNewsByUser(Long userId, Long lastScrapId, int size);
}
