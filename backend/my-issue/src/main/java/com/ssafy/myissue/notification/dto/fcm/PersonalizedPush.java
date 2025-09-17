package com.ssafy.myissue.notification.dto.fcm;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;

import java.util.Map;

public record PersonalizedPush(
        String token,
        String title,
        String body,
        Map<String, String> data
) {
    public static PersonalizedPush of(String token, String title, String body, Map<String, String> data) {
        return new PersonalizedPush(token, title, body, data);
    }

}
