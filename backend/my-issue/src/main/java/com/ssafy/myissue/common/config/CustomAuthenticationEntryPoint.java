package com.ssafy.myissue.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.myissue.common.exception.CustomException;
import com.ssafy.myissue.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper;

    public CustomAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        // JwtAuthenticationFilter에서 넘겨준 message -> ErrorCode name
        ErrorCode errorCode;
        try {
            errorCode = ErrorCode.valueOf(authException.getMessage());
        } catch (Exception e) {
            errorCode = ErrorCode.INVALID_ACCESS_TOKEN; // fallback
        }

        // CustomException 생성
        CustomException customException = new CustomException(errorCode);

        // 응답 세팅
        response.setStatus(customException.getStatus().value());
        response.setContentType("application/json;charset=UTF-8");

        // JSON 바디 만들기 (CustomException 그대로 직렬화할 수도 있지만, 보통 DTO 형태로 정리)
        Map<String, Object> body = Map.of(
                "status", customException.getStatus().value(),
                "errorCode", customException.getErrorCode(),
                "message", customException.getErrorMessage(),
                "path", request.getRequestURI()
        );

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
