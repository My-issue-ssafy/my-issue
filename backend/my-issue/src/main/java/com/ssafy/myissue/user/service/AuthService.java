package com.ssafy.myissue.user.service;

import com.ssafy.myissue.common.exception.CustomException;
import com.ssafy.myissue.user.dto.TokenPairResponse;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {
    TokenPairResponse registerOrLogin(String deviceUuid, HttpServletResponse response);
    TokenPairResponse rotateRefresh(String refreshToken, HttpServletResponse response) throws CustomException;
}
