package com.ssafy.myissue.news.dto;

import com.ssafy.myissue.news.domain.NewsCategory;
import java.time.LocalDateTime;
import java.util.List;

// 상세 페이지 데이터
public record NewsDetailResponse(
        long newsId,
        String title,
        List<ContentBlock> content,
        NewsCategory category,
        String author,
        String newspaper,
        LocalDateTime createdAt,
        int views
) {}
