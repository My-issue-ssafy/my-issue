package com.ssafy.myissue.news.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "news_scrap",
        // 같은 user가 같은 news를 두 번 스크랩하는 것을 DB 차원에서 금지 (무결성)
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_news",
                columnNames = {"user_id", "news_id"}
        ),
        // 무한 스크롤 커서(lastId = scrap_id) 최적화 핵심 인덱스
        indexes = {
                @Index(name = "idx_scrap_user_cursor", columnList = "user_id, scrap_id")
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    // @ManyToOne(fetch=LAZY, optional=false): 목록 시 불필요 로딩 방지 + 스크랩은 항상 뉴스에 소속(무결성).
    @JoinColumn(name = "news_id", nullable = false)
    // @JoinColumn: FK 컬럼 매핑(여기서는 news_scrap.news_id → news.news_id), nullable=false로 DB에서 NULL 금지.
    private News news;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private NewsScrap(Long userId, News news, LocalDateTime createdAt) {
        this.userId = userId;
        this.news = news;
        this.createdAt = createdAt; // null이면 @PrePersist에서 생성될 때 now()로 보정
    }

    @PrePersist
    void prePersist() { // 생성 시각
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
    }
}
