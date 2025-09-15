package com.ssafy.myissue.notification.service;

import com.ssafy.myissue.notification.dto.NotificationsResponse;
import com.ssafy.myissue.notification.dto.SliceResponseDto;

public interface NotificationService {
    SliceResponseDto<NotificationsResponse> findAllByUserId(Long userId, Long lastId, int size);
}
