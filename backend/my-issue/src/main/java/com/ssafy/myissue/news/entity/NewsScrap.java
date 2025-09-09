package com.ssafy.myissue.news.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "news_scrap",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_news", columnNames = {"user_id", "news_id"}),
        indexes = {
                @Index(name = "idx_scrap_user", columnList = "user_id"),
                @Index(name = "idx_scrap_news", columnList = "news_id")
        }
)
public class NewsScrap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "scrap_id")
    private Long scrapId;

    /** 로그인 미도입 시, 디바이스ID→내부 userId 매핑값 사용 */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "news_id", nullable = false)
    private News news;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected NewsScrap() {}

    public NewsScrap(Long userId, News news) {
        this.userId = userId;
        this.news = news;
        this.createdAt = LocalDateTime.now();
    }

    // --- getters ---
    public Long getScrapId() { return scrapId; }
    public Long getUserId() { return userId; }
    public News getNews() { return news; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
