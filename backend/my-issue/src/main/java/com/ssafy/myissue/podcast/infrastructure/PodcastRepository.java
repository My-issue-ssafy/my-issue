package com.ssafy.myissue.podcast.infrastructure;

import com.ssafy.myissue.podcast.domain.Podcast;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PodcastRepository extends JpaRepository<Podcast, Long> {

}
