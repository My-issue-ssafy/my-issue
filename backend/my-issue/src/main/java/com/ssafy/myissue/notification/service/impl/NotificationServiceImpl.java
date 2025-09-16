package com.ssafy.myissue.notification.service.impl;

import com.ssafy.myissue.common.exception.CustomException;
import com.ssafy.myissue.common.exception.ErrorCode;
import com.ssafy.myissue.notification.domain.Notification;
import com.ssafy.myissue.notification.dto.NotificationsResponse;
import com.ssafy.myissue.notification.dto.SliceResponseDto;
import com.ssafy.myissue.notification.infrastructure.NotificationRepository;
import com.ssafy.myissue.notification.service.NotificationService;
import com.ssafy.myissue.user.domain.User;
import com.ssafy.myissue.user.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

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
    @Transactional
    public void deleteNotification(Long userId, Long notificationId) {
        // 오류처리 : 알림이 존재하는지 / 해당 유저의 알림인지
        if(!notificationRepository.existsById(notificationId))  {
            log.error("[deleteNotification Service] Notification not found. notificationId: {}", notificationId);
            throw new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
        if(!notificationRepository.existsByUser_IdAndId(userId, notificationId)) {
            log.error("[deleteNotification Service] Unauthorized access to notification. userId: {}, notificationId: {}", userId, notificationId);
            throw new CustomException(ErrorCode.UNAUTHORIZED_NOTIFICATION);
        }

        notificationRepository.deleteByUser_IdAndId(userId, notificationId);
    }

    @Override
    @Transactional
    public void deleteNotifications(Long userId) {
        notificationRepository.deleteAllByUser_Id(userId);
    }

    @Override
    @Transactional
    public void updateNotificationStatus(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> {
                log.error("[updateNotificationStatus Service] User not found. userId: {}", userId);
                return new CustomException(ErrorCode.USER_NOT_FOUND);
            });

        user.updateNotificationEnabled();
    }

}
