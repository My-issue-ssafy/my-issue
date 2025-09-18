package com.ssafy.myissue.news.infrastructure;

import java.time.LocalDateTime;

public interface HotNewsCandidates {
    Long getId();
    LocalDateTime getCreatedAt();
    int getViews();
    int getScrapCount();
}
