package com.ssafy.myissue.notification.service;

import com.ssafy.myissue.notification.dto.NotificationsResponse;
import com.ssafy.myissue.notification.dto.SliceResponseDto;

public interface NotificationService {
    SliceResponseDto<NotificationsResponse> findAllByUserId(Long userId, Long lastId, int size);
    Boolean findUnreadNotification(Long userId);
    void deleteNotification(Long userId, Long notificationId);
    void deleteNotifications(Long userId);
    void updateNotificationStatus(Long userId);
    Boolean getNotificationStatus(Long userId);
    void updateNotificationReadStatus(Long userId, Long notificationId);
}
