package com.ssafy.myissue.toons.controller;

import com.ssafy.myissue.toons.dto.ToonResponse;
import com.ssafy.myissue.toons.service.ToonsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/toons")
public class ToonsController {

    private final ToonsService toonsService;

    // 네컷뉴스 전체 조회
    @GetMapping
    public List<ToonResponse> getToons() {
        return toonsService.getToons();
    }

    // 좋아요
    @PostMapping("/{toonId}/like")
    public void likeToon(@RequestHeader("X-USER-ID") Long userId,
                         @PathVariable Long toonId) {
        toonsService.likeToon(userId, toonId);
    }

    // 싫어요
    @PostMapping("/{toonId}/hate")
    public void hateToon(@RequestHeader("X-USER-ID") Long userId,
                         @PathVariable Long toonId) {
        toonsService.hateToon(userId, toonId);
    }

    // 좋아요/싫어요 취소
    @PatchMapping("/{toonId}/like")
    public void cancelLike(@RequestHeader("X-USER-ID") Long userId,
                           @PathVariable Long toonId) {
        toonsService.cancelLike(userId, toonId);
    }

    // 내가 좋아요한 네컷뉴스 조회
    @GetMapping("/likes")
    public List<ToonResponse> getUserLikedToons(@RequestHeader("X-USER-ID") Long userId) {
        return toonsService.getUserLikedToons(userId);
    }
}
