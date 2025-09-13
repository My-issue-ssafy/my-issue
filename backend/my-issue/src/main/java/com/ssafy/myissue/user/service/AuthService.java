package com.ssafy.myissue.user.service;

import com.ssafy.myissue.user.dto.TokenPairResponse;

public interface AuthService {
    TokenPairResponse registerOrLogin(String deviceUuid);
}
