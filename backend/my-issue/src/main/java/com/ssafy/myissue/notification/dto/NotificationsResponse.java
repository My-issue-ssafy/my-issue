package com.ssafy.myissue.notification.dto;

import com.ssafy.myissue.notification.domain.Notification;

import java.time.LocalDateTime;

public record NotificationsResponse(Long notificationId, Long newsId, String content, String thumbnail, LocalDateTime createdAt, boolean read) {
    public static NotificationsResponse from(Notification notification){
        return new NotificationsResponse(
                notification.getId(),
                notification.getNews().getNewsId(),
                notification.getContent(),
                notification.getNews().getThumbnail(),
                notification.getCreatedAt(),
                notification.isRead()
        );
    }
}
