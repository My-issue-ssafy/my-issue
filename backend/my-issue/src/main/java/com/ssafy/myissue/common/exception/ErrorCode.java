package com.ssafy.myissue.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationProperties;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // 401: Unauthorized | 400: Bad Request | 403 Forbidden | 404 Not Found | 409 Conflict | 410 Gone

    /* -------- 시은 ----------*/
    // Refresh Token
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN","해당 Refresh Token이 존재하지 않습니다."),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED,  "EXPIRED_REFRESH_TOKEN","만료된 Refresh Token입니다."),
    MALFORMED_REFRESH_TOKEN(HttpStatus.BAD_REQUEST, "MALFORMED_REFRESH_TOKEN","Refresh Token 형식이 올바르지 않습니다."),
    // Access Token
    EXPIRED_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "EXPIRED_ACCESS_TOKEN", "만료된 Access Token 입니다."),
    MALFORMED_ACCESS_TOKEN(HttpStatus.BAD_REQUEST, "MALFORMED_ACCESS_TOKEN", "Access Token 형식이 올바르지 않습니다."),
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_ACCESS_TOKEN","해당 Access Token이 존재하지 않습니다."),
    EMPTY_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "EMPTY_ACCESS_TOKEN","Access Token이 비어있습니다."),
    BEARER_PREFIX_INVALID(HttpStatus.UNAUTHORIZED, "BEARER_PREFIX_INVALID","Bearer 접두사로 시작하지 않습니다."),
    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "해당 유저를 찾을 수 없습니다."),
    EMPTY_FCM_TOKEN(HttpStatus.NOT_FOUND, "FCM_TOKEN_NOT_FOUND", "FCM Token이 비어있습니다."),
    // Notification
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND", "해당 알림을 찾을 수 없습니다."),
    UNAUTHORIZED_NOTIFICATION(HttpStatus.FORBIDDEN, "UNAUTHORIZED_NOTIFICATION", "해당 알림에 접근할 권한이 없습니다."),
    // Podcast
    PODCAST_DATE_INVALID(HttpStatus.BAD_REQUEST, "PODCAST_DATE_INVALID", "오늘 날짜 이후의 팟캐스트는 조회할 수 없습니다."),
    PODCAST_NOT_FOUND(HttpStatus.NOT_FOUND, "PODCAST_NOT_FOUND", "해당 날짜의 팟캐스트를 찾을 수 없습니다."),
    PODCAST_ID_NOT_FOUNT(HttpStatus.NOT_FOUND, "PODCAST_ID_NOT_FOUND", "해당 팟캐스트를 찾을 수 없습니다."),
    PODCAST_NEWS_NOT_FOUND(HttpStatus.NOT_FOUND,  "PODCAST_NEWS_NOT_FOUND", "팟캐스트에 해당하는 뉴스를 찾을 수 없습니다."),

    /* -------- 진현 ----------*/

    // News
    NEWS_NOT_FOUND(HttpStatus.NOT_FOUND, "NEWS_NOT_FOUND", "존재하지 않는 뉴스입니다."),                 // 상세/스크랩 시 대상 없음
    SCRAP_NOT_FOUND(HttpStatus.NOT_FOUND, "SCRAP_NOT_FOUND", "존재하지 않는 스크랩입니다."),            // 필요 시 사용
    UNAUTHORIZED_ACCESS(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED_ACCESS", "인증이 필요합니다."),          // 인증 정보 없음
    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER", "요청 파라미터가 유효하지 않습니다."), // 잘못된 쿼리/커서/size 등

    TOON_NOT_FOUND(HttpStatus.NOT_FOUND, "TOON_NOT_FOUND", "존재하지 않는 네컷뉴스입니다."),              // [ADDED]
    TOON_LIKE_NOT_FOUND(HttpStatus.NOT_FOUND, "TOON_LIKE_NOT_FOUND", "좋아요/싫어요 기록이 없습니다."),     // [ADDED]
    INVALID_CURSOR(HttpStatus.BAD_REQUEST, "INVALID_CURSOR", "커서가 유효하지 않습니다."), // [ADDED]
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}