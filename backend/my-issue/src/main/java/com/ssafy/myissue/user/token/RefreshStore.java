package com.ssafy.myissue.user.token;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RefreshStore {
    private final StringRedisTemplate redis;

    private String key(Long userId) {
        return "refresh-jti:" + userId;
    }

    /** 최신 Refresh JTI 저장 (TTL = refresh 만료와 동일) */
    public void saveLatestJti(Long userId, String jti, Duration ttl) {
        redis.opsForValue().set(key(userId), jti, ttl);
    }

    /** subject의 최신 Refresh JTI 조회 */
    public Optional<String> findLatestJti(Long userId) {
        return Optional.ofNullable(redis.opsForValue().get(key(userId)));
    }
}
