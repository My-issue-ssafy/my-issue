package com.ssafy.myissue.podcast.scheduler;

import com.ssafy.myissue.podcast.service.PodcastService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeneratePodcastJob {

    private final PodcastService podcastService;

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul") // 매일 새벽 2시 실행
    public void run() {
        try {
            log.debug("🎙️ Podcast 생성 Job 시작 ");

            podcastService.generateDailyPodcast();
            log.debug("Podcast 생성 Job 완료");
        } catch (Exception e) {
            log.error("Podcast 생성 Job 실패: {}", e.getMessage());
        }
    }
}
