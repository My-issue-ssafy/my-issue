package com.ssafy.myissue.notification.service.impl;

import com.google.firebase.messaging.*;
import com.ssafy.myissue.notification.dto.fcm.PersonalizedPush;
import com.ssafy.myissue.notification.dto.fcm.SendSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmPersonalizedSender {
    private static final int MAX_BATCH = 500; // 500 is the maximum allowed by FCM
    private final FirebaseMessaging firebaseMessaging;

    public SendSummary sendPersonalized(List<PersonalizedPush> pushes) {
        if(pushes == null || pushes.isEmpty()) {
            return SendSummary.empty();
        }

        // 초기 설정 (요청 수, 성공 수, 실패 토큰 리스트)
        int requested = pushes.size();
        int success = 0;
        List<String> invalidTokens = new ArrayList<>();

        for(int i = 0; i < pushes.size(); i += MAX_BATCH) {
            var slice = pushes.subList(i, Math.min(i + MAX_BATCH, pushes.size()));
            var messages = slice.stream()
                    .map(this::toMessage)
                    .toList();
            try {
                BatchResponse resp = firebaseMessaging.sendEach(messages); // 동기 호출
                success += resp.getSuccessCount();

                // 실패 토큰 정리
                var response = resp.getResponses();
                for(int idx = 0; idx < response.size(); idx++) {
                    var r = response.get(idx);
                    if(!r.isSuccessful()) {
                        var ex = r.getException();
                        if (ex instanceof FirebaseMessagingException fme) {
                            var code = fme.getMessagingErrorCode(); // UNREGISTERED, INVALID_ARGUMENT 등
                            if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
                                // 만료/로그아웃 등 DB에서 토큰 비활성화 대상으로 수거
                                invalidTokens.add(slice.get(idx).token());
                            }
                            log.warn("[FCM] fail token = {} code = {} msg = {}", slice.get(idx).token(), code, fme.getMessage());
                        } else {
                            log.warn("[FCM] fail token = {} msg = {}", slice.get(idx).token(), (ex != null ? ex.getMessage() : "unknown"));
                        }
                    }
                }
            } catch (Exception e) {
                // 배치 전체 전송 실패 (네트워크 문제 등)
                log.error("[FCM] sendAll error: {}", e.getMessage(), e);
            }
        }

        return SendSummary.of(requested, success, invalidTokens);
    }

    private Message toMessage(PersonalizedPush push) {
        // 화면에 표시될 알림(title, body)
        Notification notification = Notification.builder()
                .setTitle(push.title())
                .setBody(push.body())
                .build();

        // 기본 Message 빌더 생성 (토큰 + 알림)
        Message.Builder builder = Message.builder()
                .setToken(push.token())
                .setNotification(notification)
                .putData("newsId", String.valueOf(push.newsId()));

        return builder.build();
    }
}
