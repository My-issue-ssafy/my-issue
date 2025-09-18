package com.ssafy.myissue.podcast.dto;

public record PodcastDetailNewsList(Long newsId, String thumbnailUrl, String title) {
    public static PodcastDetailNewsList of(Long newsId, String thumbnailUrl, String title) {
        return new PodcastDetailNewsList(newsId, thumbnailUrl, title);
    }
}
