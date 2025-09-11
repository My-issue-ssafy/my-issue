package com.ssafy.myissue.news.dto;

import java.util.List;

// 모든 목록응답의 공통 래퍼
public record CursorPage<T>(List<T> items, String nextCursor, boolean hasNext) {}
// 포른트는 hasNext 보고 무한 스크롤 제어, nextCursor를 다음 요청 파라미터로 보냄
