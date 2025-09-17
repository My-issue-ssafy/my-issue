package com.ssafy.myissue.notification.dto.fcm;

import java.util.List;

public record SendSummary(
        int requested, // 요청한 토큰 수
        int success,
        int failure,
        List<String> invalidTokens // 실패한 토큰들
) {
    public static SendSummary empty() {
        return new SendSummary(0, 0, 0, List.of());
    }

    public static SendSummary of(int requested, int success, List<String> invalidTokens) {
        return new SendSummary(requested, success, requested - success, invalidTokens);
    }
}
