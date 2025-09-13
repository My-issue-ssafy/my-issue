package com.ssafy.myissue.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CustomException extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;
    private final String errorMessage;

    public CustomException(ErrorDto errorDto) {
        super(errorDto.message());
        this.status = errorDto.httpStatus();
        this.errorCode = errorDto.code();
        this.errorMessage = errorDto.message();
    }
}