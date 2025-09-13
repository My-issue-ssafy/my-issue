package com.ssafy.myissue.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationProperties;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // 401: Unauthorized | 404: Bad Request | 403 Forbidden | 404 Not Found | 409 Conflict | 410 Gone

    // Refresh Token
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN","해당 Refresh Token이 존재하지 않습니다."),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED,  "EXPIRED_REFRESH_TOKEN","만료된 Refresh Token입니다."),
    MALFORMED_REFRESH_TOKEN(HttpStatus.BAD_REQUEST, "MALFORMED_REFRESH_TOKEN","Refresh Token 형식이 올바르지 않습니다."),
    // Access Token
    EXPIRED_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "EXPIRED_ACCESS_TOKEN", "만료된 Access Token 입니다."),
    MALFORMED_ACCESS_TOKEN(HttpStatus.BAD_REQUEST, "MALFORMED_ACCESS_TOKEN", "Access Token 형식이 올바르지 않습니다.");
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}