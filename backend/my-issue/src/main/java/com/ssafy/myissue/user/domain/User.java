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

    private Instant lastSeen;

    public static User newOf(String uuid) {
        return User.builder()
                .uuid(uuid)
                .lastSeen(Instant.now())
                .build();
    }

    public void updateFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }
}
