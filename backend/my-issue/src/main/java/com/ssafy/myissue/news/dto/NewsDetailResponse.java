package com.ssafy.myissue.news.dto;

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
        int views
) {}
