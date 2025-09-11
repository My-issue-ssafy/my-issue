package com.ssafy.myissue.news.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 파라미터 없는 생성자를 protected로 만들어라
@Entity
@Table(
        name = "news",
        indexes = {
                @Index(name = "idx_news_created_at", columnList = "created_at, news_id"),
                // 최신 뉴스 전체 조회 무한 스크롤 ㄱㄴ
                @Index(name = "idx_news_views_created", columnList = "views, created_at, news_id"),
                // HOT 뉴스 전체 조회 무한 스크롤 ㄱㄴ
                @Index(name = "idx_news_category_cursor", columnList = "category, news_id")
                // 카테고리별 뉴스 전체 조회 무한 스크롤 ㄱㄴ
        }
)
public class News {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // PK 값을 DB가 자동으로 생성하도록 지정하는 JPA 어노테이션. DB auto-increment
    @Column(name = "news_id")
    private Long newsId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "jsonb")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NewsCategory category;

    @Column(nullable = false, length = 80)
    private String author;

    @Column(nullable = false, length = 80)
    private String newspaper;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private int views;

    @Column(columnDefinition = "vector(768)")
    private String embedding;

    @Column(nullable = false)
    private String thumbnail;


    /** 생성/삽입 직전에 createdAt이 비어 있으면 자동 세팅 */
    @PrePersist
    void onCreate() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
    }

    public void increaseViews() { this.views++; }
}
