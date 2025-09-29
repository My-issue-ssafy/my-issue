package com.ssafy.myissue.news.infrastructure;

import com.ssafy.myissue.news.domain.NewsScrap;
import org.springframework.data.jpa.repository.JpaRepository;

/** 스크랩 저장/삭제용 기본 CRUD 레포지토리 */
public interface NewsScrapRepository extends JpaRepository<NewsScrap, Long> {
    boolean existsByNewsIdAndUserId(Long newsId, Long userId);
}
