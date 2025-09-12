package com.ssafy.myissue.user.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
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
    private final Key key;

    public JwtIssuer(@Value("${jwt.secret-base64}") String base64Secret) {
        // Base64 문자열을 실제 바이트 배열로 디코딩
        byte[] secretBytes = Base64.getDecoder().decode(base64Secret);
        // HMAC-SHA 알고리즘을 사용하여 Key 객체 생성
        this.key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secretBytes));
    }

    // Access JWT 발급 (jti: JWT IDentifier, 15분 만료)
    public String createAccess(String userId, Long uuid, String jti) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(userId)           // sub: 사용자 식별자
                .claim("uuid", uuid)      // 커스텀 클레임
                .setId(jti)                  // jti: JWT 고유 식별자
                .setIssuedAt(Date.from(now))         // iat: 발급 시간
                .setExpiration(Date.from(now.plus(Duration.ofMinutes(30))))  // exp: 만료 시간 (30분)
                .signWith(key, SignatureAlgorithm.ES256)            // 서명
                .compact();
    }

    // Access JWT 파싱/검증 (서명/만료 확인)
    public Claims parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)          // 서명 검증을 위한 키 설정
                .build()
                .parseClaimsJws(token)       // 토큰 파싱 및 서명 검증
                .getBody();                  // 클레임 반환
    }

    // Refresh JWT 발급 (jti: JWT IDentifier, 7일 만료)
    public String randomRefresh() {
        return UUID.randomUUID().toString().replace("-", "") +
                UUID.randomUUID().toString().replace("-", "");
    }
}
