package com.ssafy.myissue.news.dto;

import java.time.LocalDateTime;

public record HotNewsCandidates(Long id, LocalDateTime createdAt, int views, int scrapCount) {
}
