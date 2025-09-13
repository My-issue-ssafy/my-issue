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

    private String key(String uuid) {
        return "refresh-jti:" + uuid;
    }

    /** 최신 Refresh JTI 저장 (TTL = refresh 만료와 동일) */
    public void saveLatestJti(String uuid, String jti, Duration ttl) {
        redis.opsForValue().set(key(uuid), jti, ttl);
    }

    /** subject의 최신 Refresh JTI 조회 */
    public Optional<String> findLatestJti(String subject) {
        return Optional.ofNullable(redis.opsForValue().get(key(subject)));
    }

    /** 로그아웃/강제 무효화 */
    public void delete(String subject) {
        redis.delete(key(subject));
    }
}
