package com.ssafy.myissue.user.controller;

import com.ssafy.myissue.user.component.TokenResponseWriter;
import com.ssafy.myissue.user.dto.RegisterDeviceRequest;
import com.ssafy.myissue.user.dto.RegisterDeviceResponse;
import com.ssafy.myissue.user.dto.TokenPairResponse;
import com.ssafy.myissue.user.service.AuthService;
import com.ssafy.myissue.user.token.JwtIssuer;
import com.ssafy.myissue.user.token.RefreshStore;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final TokenResponseWriter tokenResponseWriter;

    // accessToken: 헤더, refreshToken: 쿠키
    // HttpServletResponse header에 accessToken 담아야 하니 받아야 함
    @PostMapping("/device")
    public ResponseEntity<RegisterDeviceResponse> registerDevice(@RequestBody RegisterDeviceRequest req, HttpServletResponse response) {
        log.debug("[Device 등록 - RequestBody] deviceUuid: {}", req.deviceUuid());

        TokenPairResponse tokenPairResponse = authService.registerOrLogin(req.deviceUuid());
        tokenResponseWriter.write(tokenPairResponse, response);

        return ResponseEntity.ok(RegisterDeviceResponse.from(tokenPairResponse.userId()));
    }

    @PostMapping("/reissue")
    public ResponseEntity<Void> rotateRefresh(@Parameter(hidden = true) @CookieValue("refreshToken") String refresh, HttpServletResponse response) {
        log.debug("[Refresh 재발급 - CookieValue] refreshToken: {}", refresh);

        TokenPairResponse tokenPairResponse = authService.rotateRefresh(refresh);
        tokenResponseWriter.write(tokenPairResponse, response);

        return ResponseEntity.noContent().build();
    }
}
