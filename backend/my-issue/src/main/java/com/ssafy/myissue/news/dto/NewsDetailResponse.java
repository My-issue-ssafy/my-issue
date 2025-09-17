package com.ssafy.myissue.news.dto;

import com.ssafy.myissue.news.domain.News;

import java.time.LocalDateTime;
import java.util.List;

// 상세 페이지 데이터
public record NewsDetailResponse(
        long newsId,
        String title,
        List<ContentBlock> content,
        String category,
        String author,
        String newspaper,
        LocalDateTime createdAt,
        int views,
        int scrapCount
) {
    public static NewsDetailResponse from(News news, List<ContentBlock> blocks){
        return new NewsDetailResponse(
                news.getId(),
                news.getTitle(),
                blocks,
                news.getCategory(),
                news.getAuthor(),
                news.getNewsPaper(),
                news.getCreatedAt(),
                news.getViews(),
                news.getScrapCount()
        );
    }
}
