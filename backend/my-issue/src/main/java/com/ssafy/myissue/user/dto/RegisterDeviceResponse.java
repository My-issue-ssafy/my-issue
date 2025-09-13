package com.ssafy.myissue.user.dto;

public record RegisterDeviceResponse(Long userId) {
    public static RegisterDeviceResponse from(Long userId) {
        return new RegisterDeviceResponse(userId);
    }
}
