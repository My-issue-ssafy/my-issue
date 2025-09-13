package com.ssafy.myissue.user.controller;

import com.ssafy.myissue.user.component.TokenResponseWriter;
import com.ssafy.myissue.user.dto.RegisterDeviceRequest;
import com.ssafy.myissue.user.dto.TokenPairResponse;
import com.ssafy.myissue.user.service.AuthService;
import com.ssafy.myissue.user.token.JwtIssuer;
import com.ssafy.myissue.user.token.RefreshStore;
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

    private final RefreshStore store;
    private final JwtIssuer jwt;

    private final AuthService authService;
    private final TokenResponseWriter tokenResponseWriter;

    // accessToken: 헤더, refreshToken: 쿠키
    // HttpServletResponse header에 accessToken 담아야 하니 받아야 함
    @PostMapping("/device")
    public ResponseEntity<Void> registerDevice(@RequestBody RegisterDeviceRequest req, HttpServletResponse response) {
        log.debug("[Device 등록 - RequestBody] deviceUuid: {}", req.deviceUuid());

        TokenPairResponse tokenPairResponse = authService.registerOrLogin(req.deviceUuid());
        tokenResponseWriter.write(tokenPairResponse, response);

        return ResponseEntity.noContent().build();
    }

//    @PostMapping("/refresh")
//    public ResponseEntity<Map<String, Object>> rotate(@CookieValue("refreshToken") String refresh) {
//        var data = store.get(refresh).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
//                org.springframework.http.HttpStatus.UNAUTHORIZED, "invalid refresh"));
//
//        // 회전: 기존 토큰 폐기 → 새 토큰 발급/저장
//        store.delete(refresh);
//
//        String jti = UUID.randomUUID().toString();
//        String access = jwt.createAccess(data.userId(), null, jti);
//        String newRefresh = jwt.randomRefresh();
//        store.save(newRefresh, data.userId(), data.uuid(), jti, Duration.ofDays(30));
//
//        var cookie = ResponseCookie.from("refreshToken", newRefresh)
//                .httpOnly(true).secure(true).sameSite("Strict").path("/auth/refresh")
//                .maxAge(Duration.ofDays(30)).build();
//
//        return ResponseEntity.ok()
//                .header(HttpHeaders.SET_COOKIE, cookie.toString())
//                .header(HttpHeaders.AUTHORIZATION, "Bearer " + access)
//                .header("Access-Control-Expose-Headers", "Authorization")
//                .body(Map.of("accessTokenExpiresIn", 900));
//    }
}
