package com.ssafy.myissue.news.infrastructure;

import com.ssafy.myissue.news.domain.News;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/** 기본 CRUD + 커스텀(QueryDSL) 혼합 */
public interface NewsRepository extends JpaRepository<News, Long>, NewsCustomRepository {
    @Query("SELECT n FROM News n WHERE n.createdAt BETWEEN :start AND :end ORDER BY n.views DESC")
    List<News> findTop10ByDate(@Param("start") LocalDateTime start,
                               @Param("end") LocalDateTime end,
                               Pageable pageable);

    @Query("SELECT n.id AS id, n.views AS views, n.createdAt AS createdAt, n.scrapCount AS scrapCount FROM News n WHERE n.createdAt >= :since AND n.views >= :minViews AND n.scrapCount >= :minScraps")
    List<HotNewsCandidates> findHotCandidates(@Param("since") LocalDateTime since,
                                              @Param("minViews") int minViews,
                                              @Param("minScraps") int minScraps);
}
