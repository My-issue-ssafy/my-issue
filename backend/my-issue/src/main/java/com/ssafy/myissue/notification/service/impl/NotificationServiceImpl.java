package com.ssafy.myissue.notification.service.impl;

import com.ssafy.myissue.notification.domain.Notification;
import com.ssafy.myissue.notification.dto.NotificationsResponse;
import com.ssafy.myissue.notification.dto.SliceResponseDto;
import com.ssafy.myissue.notification.infrastructure.NotificationRepository;
import com.ssafy.myissue.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    public SliceResponseDto<NotificationsResponse> findAllByUserId(Long userId, Long lastId, int size) {
        Pageable pageable = PageRequest.of(
                0,
                size + 1, // size + 1 로 다음 페이지 존재 여부 확인
                Sort.by(Sort.Direction.DESC, "id") // DESC 정렬 추가
        );
        List<Notification> notifications;

        if(lastId == null) notifications = notificationRepository.findByUser_Id(userId, pageable);
        else notifications = notificationRepository.findByUser_IdAndIdLessThan(userId, lastId, pageable);

        boolean hasNext = notifications.size() > size;
        if (hasNext) notifications = notifications.subList(0, size); // 다음 페이지 존재 시 마지막 요소 제거
        log.debug("[findAllByUserId Service] notificationsList size: {}, hasNext: {}", notifications.size(), hasNext);

        List<NotificationsResponse> responseList = notifications.stream()
                .map(NotificationsResponse::from)
                .toList();

        return SliceResponseDto.of(responseList, hasNext);
    }

    @Override
    public Boolean findUnreadNotification(Long userId) {
        return notificationRepository.existsByUser_IdAndReadIsFalse(userId);
    }

    @Override
    public Void deleteNotification(Long userId, Long notificationId) {

        return null;
    }

    @Override
    public Void deleteNotifications(Long userId) {
        return null;
    }
}
