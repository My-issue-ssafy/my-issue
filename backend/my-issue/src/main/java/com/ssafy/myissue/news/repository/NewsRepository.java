package com.ssafy.myissue.news.repository;

import com.ssafy.myissue.news.entity.News;
import org.springframework.data.jpa.repository.JpaRepository;

/** 기본 CRUD + 커스텀(QueryDSL) 혼합 */
public interface NewsRepository extends JpaRepository<News, Long>, NewsCustomRepository {
}
