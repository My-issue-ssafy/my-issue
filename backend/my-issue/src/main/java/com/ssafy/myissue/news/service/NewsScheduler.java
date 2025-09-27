package com.ssafy.myissue.news.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ssafy.myissue.news.dto.HotNewsCandidates;
import com.ssafy.myissue.news.dto.NewsCardResponse;
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
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
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

        for(int i = 0 ; i < candidates.size(); i++) {
            HotNewsCandidates n = candidates.get(i);
            log.info("{}. {} (조회수: {}, 스크랩: {}, 작성일: {}, 신문사: {})", i + 1, n.getTitle(), n.getViews(), n.getScrapCount(), n.getCreatedAt(), n.getNewsPaper());
        }
        // 점수 계산 후 정렬
        List<HotNewsCandidates> sorted = candidates.stream()
                .sorted(Comparator.comparingDouble(this::calculateScore).reversed())
                .limit(100)
                .toList();

        // Redis Sorted Set에 ID만 저장
        redisTemplate.delete(HOT_KEY);
        ZSetOperations<String, Object> zSet = redisTemplate.opsForZSet();

        for (HotNewsCandidates n : sorted) {
            try {
                NewsCardResponse card = NewsCardResponse.toCard(n);
                String json = objectMapper.writeValueAsString(card);

                double score = calculateScore(n);
                zSet.add(HOT_KEY, json, score);

            } catch (JsonProcessingException e) {
                log.error("NewsCardResponse 직렬화 실패: {}", n.getId(), e);
            }
        }

        log.info("HOT 뉴스 TOP 100 업데이트 완료");
    }

    private double calculateScore(HotNewsCandidates news) {
        long hoursSince = Duration.between(news.getCreatedAt(), LocalDateTime.now()).toHours();

        // 가중치: 조회수(1점), 스크랩(5점)
        int points = news.getViews() + news.getScrapCount() * 5;

        return (points - 1) / Math.pow(hoursSince + 2, 1.8);
    }

    record NewsScore(NewsCardResponse card, double score) {}
}
