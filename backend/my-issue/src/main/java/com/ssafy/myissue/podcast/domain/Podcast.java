package com.ssafy.myissue.podcast.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

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

    public static Podcast of(String audio, LocalDate date, List<String> keywords) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String jsonKeywords = mapper.writeValueAsString(keywords); // ["AI","뉴스","경제"]
            return Podcast.builder()
                .audio(audio)
                .date(date)
                .keyword(jsonKeywords)
                .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("키워드 직렬화 실패", e);
        }
    }
}
