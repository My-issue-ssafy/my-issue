package com.ssafy.myissue.podcast.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.myissue.podcast.domain.Podcast;
import com.ssafy.myissue.podcast.domain.PodcastNews;

import java.util.List;

public record PodcastResponse(Long podcastId, String thumbnail, String podcastUrl, List<String> keyword, List<Subtitles> subtitles) {
    public static PodcastResponse of(Podcast podcast, PodcastNews podcastNews, List<Subtitles> subtitles) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            // DB에서 가져온 String → List<String> 변환
            List<String> keywords = mapper.readValue(podcastNews.getPodcast().getKeyword(), new TypeReference<>() {});
            return new PodcastResponse(podcast.getId(), podcastNews.getNews().getThumbnail(), podcast.getAudio(), keywords, subtitles);
        } catch (Exception e) {
            throw new RuntimeException("Keyword 파싱 실패", e);
        }
    }
}
