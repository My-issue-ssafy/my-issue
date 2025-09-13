package com.ssafy.myissue.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CustomException extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;
    private final String errorMessage;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.status = errorCode.getHttpStatus();
        this.errorCode = errorCode.getCode();
        this.errorMessage = errorCode.getMessage();
    }

}