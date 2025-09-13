package com.ssafy.myissue.user.service;

import com.ssafy.myissue.common.exception.CustomException;
import com.ssafy.myissue.user.dto.TokenPairResponse;

public interface AuthService {
    TokenPairResponse registerOrLogin(String deviceUuid);
    TokenPairResponse rotateRefresh(String refreshToken) throws CustomException;
}
