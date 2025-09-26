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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

        // 대상자 조회 : FcmToken != null & 알림 설정 = true
        List<User> users = userRepository.findByFcmTokenIsNotNullAndNotificationEnabledTrue();
        if(users == null || users.isEmpty()) return;

        List<PersonalizedPush> pushes = users.stream()
            .map(this::selectNewsIdForUser)
            .filter(newsId -> newsId != null)
            .map(newsIdUserPair -> createPush(newsIdUserPair.user(), newsIdUserPair.newsId()))
            .filter(push -> push != null)
            .toList();

        if (pushes.isEmpty()) return;

        var result = sender.sendPersonalized(pushes);
        log.debug("Sent {} personalized push notifications", pushes.size());

        saveNotifications(pushes, result.invalidTokens());
    }

    // 후보 뉴스 선택 (Redis -> Python API -> 인기 뉴스 fallback)
    private NewsIdUserPair selectNewsIdForUser(User user) {
        List<Long> candidateNewsIds = getCandidateNewsIdsFromRedis(user.getId());

        if (candidateNewsIds.isEmpty()) {
            candidateNewsIds = recommendationService.getRecommendations(user.getId(), 50, 50, "balanced");
            log.debug("Fallback to Python API for userId={}", user.getId());
        }

        if (candidateNewsIds.isEmpty()) {
            candidateNewsIds = getHotNewsFallback();
            log.debug("Fallback to Hot News for userId={}", user.getId());
        }

        Long newsId = candidateNewsIds.stream()
            .filter(id -> !notificationRepository.existsByUser_IdAndNews_Id(user.getId(), id))
            .findFirst()
            .orElse(null);

        return newsId != null ? new NewsIdUserPair(user, newsId) : null;
    }

    // Redis에서 후보 뉴스 ID 목록 조회
    private List<Long> getCandidateNewsIdsFromRedis(Long userId) {
        List<String> recommendedNewsIds = stringRedisTemplate.opsForList()
            .range("recommend:news:" + userId, 0, -1);

        if (recommendedNewsIds == null || recommendedNewsIds.isEmpty()) return List.of();

        return recommendedNewsIds.stream()
            .map(raw -> raw.replace("\"", "").trim().split(":")[0])
            .map(Long::valueOf)
            .toList();
    }

    // 인기 뉴스 fallback
    private List<Long> getHotNewsFallback() {
        return newsRepository.findTop10ByDate(LocalDate.now().atStartOfDay(), LocalDateTime.now(), PageRequest.of(0, 10)).stream()
            .map(News::getId)
            .toList();
    }

    // 개인화 알림 생성
    private PersonalizedPush createPush(User user, Long newsId) {
        return newsRepository.findById(newsId)
            .map(news -> {
                final String title = "당신을 위한 맞춤 뉴스가 도착했습니다!";
                final String body = news.getTitle();
                log.debug("Prepared push for userId={}, title={}", user.getId(), news.getTitle());
                return PersonalizedPush.of(user, news, title, body);
            })
            .orElse(null);
    }

    // 성공한 알림만 DB 저장 & invalid token 정리
    private void saveNotifications(List<PersonalizedPush> pushes, List<String> invalidTokens) {
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

        if (!invalidTokens.isEmpty()) {
            log.debug("Invalid tokens detected: {}", invalidTokens);
            userRepository.clearFcmTokens(invalidTokens);
        }
    }

    // 유저ID + 뉴스ID 매핑
    private record NewsIdUserPair(User user, Long newsId) {}
}
