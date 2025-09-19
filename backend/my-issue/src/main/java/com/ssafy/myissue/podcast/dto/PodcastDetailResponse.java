package com.ssafy.myissue.podcast.dto;

import java.util.List;

public record PodcastDetailResponse(String keyword, List<PodcastDetailNewsList> podcastDetailNewsList) {
}
