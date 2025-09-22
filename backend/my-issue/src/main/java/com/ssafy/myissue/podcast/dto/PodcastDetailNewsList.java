package com.ssafy.myissue.podcast.dto;

import com.ssafy.myissue.news.domain.News;
import com.ssafy.myissue.podcast.domain.PodcastNews;

public record PodcastDetailNewsList(Long newsId, String thumbnailUrl, String title, String category) {
    public static PodcastDetailNewsList of(News news) {
        return new PodcastDetailNewsList(news.getId(), news.getThumbnail(), news.getTitle(), news.getCategory());
    }
}
