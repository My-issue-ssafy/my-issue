package com.ssafy.myissue.user.service.impl;

import com.ssafy.myissue.common.exception.CustomException;
import com.ssafy.myissue.common.exception.ErrorCode;
import com.ssafy.myissue.user.domain.User;
import com.ssafy.myissue.user.dto.TokenPairResponse;
import com.ssafy.myissue.user.infrastructure.UserRepository;
import com.ssafy.myissue.user.service.AuthService;
import com.ssafy.myissue.user.token.JwtIssuer;
import com.ssafy.myissue.user.token.RefreshStore;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final JwtIssuer jwtIssuer;
    private final RefreshStore refreshStore;

    @Override
    public TokenPairResponse registerOrLogin(String deviceUuid) {
        User user = userRepository.findByUuid(deviceUuid);

        // 등록되지 않은 uuid -> 등록
        if(user == null) {
            user = userRepository.save(User.newOf(deviceUuid));
        }

        return createJwt(user.getId());
    }

    @Override
    public TokenPairResponse rotateRefresh(String refreshToken) throws CustomException {
        Claims claims;
        try {
            // 1. JWT 파싱 & 만료 확인
            claims = jwtIssuer.parse(refreshToken);
        } catch (ExpiredJwtException e) {
            // exp 지난 경우 → 만료된 Refresh Token
            log.warn("만료된 Refresh Token");
            throw new CustomException(ErrorCode.EXPIRED_REFRESH_TOKEN);
        } catch (JwtException e) {
            // 형식이 아예 깨진 경우
            log.warn("잘못된 Refresh Token");
            throw new CustomException(ErrorCode.MALFORMED_REFRESH_TOKEN);
        }

        Long userId = claims.get("userId", Long.class);
        String jti = claims.getId();
        log.debug("[RefreshToken 확인] userId: {}, jti: {}", userId, jti);

        // Redis에 저장된 최신 jti 확인
        String latest = refreshStore.findLatestJti(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_REFRESH_TOKEN));

        // jti 불일치 → 이미 rotate 됐거나 위조됨
        if (!jti.equals(latest)) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        return createJwt(userId);
    }

    // 토큰 발급 로직
    private TokenPairResponse createJwt(Long userId) {
        // 토큰 발급
        String access  = jwtIssuer.createAccess(userId);
        String refresh = jwtIssuer.createRefresh(userId);

        // 토큰 저장
        String jti = jwtIssuer.getJti(refresh);
        refreshStore.saveLatestJti(userId, jti, Duration.ofDays(14));

        return TokenPairResponse.of(access, refresh, userId);
    }
}
