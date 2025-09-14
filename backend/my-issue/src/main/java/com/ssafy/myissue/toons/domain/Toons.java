package com.ssafy.myissue.toons.domain;

import jakarta.persistence.*;
import lombok.*;

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

    @Column(nullable = false)
    private String toonImage;

    @Column(columnDefinition = "text")
    private String summary;
}
