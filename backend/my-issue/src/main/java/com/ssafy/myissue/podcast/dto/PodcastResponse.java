package com.ssafy.myissue.podcast.dto;

import java.util.List;

public record PodcastResponse(Long podcastId, String thumbnail, String podcastUrl, String keyword, List<Subtitles> subtitles) {
    public static PodcastResponse of(Long podcastId, String thumbnail, String podcastUrl, String keyword, List<Subtitles> subtitles) {
        return new PodcastResponse(podcastId, thumbnail, podcastUrl, keyword, subtitles);
    }
}
