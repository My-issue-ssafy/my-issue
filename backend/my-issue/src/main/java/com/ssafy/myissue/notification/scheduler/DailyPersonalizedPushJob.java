package com.ssafy.myissue.notification.scheduler;

import com.ssafy.myissue.news.domain.News;
import com.ssafy.myissue.news.infrastructure.NewsRepository;
import com.ssafy.myissue.notification.domain.Notification;
import com.ssafy.myissue.notification.dto.fcm.PersonalizedPush;
import com.ssafy.myissue.notification.infrastructure.NotificationRepository;
import com.ssafy.myissue.notification.service.impl.FcmPersonalizedSender;
import com.ssafy.myissue.notification.service.impl.RecommendationService;
import com.ssafy.myissue.user.domain.User;
import com.ssafy.myissue.user.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyPersonalizedPushJob {

    private final FcmPersonalizedSender sender;
    private final UserRepository userRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final RecommendationService recommendationService;

    private final NewsRepository newsRepository;
    private final NotificationRepository notificationRepository;

    @Scheduled(cron = "0 30 8,20 * * *", /* 매일 오전 8시 30분 */ zone = "Asia/Seoul")
    public void run() {
        log.debug("Running DailyPersonalizedPushJob");

        // 대상자 조회 : FcmToken이 null이 아니고 알림 설정이 true일 경우
        List<User> users = userRepository.findByFcmTokenIsNotNullAndNotificationEnabledTrue();
        if(users == null || users.isEmpty()) return;

        // 유저 별 추천 기사 1개 조회
        List<PersonalizedPush> pushes = users.stream()
            .map(user -> {
                // 유저별 추천 기사 조회
                List<String> recommendedNewsIds = stringRedisTemplate.opsForList()
                        .range("recommend:news:" + user.getId(), 0, -1);

                // 기사목록
                List<Long> candidateNewsIds;

                // 추천 기사 없으면 Python API 호출
                if (recommendedNewsIds == null || recommendedNewsIds.isEmpty()) {
                    log.debug("No recommended news in Redis for userId={}, calling Python API...", user.getId());

                    candidateNewsIds = recommendationService.getRecommendations(
                        user.getId(),
                        50,
                        50,
                        "balanced"
                    );

                    if (candidateNewsIds.isEmpty()) {
                        log.debug("No recommendations from Python API for userId={}", user.getId());
                        return null;
                    }
                } else {
                    // Redis 결과 사용, ":" 기준으로 잘라서 앞부분만 사용
                    candidateNewsIds = recommendedNewsIds.stream()
                        .map(raw -> raw.replace("\"", "").trim().split(":")[0])
                        .map(Long::valueOf)
                        .toList();
                }

                // 유저가 이미 알림 받은 뉴스는 제외
                Long newsId = candidateNewsIds.stream()
                    .filter(id -> !notificationRepository.existsByUser_IdAndNews_Id(user.getId(), id))
                    .findFirst()
                    .orElse(null);

                if (newsId == null) {
                    log.debug("All candidate news already notified to userId={}", user.getId());
                    return null;
                }

                var newsOpt = newsRepository.findById(newsId);
                if (newsOpt.isEmpty()) {
                    log.debug("News not found for newsId={}", newsId);
                    return null;
                }
                News news = newsOpt.get();

                // 알림 제목/본문 생성
                String newsTitle = newsOpt.get().getTitle();
                final String title = "당신을 위한 맞춤 뉴스가 도착했습니다!" ;
                final String body = newsTitle;

                log.debug("Prepared push for userId={}, title={}", user.getId(), newsTitle);
                return PersonalizedPush.of(user, news, title, body);
            })
            .filter(push -> push != null) // null 제외
            .toList();

        if (pushes.isEmpty()) return;

        // 한 번에 전송
        var result = sender.sendPersonalized(pushes);
        log.debug("Sent {} personalized push notifications", pushes.size());

        List<String> invalidTokens = result.invalidTokens();

        // 성공한 push만 DB 저장
        pushes.stream()
            .filter(push -> !invalidTokens.contains(push.token()))
            .forEach(push -> {
                User user = userRepository.findById(push.userId()).orElse(null);
                News news = newsRepository.findById(push.newsId()).orElse(null);

                if (user == null || news == null) {
                    log.warn("Skip saving notification: userId={}, newsId={}", push.userId(), push.newsId());
                    return;
                }

                Notification notification = Notification.of(user, news, push.body());
                notificationRepository.save(notification);
            });

        // 만료/실패 토큰 정리
        if (!invalidTokens.isEmpty()) {
            log.debug("Invalid tokens detected: {}", invalidTokens);
            userRepository.clearFcmTokens(invalidTokens);
        }
    }
}
