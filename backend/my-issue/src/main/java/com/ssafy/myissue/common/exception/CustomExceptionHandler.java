package com.ssafy.myissue.common.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CustomExceptionHandler {
    @ExceptionHandler(CustomException.class)
    private ResponseEntity<ErrorDto> handleCustomException(CustomException e) {
        return ErrorDto.toResponseEntity(e);
    }
}