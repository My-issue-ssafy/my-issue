package com.ssafy.myissue.user.dto;

public record TokenPairResponse(String accessToken, String refreshToken) {
    public static TokenPairResponse of(String accessToken, String refreshToken) {
        return new TokenPairResponse(accessToken, refreshToken);
    }
}
