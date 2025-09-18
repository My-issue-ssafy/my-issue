package com.ssafy.myissue.podcast.infrastructure;

import com.ssafy.myissue.podcast.domain.PodcastSubtitle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PodcastSubtitleRepository extends JpaRepository<PodcastSubtitle, Long> {
}
