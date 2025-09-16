package com.ssafy.myissue.toons.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "toon_id"})
        }
)
public class ToonLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long toonLikeId;

    @Column(nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "toon_id", nullable = false)
    private Toons toon;

    // true = 좋아요, false = 싫어요
    @Column(nullable = false)
    private Boolean liked;
}
