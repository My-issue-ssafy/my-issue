package com.ssafy.myissue.podcast.infrastructure;

import com.ssafy.myissue.podcast.domain.PodcastSubtitle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PodcastSubtitleRepository extends JpaRepository<PodcastSubtitle, Long> {
    List<PodcastSubtitle> findByPodcast_IdOrderByIdAsc(Long podcastId);
}
