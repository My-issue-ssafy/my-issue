package com.ssafy.myissue.podcast.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ssafy.myissue.podcast.dto.PodcastDetailNewsList;
import com.ssafy.myissue.podcast.dto.PodcastResponse;

import java.time.LocalDate;
import java.util.List;

public interface PodcastService {
    PodcastResponse getPodcast(LocalDate date);
    List<PodcastDetailNewsList> getPodcastNews(Long podcastId);

    void generateDailyPodcast() throws JsonProcessingException; // 매일 팟캐스트 생성 메서드
}

