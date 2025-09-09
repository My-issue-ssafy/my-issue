package com.ssafy.myissue.news.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "news_image",
        // UNIQUE(news_id, image_index) → DB가 유니크 인덱스 자동 생성
        //  - 같은 기사 내 순번 중복 방지(무결성)
        //  - WHERE news_id=? AND ORDER BY image_index 최적화(인덱스 범위 스캔, 추가 정렬 불필요)
        uniqueConstraints = @UniqueConstraint(
                name = "uk_news_image_position",
                columnNames = {"news_id", "image_index"}
        )
)
public class NewsImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Long imageId;

    // @JoinColumn: FK(news_image.news_id → news.news_id), nullable=false로 DB에서 NULL 금지
    // LAZY: 목록 시 불필요 로딩 방지, optional=false: 항상 뉴스에 소속(무결성)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "news_id", nullable = false)
    private News news;

    /** 이미지 URL 또는 경로(불변) */
    @Column(name = "image", nullable = false, length = 500, updatable = false)
    private String image;

    /** 기사 내 이미지 순서(0부터, 불변) */
    @Column(name = "image_index", nullable = false, updatable = false)
    private int imageIndex;

    @Builder
    private NewsImage(News news, String image, int imageIndex) {
        this.news = news;
        this.image = image;
        this.imageIndex = imageIndex;
    }
}
