package com.ssafy.myissue.user.dto;

public record TokenPairResponse(String accessToken, String refreshToken, Long userId) {
    public static TokenPairResponse of(String accessToken, String refreshToken, Long userId) {
        return new TokenPairResponse(accessToken, refreshToken, userId);
    }
}
