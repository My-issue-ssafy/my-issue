package com.ssafy.myissue.user.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RefreshStore {
    private final StringRedisTemplate redis;

    public void save(String token, String userId, String uuid, String jt1, Duration expiration) {
        String key = "RT:" + token;
        redis.opsForHash().putAll(key, Map.of(
                "userId", userId,
                "uuid", uuid,
                "jt1", jt1
        ));
        redis.expire(key, expiration);
    }

    public Optional<RefreshData> get(String token) {
        String key = "RT:" + token;
        var m = redis.opsForHash().entries(key);
        if (m == null || m.isEmpty()) return Optional.empty();
        return Optional.of(new RefreshData(
                (String) m.get("userId"),
                (String) m.get("uuid"),
                (String) m.get("jt1")
        ));
    }

    public void delete(String token) {
        redis.delete("RT:" + token);
    }

    public record RefreshData(String userId, String uuid, String jti){}
}
