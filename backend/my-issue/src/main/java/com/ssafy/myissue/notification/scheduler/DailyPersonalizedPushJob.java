package com.ssafy.myissue.notification.scheduler;

import com.ssafy.myissue.news.infrastructure.NewsRepository;
import com.ssafy.myissue.notification.dto.fcm.PersonalizedPush;
import com.ssafy.myissue.notification.service.impl.FcmPersonalizedSender;
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
    // 개인화 컨텐츠 조회

    private final FcmPersonalizedSender sender;
    private final UserRepository userRepository;
    private final StringRedisTemplate stringRedisTemplate;

    private final NewsRepository newsRepository;

    //    @Scheduled(cron = "0 30 8,20 * * *", /* 매일 오전 8시 30분 */ zone = "Asia/Seoul")
    @Scheduled(cron = "0 37 17 * * *", /* 매일 오전 8시 30분 */ zone = "Asia/Seoul")
    private void run() {
        // TODO: 개인화 푸시 알림 발송 작업
        // 대상자 조회
        // 유저 별 추천 기사 1개 조회
        // 오류 처리 ( 추천 기사 없을 경우, 파이썬에 요청 / 요청 후에도 응답이 없을 경우 해당 유저 제외 )
        // DTO 변환 및 GPT 통한 알림 내용 생성
        // 전송
        // 만료/실패 토큰 정리

        // 임시 작업
        log.debug("Running DailyPersonalizedPushJob");
        List<User> users = userRepository.findByFcmTokenIsNotNull();
        if(users == null || users.isEmpty()) return;

        // 가장 인기있는 뉴스 1개 조회 (예시)
        String topNewsId = stringRedisTemplate
                .opsForZSet()
                .reverseRange("hot:news", 0, 0)
                .stream().findFirst().orElse(null);

        // 뉴스 ID로 뉴스 정보 조회 후, 개인화된 제목/내용 생성 (생략)
        var newsOpt = newsRepository.findById(Long.valueOf(topNewsId));
        if (newsOpt.isEmpty()) return;
        String newsTitle = newsOpt.get().getTitle();

        final String title = "개인화 푸시 알림 제목: " + newsTitle  ;
        final String body = "지금 들어가서 당신을 위한 맞춤 뉴스를 확인해보세요!";

        log.debug("title: {}, body: {}", title, body);
        List<PersonalizedPush> pushes = users.stream()
                .map(User::getFcmToken)
                .filter(t -> t != null && !t.isBlank())
                .map(t -> PersonalizedPush.of(t, title, body))
                .toList();

        if(pushes.isEmpty()) return;

        var result = sender.sendPersonalized(pushes);
        log.debug("Sent {} personalized push notifications", result);

        if (!result.invalidTokens().isEmpty()) {
            log.debug("Sent {} personalized push notifications", result.invalidTokens().size());
            userRepository.clearFcmTokens(result.invalidTokens());
        }
    }
}
