package com.ssafy.myissue.news.entity;

import jakarta.persistence.*;
import lombok.AccessLevel; // Lombok이 생성해주는 getter, setter 등 접근 제어자를 지정할 때 씀. (public, protect, package 등)
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 파라미터 없는 생성자를 protected로 만들어라
@Entity
@Table(
        name = "news",
        indexes = {
                @Index(name = "idx_news_created_at", columnList = "created_at"),
                @Index(name = "idx_news_views_created", columnList = "views, created_at"),
                @Index(name = "idx_news_category_id", columnList = "category, news_id")
        }
)
public class News {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // PK 값을 DB가 자동으로 생성하도록 지정하는 JPA 어노테이션.
    @Column(name = "news_id")
    private Long newsId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 500)
    private String summary;

    @Lob
    @Column(nullable = false)
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

    @Builder
    private News(String title, String summary, String content, NewsCategory category,
                 String author, String newspaper, LocalDateTime createdAt) {
        this.title = title;
        this.summary = summary;
        this.content = content;
        this.category = category;
        this.author = author;
        this.newspaper = newspaper;
        this.createdAt = createdAt;
        this.views = 0;
    }

    /** 생성/삽입 직전에 createdAt이 비어 있으면 자동 세팅 */
    @PrePersist
    void onCreate() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
    }

    public void increaseViews() { this.views++; }
}
