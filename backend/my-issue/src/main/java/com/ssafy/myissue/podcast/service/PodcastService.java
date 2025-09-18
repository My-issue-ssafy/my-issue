package com.ssafy.myissue.podcast.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ssafy.myissue.podcast.dto.PodcastResponse;

import java.time.LocalDate;

public interface PodcastService {
    PodcastResponse getPodcast(LocalDate date); // 팟캐스트 조회 메서드

    void generateDailyPodcast() throws JsonProcessingException; // 매일 팟캐스트 생성 메서드
}

