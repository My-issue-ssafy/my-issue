package com.ssafy.myissue.news.dto;

import java.time.LocalDateTime;

public interface HotNewsCandidates {
    Long getId();
    String getTitle();
    String getThumbnail();
    String getCategory();
    String getNewsPaper();
    LocalDateTime getCreatedAt();
    int getViews();
    int getScrapCount();
}
