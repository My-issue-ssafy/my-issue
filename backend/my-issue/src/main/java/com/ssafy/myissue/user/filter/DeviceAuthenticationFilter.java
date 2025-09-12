package com.ssafy.myissue.user.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.myissue.user.jwt.JwtIssuer;
import com.ssafy.myissue.user.jwt.RefreshStore;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

public class DeviceAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    private final ObjectMapper om = new ObjectMapper();
    private final JwtIssuer jwt;
    private final RefreshStore store;

    public DeviceAuthenticationFilter(AuthenticationManager am, JwtIssuer jwt, RefreshStore store) {
        super(new AntPathRequestMatcher("/auth/device", "POST"));
        setAuthenticationManager(am);
        this.jwt = jwt;
        this.store = store;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest req, HttpServletResponse res) {
        try {
            String body = StreamUtils.copyToString(req.getInputStream(), StandardCharsets.UTF_8);
            String uuid;
            if (body != null && !body.isBlank()) {
                Map<?, ?> json = om.readValue(body, Map.class);
                uuid = json.get("deviceUuid") == null ? null : String.valueOf(json.get("deviceUuid"));
            } else {
                uuid = req.getHeader("X-Device-UUID"); // 백업 경로
            }
            if (uuid == null || uuid.isBlank()) throw new IllegalArgumentException("deviceUuid is required");
            return getAuthenticationManager().authenticate(DeviceAuthenticationToken.unauthenticated(uuid));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest req, HttpServletResponse res, FilterChain chain, Authentication auth) {
        String userId = auth.getName(); // 여기서는 uuid = userId
        String jti = UUID.randomUUID().toString();

        // 1) Access → Authorization 헤더
        String access = jwt.createAccess(userId, null, jti);
        res.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + access);
        // 브라우저에서 읽게 노출
        res.setHeader("Access-Control-Expose-Headers", "Authorization");

        // 2) Refresh(opaque) → Redis 저장 + HttpOnly 쿠키로 회전 발급
        String refresh = jwt.randomRefresh();
        store.save(refresh, userId, userId, jti, Duration.ofDays(30));

        ResponseCookie cookie = ResponseCookie.from("refreshToken", refresh)
                .httpOnly(true).secure(true)      // 프로덕션: HTTPS 필수
                .sameSite("Strict")               // 크로스도메인이면 None; Secure
                .path("/auth/refresh")            // 범위 최소화
                .maxAge(Duration.ofDays(30))
                .build();
        res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        res.setStatus(HttpServletResponse.SC_OK);
    }
}
