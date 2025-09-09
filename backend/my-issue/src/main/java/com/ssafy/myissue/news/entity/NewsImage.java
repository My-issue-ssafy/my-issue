package com.ssafy.myissue.news.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "news_image",
        indexes = {
                // 한 기사 내 이미지 정렬(대표 이미지 index=0)
                @Index(name = "idx_news_image_news", columnList = "news_id, image_index")
        }
)
public class NewsImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Long imageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "news_id", nullable = false)
    private News news;

    /** ERD: image (URL or 경로) */
    @Column(name = "image", nullable = false, length = 500)
    private String image;

    /** ERD: image_index (0부터 시작) */
    @Column(name = "image_index", nullable = false)
    private int imageIndex;

    protected NewsImage() {}

    public NewsImage(News news, String image, int imageIndex) {
        this.news = news;
        this.image = image;
        this.imageIndex = imageIndex;
    }

    // --- getters ---
    public Long getImageId() { return imageId; }
    public News getNews() { return news; }
    public String getImage() { return image; }
    public int getImageIndex() { return imageIndex; }
}
