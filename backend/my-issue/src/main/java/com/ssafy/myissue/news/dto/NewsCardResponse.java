package com.ssafy.myissue.news.dto;

import com.ssafy.myissue.news.domain.News;

import java.time.LocalDateTime;

// 목록 카드 한 장의 데이터
public record NewsCardResponse(
        long newsId,
        String title,
        String newspaper,
        LocalDateTime createdAt,
        int views,
        String category,
        String thumbnail

) {
    public static NewsCardResponse from(News news) {
        return new NewsCardResponse(
                news.getId(),
                news.getTitle(),
                news.getNewsPaper(),
                news.getCreatedAt(),
                news.getViews(),
                news.getCategory(),
                news.getThumbnail()
        );
    }
    public static NewsCardResponse toCard(HotNewsCandidates n) {
        return new NewsCardResponse(
                n.getId(),
                n.getTitle(),
                n.getNewsPaper(),
                n.getCreatedAt(),
                n.getViews(),
                n.getCategory(),
                n.getThumbnail()
        );
    }
}
