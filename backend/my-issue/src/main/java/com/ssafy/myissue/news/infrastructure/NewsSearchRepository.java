package com.ssafy.myissue.news.infrastructure;

import com.ssafy.myissue.news.domain.NewsDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface NewsSearchRepository extends ElasticsearchRepository<NewsDocument, Long> {
}
