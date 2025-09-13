package com.ssafy.myissue.user.service.impl;

import com.ssafy.myissue.user.domain.User;
import com.ssafy.myissue.user.dto.TokenPairResponse;
import com.ssafy.myissue.user.infrastructure.UserRepository;
import com.ssafy.myissue.user.service.AuthService;
import com.ssafy.myissue.user.token.JwtIssuer;
import com.ssafy.myissue.user.token.RefreshStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

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
        if(user == null) { userRepository.save(User.newOf(deviceUuid)); }

        // 토큰 발급
        String access  = jwtIssuer.createAccess(deviceUuid);
        String refresh = jwtIssuer.createRefresh(deviceUuid);

        // 토큰 저장
        String jti = jwtIssuer.getJti(refresh);
        refreshStore.saveLatestJti(deviceUuid, jti, Duration.ofDays(14));

        return TokenPairResponse.of(access, refresh);
    }
}
