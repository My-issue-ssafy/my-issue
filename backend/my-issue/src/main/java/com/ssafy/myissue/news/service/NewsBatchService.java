package com.ssafy.myissue.news.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import com.ssafy.myissue.news.domain.News;
import com.ssafy.myissue.news.domain.NewsDocument;
import com.ssafy.myissue.news.infrastructure.NewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsBatchService {
    private final NewsRepository newsRepository; // JPA
    private final ElasticsearchClient elasticsearchClient;

    @Transactional(readOnly = true)
    public void reindexAll() throws IOException {
        int pageSize = 1000;
        int page = 0;

        while (true) {
            Pageable pageable = PageRequest.of(page, pageSize);
            List<News> batch = newsRepository.findAll(pageable).getContent();

            if (batch.isEmpty()) break;

            BulkRequest.Builder br = new BulkRequest.Builder();

            for (News news : batch) {
                // ✅ News → NewsDocument 변환
                NewsDocument doc = NewsDocument.builder()
                        .id(news.getId())
                        .title(news.getTitle())
                        .content(news.getContent())
                        .category(news.getCategory())
                        .author(news.getAuthor())
                        .newsPaper(news.getNewsPaper())
                        .createdAt(news.getCreatedAt().toString()) // ISO 8601 문자열로 변환
                        .build();

                br.operations(op -> op
                        .index(idx -> idx
                                .index("news")
                                .id(String.valueOf(news.getId()))
                                .document(doc) // ✅ ES Document만 저장
                        )
                );
            }

            // ✅ Bulk 실행
            BulkResponse response = elasticsearchClient.bulk(br.build());

            // ✅ 로그 출력
            if (response.errors()) {
                log.error("Bulk indexing had failures!");
                for (BulkResponseItem item : response.items()) {
                    if (item.error() != null) {
                        log.error("Failed to index docId={} error={}",
                                item.id(), item.error().reason());
                    }
                }
            } else {
                log.info("Indexed {} documents successfully (page={})",
                        batch.size(), page);
            }

            page++;
        }
        log.info("✅ Reindexing completed!");
    }
}
