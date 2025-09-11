package com.ssafy.myissue.news.dto;

import java.util.List;

/** 홈 상단 3섹션(HOT 5, 추천 5, 최신 5) */
public record NewsHomeResponse(
        List<NewsCardResponse> hot,
        List<NewsCardResponse> recommend,
        List<NewsCardResponse> latest
) {}
