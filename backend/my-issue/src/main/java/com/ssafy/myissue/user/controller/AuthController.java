package com.ssafy.myissue.user.controller;

import com.ssafy.myissue.user.dto.RegisterDeviceRequest;
import com.ssafy.myissue.user.dto.RegisterDeviceResponse;
import com.ssafy.myissue.user.dto.RegisterFcmTokenRequest;
import com.ssafy.myissue.user.dto.TokenPairResponse;
import com.ssafy.myissue.user.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/auth")
@Tag(name = "Auth", description = "인증/인가(Auth) API")
public class AuthController {

    private final AuthService authService;

    // accessToken: 헤더, refreshToken: 쿠키
    // HttpServletResponse header에 accessToken 담아야 하니 받아야 함
    @PostMapping("/device")
    @Operation(
        summary = "device 등록 및 jwt 토큰 발급 API",
        description = """
                ### - 클라이언트에서 디바이스 UUID를 전달하면, 신규 가입 또는 기존 사용자 로그인 처리</li>
                ### - 성공 시 AccessToken은 <b>응답 헤더(Authorization: Bearer ...)</b>, RefreshToken은 <b>쿠키(refreshToken)</b>로 내려감</li>
                """
    )
    public ResponseEntity<RegisterDeviceResponse> registerDevice(@RequestBody RegisterDeviceRequest req, HttpServletResponse response) {
        log.debug("[Device 등록 - RequestBody] deviceUuid: {}", req.deviceUuid());

        TokenPairResponse tokenPairResponse = authService.registerOrLogin(req.deviceUuid(), response);

        return ResponseEntity.ok(RegisterDeviceResponse.from(tokenPairResponse.userId()));
    }

    @PostMapping("/reissue")
    @Operation(
        summary = "토큰 재발급 API",
        description = """
                ### - AccessToken이 만료되었을 때, RefreshToken을 이용하여 AccessToken과 RefreshToken을 재발급
                ### - RefreshToken을 쿠키(<code>refreshToken</code>)로 전달
                ### - AccessToken은 <b>응답 헤더(Authorization: Bearer ...)</b>, RefreshToken은 <b>쿠키(refreshToken)</b>로 내려감
                ### - RefreshToken은 매번 재발급(rotate)되며, 기존 토큰은 무효화
                """
    )
    public ResponseEntity<Void> rotateRefresh(@Parameter(hidden = true) @CookieValue("refreshToken") String refresh, HttpServletResponse response) {
        log.debug("[Refresh 재발급 - CookieValue] refreshToken: {}", refresh);
        authService.rotateRefresh(refresh, response);

        return ResponseEntity.noContent().build();
    }


    @PostMapping("/fcm")
    @Operation(
            summary = "유저 FCM 토큰 등록 API",
            description = """
                    ### - FCM 토큰을 등록
                    ### - 헤더의 AccessToken을 통해 사용자 인증
                    ### - 요청 바디에 FCM 토큰 전달
                    ### - 성공 시 204 No Content 응답
                """
    )
    public ResponseEntity<Void> registerFcmToken(@AuthenticationPrincipal Long userId, @RequestBody RegisterFcmTokenRequest request) {
        log.debug("[FCM 토큰 등록 - RequestBody] fcmToken: {}", request.fcmToken());
        authService.registerFcmToken(userId, request.fcmToken());

        return ResponseEntity.noContent().build();
    }
}
