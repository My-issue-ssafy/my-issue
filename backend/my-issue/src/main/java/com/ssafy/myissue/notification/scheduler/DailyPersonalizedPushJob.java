package com.ssafy.myissue.notification.scheduler;

import com.ssafy.myissue.notification.dto.fcm.PersonalizedPush;
import com.ssafy.myissue.notification.service.impl.FcmPersonalizedSender;
import com.ssafy.myissue.user.domain.User;
import com.ssafy.myissue.user.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DailyPersonalizedPushJob {
    // 개인화 컨텐츠 조회

    private final FcmPersonalizedSender sender;
    private final UserRepository userRepository;

//    @Scheduled(cron = "0 30 8,20 * * *", /* 매일 오전 8시 30분 */ zone = "Asia/Seoul")
    @Scheduled(cron = "0 59 13 * * *", /* 매일 오전 8시 30분 */ zone = "Asia/Seoul")
    private void run() {
        // TODO: 개인화 푸시 알림 발송 작업
        // 대상자 조회
        // DTO 변환 및 GPT 통한 알림 내용 생성
        // 전송
        // 만료/실패 토큰 정리

        // 임시 작업
        List<User> users = userRepository.findByFcmTokenIsNotNull();
        if(users == null || users.isEmpty()) return;

        final String title = "개인화 푸시 알림 제목";
        final String body = "지금 들어가서 당신을 위한 맞춤 뉴스를 확인해보세요!";
        final Map<String, String> data = Map.of(
                "deeplink", "myissue://home",
                "type", "DAILY_BROADCAST"
        );

        List<PersonalizedPush> pushes = users.stream()
                .map(User::getFcmToken)
                .filter(t -> t != null && !t.isBlank())
                .map(t -> PersonalizedPush.of(t, title, body, data))
                .toList();

        if(pushes.isEmpty()) return;

        var result = sender.sendPersonalized(pushes);

        if (!result.invalidTokens().isEmpty()) {
            userRepository.clearFcmTokens(result.invalidTokens());
        }
    }

}
