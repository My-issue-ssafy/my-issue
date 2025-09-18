package com.ssafy.myissue.podcast.service;

import com.fasterxml.jackson.core.JsonProcessingException;

public interface PodcastService {


    void generateDailyPodcast() throws JsonProcessingException; // 매일 팟캐스트 생성 메서드
}

