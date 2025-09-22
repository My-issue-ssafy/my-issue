package com.ssafy.myissue.notification.dto.fcm;

import com.ssafy.myissue.news.domain.News;
import com.ssafy.myissue.user.domain.User;

public record PersonalizedPush(
        String token,
        String title,
        String body,
        Long userId,
        Long newsId
) {
    public static PersonalizedPush of(User user, News news, String title, String body) {
        return new PersonalizedPush(user.getFcmToken(), title, body, user.getId(), news.getId());
    }
}
