package com.ssafy.myissue.news.dto;

import java.util.List;

/** 공통 페이지 응답 형태 */
public record CursorPage<T>(List<T> items, String nextCursor, boolean hasNext) {}

