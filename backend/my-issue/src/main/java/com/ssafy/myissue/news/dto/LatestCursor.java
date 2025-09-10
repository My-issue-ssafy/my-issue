package com.ssafy.myissue.news.dto;

/** 최신/카테고리 키셋 페이징 커서 키 */
public record LatestCursor(long createdAtMs, long newsId) {}
