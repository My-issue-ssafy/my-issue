package com.ssafy.myissue.toons.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Toons {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long toonId;

    @Column(nullable = false)
    private Long newsId;

    @Column(nullable = true)
    private String toonImage;

    @Column(columnDefinition = "text")
    private String summary;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private LocalDate date;
}
