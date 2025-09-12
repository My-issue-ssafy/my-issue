package com.ssafy.myissue.news.dto;

/** 최신/카테고리 키셋 페이징 커서 키 */
public record LatestCursor(long createdAtSec, long newsId) {}
// 지금 NewsRepositoryImpl을 보면 createdAt, newsId가 모두 DESC 정렬. 그래서 커서도 동일한 키로 구성해야 직전 마지막보다 작은 것만 정확하게 가져올 수 있음.
