package com.ssafy.myissue.podcast.infrastructure;

import com.ssafy.myissue.podcast.domain.PodcastNews;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PodcastNewsRepository extends JpaRepository<PodcastNews, Long> {
}
