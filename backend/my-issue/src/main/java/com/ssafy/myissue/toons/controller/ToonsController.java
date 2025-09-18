package com.ssafy.myissue.toons.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ssafy.myissue.toons.dto.ToonResponse;
import com.ssafy.myissue.toons.service.ToonsService;
import com.ssafy.myissue.toons.service.ToonGeneratorService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.ssafy.myissue.common.exception.CustomException;
import com.ssafy.myissue.common.exception.ErrorCode;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/toons")
@Tag(name = "Toons", description = "네컷뉴스 API - 진현")
public class ToonsController {

    private final ToonsService toonsService;
    private final ToonGeneratorService toonGeneratorService;

    // 네컷뉴스 전체 조회
    @GetMapping
    public ResponseEntity<List<ToonResponse>> getToons() {
        return ResponseEntity.ok(toonsService.getToons());
    }

    // 좋아요
    @PostMapping("/{toonId}/like")
    public ResponseEntity<Void> likeToon(@PathVariable Long toonId,
                                         @AuthenticationPrincipal Long userId) {
        requireUser(userId);
        toonsService.likeToon(userId, toonId);
        return ResponseEntity.ok().build();
    }

    // 싫어요
    @PostMapping("/{toonId}/hate")
    public ResponseEntity<Void> hateToon(@PathVariable Long toonId,
                                         @AuthenticationPrincipal Long userId) {
        requireUser(userId);
        toonsService.hateToon(userId, toonId);
        return ResponseEntity.ok().build();
    }

    // 좋아요/싫어요 취소
    @PatchMapping("/{toonId}/like")
    public ResponseEntity<Void> cancelLike(@PathVariable Long toonId,
                                           @AuthenticationPrincipal Long userId) {
        requireUser(userId);
        toonsService.cancelLike(userId, toonId);
        return ResponseEntity.noContent().build();
    }

    // 내가 좋아요한 네컷뉴스 조회
    @GetMapping("/likes")
    public ResponseEntity<List<ToonResponse>> getUserLikedToons(@AuthenticationPrincipal Long userId) {
        requireUser(userId);
        return ResponseEntity.ok(toonsService.getUserLikedToons(userId));
    }
    private void requireUser(Long userId) {
        if (userId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_ACCESS);
        }
    }

    @PostMapping("/generate-daily")
    public ResponseEntity<Void> generateDailyToons() throws JsonProcessingException {
        toonGeneratorService.generateDailyToons();
        return ResponseEntity.ok().build();
    }
}
