package com.ssafy.myissue.podcast.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Table(name = "podcast")
public class Podcast {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String audio;
    private LocalDate date;
    private String keyword;

    public static Podcast of(String audio, LocalDate date, String keyword) {
        return Podcast.builder()
                .audio(audio)
                .date(date)
                .keyword(keyword)
                .build();
    }
}
