package com.ssafy.myissue.notification.domain;

import com.ssafy.myissue.news.domain.News;
import com.ssafy.myissue.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") // Foreign key column name
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "news_id")
    private News news;

    private String content;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    private boolean read;
}
