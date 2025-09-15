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
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    public SliceResponseDto<NotificationsResponse> findAllByUserId(Long userId, Long lastId, int size) {
        Pageable pageable = PageRequest.of(0, size + 1); // size + 1 로 다음 페이지 존재 여부 확인
        List<Notification> notifications;

        if(userId == null) notifications = notificationRepository.findByUserIdOrderByIdDesc(userId, pageable);
        else notifications = notificationRepository.findByUserIdAndIdLessThanOrderByIdDesc(userId, lastId, pageable);

        boolean hasNext = notifications.size() > size;
        if (hasNext) notifications = notifications.subList(0, size); // 다음 페이지 존재 시 마지막 요소 제거

        // notificationId <- notification , newsId <- 연결된 news 의 id, content <- notification , iamge <- 연결된 news의 thumbnail, createdAt <- notifi, read <- notification
        List<NotificationsResponse> responseList = notifications.stream()
                .map(n -> NotificationsResponse.from(
                        n.getId(),
                        n.getNews().getNewsId(),
                        n.getContent(),
                        n.getNews().getThumbnail(),
                        n.getCreatedAt(),
                        n.isRead()
                ))
                .toList();


        return null;
    }
}
