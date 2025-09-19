package com.ssafy.myissue.news.dto;

import java.time.LocalDateTime;

public interface HotNewsCandidates {
    Long getId();
    LocalDateTime getCreatedAt();
    int getViews();
    int getScrapCount();
}
