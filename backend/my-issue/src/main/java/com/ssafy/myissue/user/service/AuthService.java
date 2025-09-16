package com.ssafy.myissue.user.service;

import com.ssafy.myissue.common.exception.CustomException;
import com.ssafy.myissue.user.dto.TokenPairResponse;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {
    TokenPairResponse registerOrLogin(String deviceUuid, HttpServletResponse response);
    void rotateRefresh(String refreshToken, HttpServletResponse response) throws CustomException;
    void registerFcmToken(Long userId, String fcmToken);
}
