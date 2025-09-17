package com.ssafy.myissue.notification.dto.fcm;

public record PersonalizedPush(
        String token,
        String title,
        String body
) {
    public static PersonalizedPush of(String token, String title, String body) {
        return new PersonalizedPush(token, title, body);
    }

}
