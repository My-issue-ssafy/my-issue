package com.ssafy.myissue.podcast.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Table(name = "podcast_subtitle")
public class PodcastSubtitle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "podcast_id")
    private Podcast podcast;

    private int speaker;
    private String line;

    @Column(name = "start_time")
    private long startTime;

    public static PodcastSubtitle of(Podcast podcast, int speaker, String line, long startTime) {
        return PodcastSubtitle.builder()
                .podcast(podcast)
                .speaker(speaker)
                .line(line)
                .startTime((int)startTime)
                .build();
    }
}
