package com.ssafy.myissue.toons.controller;

import com.ssafy.myissue.toons.dto.ToonResponse;
import com.ssafy.myissue.toons.service.ToonsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/toons")
public class ToonsController {

    private final ToonsService toonsService;

    // 네컷뉴스 전체 조회
    @GetMapping
    public ResponseEntity<List<ToonResponse>> getToons() {
        return ResponseEntity.ok(toonsService.getToons());
    }

    // 좋아요
    @PostMapping("/{toonId}/like")
    public ResponseEntity<Void> likeToon(@PathVariable Long toonId,
                                         @AuthenticationPrincipal Long userId) {
        toonsService.likeToon(userId, toonId);
        return ResponseEntity.ok().build();
    }

    // 싫어요
    @PostMapping("/{toonId}/hate")
    public ResponseEntity<Void> hateToon(@PathVariable Long toonId,
                                         @AuthenticationPrincipal Long userId) {
        toonsService.hateToon(userId, toonId);
        return ResponseEntity.ok().build();
    }

    // 좋아요/싫어요 취소
    @PatchMapping("/{toonId}/like")
    public ResponseEntity<Void> cancelLike(@PathVariable Long toonId,
                                           @AuthenticationPrincipal Long userId) {
        toonsService.cancelLike(userId, toonId);
        return ResponseEntity.noContent().build();
    }

    // 내가 좋아요한 네컷뉴스 조회
    @GetMapping("/likes")
    public ResponseEntity<List<ToonResponse>> getUserLikedToons(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(toonsService.getUserLikedToons(userId));
    }
}
