package com.ssafy.myissue.toons.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ssafy.myissue.toons.service.ToonGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component // [ADDED]
@RequiredArgsConstructor // [ADDED]
public class ToonsScheduler { // [ADDED]

    private final ToonGeneratorService toonGeneratorService; // [ADDED]
    private final AtomicBoolean running = new AtomicBoolean(false); // [ADDED]

    /**
     * 매일 00:30 (KST) 실행
     * cron 포맷: 초 분 시 일 월 요일
     */
    @Scheduled(cron = "0 00 23 * * *", zone = "Asia/Seoul") // [ADDED]
    public void generateDailyToonsAt0030KST() {
        if (!running.compareAndSet(false, true)) {
            log.warn("[ToonsScheduler] A previous run is still running. Skip this tick."); // [ADDED]
            return;
        }
        long started = System.currentTimeMillis(); // [ADDED]
        try {
            log.info("[ToonsScheduler] Start daily toon generation (23:00 KST)"); // [ADDED]
            toonGeneratorService.generateDailyToons(); // [ADDED]
            log.info("[ToonsScheduler] Done in {} ms", System.currentTimeMillis() - started); // [ADDED]
        } catch (JsonProcessingException e) {
            log.error("[ToonsScheduler] JSON processing error", e); // [ADDED]
        } catch (Exception e) {
            log.error("[ToonsScheduler] Unexpected error", e); // [ADDED]
        } finally {
            running.set(false); // [ADDED]
        }
    }
}
