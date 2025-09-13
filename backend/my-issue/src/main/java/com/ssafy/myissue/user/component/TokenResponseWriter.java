package com.ssafy.myissue.user.component;

import com.ssafy.myissue.user.dto.TokenPairResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class TokenResponseWriter {
    public void write(TokenPairResponse tokens, HttpServletResponse resp) {
        String bearer = "Bearer " + tokens.accessToken();
        resp.setHeader(HttpHeaders.AUTHORIZATION, bearer);
        resp.setHeader("X-Access-Token", tokens.accessToken());
        resp.addHeader("Access-Control-Expose-Headers", "Authorization, X-Access-Token");

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", tokens.refreshToken())
                .httpOnly(true).secure(true).path("/").maxAge(Duration.ofDays(14)).sameSite("Strict")
                .build();
        resp.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }
}
