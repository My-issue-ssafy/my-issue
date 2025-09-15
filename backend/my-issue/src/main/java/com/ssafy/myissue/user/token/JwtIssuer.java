package com.ssafy.myissue.user.token;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtIssuer {

    private final Key hmacKey;

    public JwtIssuer(@Value("${jwt.secret-base64}") String base64Secret) {
        byte[] secretBytes = Base64.getDecoder().decode(base64Secret);
        this.hmacKey = Keys.hmacShaKeyFor(secretBytes); // HMAC 키 (32바이트 이상)
    }

    /** Access JWT 발급 */
    public String createAccess(Long userId) {
        return buildJwt(userId, "access", Duration.ofMinutes(30));
    }

    /** Refresh JWT 발급 */
    public String createRefresh(Long userId) {
        return buildJwt(userId, "refresh", Duration.ofDays(14));
    }

    /** 공통 빌더: HS256 + jti + type + exp */
    private String buildJwt(Long userId, String type, Duration ttl) {
        Instant now = Instant.now();
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .setHeaderParam("typ", "JWT")
                .setSubject(String.valueOf(userId))
                .claim("type", type)
                .claim("userId", userId)
                .setId(jti)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(ttl)))
                .signWith(hmacKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /** 파싱/검증: 서명/만료 확인 후 Claims 반환 */ // Claims = JWT Payload(실제 데이터)
    public Claims parse(String token) throws JwtException {
        return Jwts.parserBuilder()
                .setSigningKey(hmacKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isRefresh(String token) {
        try {
            return "refresh".equals(parse(token).get("type", String.class));
        } catch (JwtException e) {
            return false;
        }
    }

    public String getJti(String token) {
        return parse(token).getId();
    }
}