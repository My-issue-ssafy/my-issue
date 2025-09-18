package com.ssafy.myissue.podcast.infrastructure;

import com.ssafy.myissue.podcast.domain.PodcastNews;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PodcastNewsRepository extends JpaRepository<PodcastNews, Long> {
    PodcastNews findFirstByPodcast_Id(Long podcastId);
    List<PodcastNews> findByPodcast_Id(Long podcastId);
}
