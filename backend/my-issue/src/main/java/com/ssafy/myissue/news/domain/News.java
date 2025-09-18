package com.ssafy.myissue.news.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

import org.hibernate.annotations.Formula;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 파라미터 없는 생성자를 protected로 만들어라
@Entity
@Table(
        name = "news",
        indexes = {
                @Index(name = "idx_news_created_at", columnList = "created_at, id"),
                // 최신 뉴스 전체 조회 무한 스크롤 ㄱㄴ
                @Index(name = "idx_news_views_created", columnList = "views, created_at, id"),
                // HOT 뉴스 전체 조회 무한 스크롤 ㄱㄴ
                @Index(name = "idx_news_category_cursor", columnList = "category, id")
                // 카테고리별 뉴스 전체 조회 무한 스크롤 ㄱㄴ
        }
)
public class News {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // PK 값을 DB가 자동으로 생성하도록 지정하는 JPA 어노테이션. DB auto-increment
    @Column(name = "id")
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "jsonb", nullable = false)
//    @Column(columnDefinition = "text", nullable = false)
    private String content;

    @Column(nullable = false)
    private String category;

    @Column(nullable = true)
    private String author;

    @Column(nullable = true)
    private String newsPaper;

    @Column(nullable = true)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private int views = 0;

    @Formula("embedding::text")
    private String embedding;

    @Column(nullable = true)
    private String thumbnail;

    @Column(nullable = true)
    private int scrapCount = 0;

    public void increaseScrapCount() { this.scrapCount++; }
    public void decreaseScrapCount() { this.scrapCount--; }
    public void increaseViews() { this.views++; }
}
