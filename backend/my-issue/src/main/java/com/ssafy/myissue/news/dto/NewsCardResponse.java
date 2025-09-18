package com.ssafy.myissue.news.dto;

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

) {}
