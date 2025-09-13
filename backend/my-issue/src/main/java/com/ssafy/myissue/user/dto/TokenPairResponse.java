package com.ssafy.myissue.user.dto;

import com.ssafy.myissue.user.domain.User;

public record TokenPairResponse(String accessToken, String refreshToken, User user) {
    public static TokenPairResponse of(String accessToken, String refreshToken, User user) {
        return new TokenPairResponse(accessToken, refreshToken, user);
    }
}
