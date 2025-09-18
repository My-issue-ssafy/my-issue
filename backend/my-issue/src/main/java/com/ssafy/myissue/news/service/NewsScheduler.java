package com.ssafy.myissue.news.service;

import com.ssafy.myissue.news.dto.HotNewsCandidates;
import com.ssafy.myissue.news.infrastructure.NewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsScheduler {

    private final NewsRepository newsRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String HOT_KEY = "hot:news";

    @Scheduled(fixedRate = 1800000) // 30분마다 실행
    public void startScheduler() {
        log.info("뉴스 스케줄러 시작 - HOT 뉴스 업데이트");
        updateHotNews();
    }

    public void manualScheduler() {
        log.info("뉴스 스케줄러 실행 (수동)");
        updateHotNews();
    }

    private void updateHotNews() {
        LocalDateTime since = LocalDateTime.now().minusDays(7);

        // 후보군 조회
        List<HotNewsCandidates> candidates = newsRepository.findHotCandidates(since, 50, 0);

        // 점수 계산 후 정렬
        List<NewsScore> scored = candidates.stream()
                .map(n -> new NewsScore(n.getId(), calculateScore(n)))
                .sorted(Comparator.comparingDouble(NewsScore::score).reversed())
                .limit(100)
                .toList();

        // Redis Sorted Set에 ID만 저장
        redisTemplate.delete(HOT_KEY);
        ZSetOperations<String, Object> zset = redisTemplate.opsForZSet();

        for (NewsScore ns : scored) {
            log.info("HOT 뉴스 후보 - ID: {}, 점수: {}", ns.newsId(), ns.score());
            zset.add(HOT_KEY, ns.newsId(), ns.score());
        }
        log.info("HOT 뉴스 TOP 100 업데이트 완료");
    }

    private double calculateScore(HotNewsCandidates news) {
        long hoursSince = Duration.between(news.getCreatedAt(), LocalDateTime.now()).toHours();

        // 가중치: 조회수(1점), 스크랩(5점)
        int points = news.getViews() + news.getScrapCount() * 5;

        return (points - 1) / Math.pow(hoursSince + 2, 1.8);
    }

    record NewsScore(Long newsId, double score) {}
}
