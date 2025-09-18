package com.ssafy.myissue.podcast.controller;

import com.ssafy.myissue.podcast.service.PodcastService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/podcast")
@Tag(name = "Podcast", description = "팟캐스트(Podcast) API - 시은")
public class PodcastController {
    private final PodcastService podcastService;

    @PostMapping("/generate")
    public ResponseEntity<String> generateDailyPodcast() {
        try {
            podcastService.generateDailyPodcast();
            return ResponseEntity.ok("✅ 팟캐스트 생성 완료");
        } catch (Exception e) {
            log.error("팟캐스트 생성 실패", e);
            return ResponseEntity.internalServerError().body("❌ 팟캐스트 생성 실패: " + e.getMessage());
        }
    }
}
