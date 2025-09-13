package com.ssafy.myissue.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    EMPTY_IMAGE(HttpStatus.BAD_REQUEST,"EMPTY_IMAGE","이미지가 비어있습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}