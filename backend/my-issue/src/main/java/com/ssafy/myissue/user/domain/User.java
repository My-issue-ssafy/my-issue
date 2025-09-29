package com.ssafy.myissue.user.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Getter
@Builder
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String uuid;

    @Column(name = "fcm_token")
    private String fcmToken;

    @Builder.Default // Builder는 필드 초기갑 무시
    @Column(name = "notification_enabled")
    private boolean notificationEnabled = false;

    private Instant lastSeen;

    public static User newOf(String uuid, String fcmToken) {
        return User.builder()
                .uuid(uuid)
                .fcmToken(fcmToken)
                .lastSeen(Instant.now())
                .build();
    }

    public void updateFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public void updateNotificationEnabled() {
        this.notificationEnabled = !this.notificationEnabled;
    }
}
