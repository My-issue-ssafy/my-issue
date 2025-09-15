package com.ssafy.myissue.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationProperties;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // 401: Unauthorized | 400: Bad Request | 403 Forbidden | 404 Not Found | 409 Conflict | 410 Gone

    // Refresh Token
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN","해당 Refresh Token이 존재하지 않습니다."),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED,  "EXPIRED_REFRESH_TOKEN","만료된 Refresh Token입니다."),
    MALFORMED_REFRESH_TOKEN(HttpStatus.BAD_REQUEST, "MALFORMED_REFRESH_TOKEN","Refresh Token 형식이 올바르지 않습니다."),
    // Access Token
    EXPIRED_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "EXPIRED_ACCESS_TOKEN", "만료된 Access Token 입니다."),
    MALFORMED_ACCESS_TOKEN(HttpStatus.BAD_REQUEST, "MALFORMED_ACCESS_TOKEN", "Access Token 형식이 올바르지 않습니다."),

    NEWS_NOT_FOUND(HttpStatus.NOT_FOUND, "NEWS_NOT_FOUND", "존재하지 않는 뉴스입니다."),                 // 상세/스크랩 시 대상 없음
    SCRAP_NOT_FOUND(HttpStatus.NOT_FOUND, "SCRAP_NOT_FOUND", "존재하지 않는 스크랩입니다."),            // 필요 시 사용
    UNAUTHORIZED_ACCESS(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED_ACCESS", "인증이 필요합니다."),          // 인증 정보 없음
    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER", "요청 파라미터가 유효하지 않습니다."); // 잘못된 쿼리/커서/size 등


    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}