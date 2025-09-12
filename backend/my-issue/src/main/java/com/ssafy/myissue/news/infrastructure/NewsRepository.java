package com.ssafy.myissue.news.infrastructure;

import com.ssafy.myissue.news.domain.News;
import org.springframework.data.jpa.repository.JpaRepository;

/** 기본 CRUD + 커스텀(QueryDSL) 혼합 */
public interface NewsRepository extends JpaRepository<News, Long>, NewsCustomRepository {
}
