package com.ssafy.myissue.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public record ErrorDto(HttpStatus httpStatus, String code, String message) {
    public static ResponseEntity<ErrorDto> toResponseEntity(CustomException ex) {
        return ResponseEntity
                .status(ex.getStatus())
                .body(new ErrorDto(
                        ex.getStatus(),
                        ex.getErrorCode(),
                        ex.getErrorMessage()
                ));
    }
}
