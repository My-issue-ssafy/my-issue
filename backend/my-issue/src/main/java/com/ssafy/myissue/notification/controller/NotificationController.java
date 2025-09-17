package com.ssafy.myissue.notification.controller;

import com.ssafy.myissue.notification.dto.NotificationsResponse;
import com.ssafy.myissue.notification.dto.SliceResponseDto;
import com.ssafy.myissue.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/notification")
@Tag(name = "Notification", description = "알림(Notification) API - 시은")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(
            summary = "내 알림 전체 조회",
            description = """
                    ### - 최신 알림 순으로 size 개수만큼 조회
                    ### - lastId가 없으면 가장 최신 알림부터 size 개수만큼 조회
                    ### - lastId가 있으면 lastId보다 작은 알림부터 size 개수만큼 조회
                    """
    )
    public ResponseEntity<SliceResponseDto<NotificationsResponse>> findAllByUserId(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Long lastId,
            @RequestParam(defaultValue = "20") int size
            ) {
        log.debug("[findAllByUserId] userId: {}, lastId: {}, size: {})", userId, lastId, size);
        return ResponseEntity.ok(notificationService.findAllByUserId(userId, lastId, size));
    }

    @GetMapping("/unread")
    @Operation(
            summary = "내 알림 중 읽지 않은 알림 존재 여부 조회",
            description = "### - 읽지 않은 알림이 하나라도 있으면 true, 없으면 false 반환"
    )
    public ResponseEntity<Boolean> findUnreadNotification(@AuthenticationPrincipal Long userId) {
        log.debug("[findUnreadNotification] userId: {}", userId);
        return ResponseEntity.ok(notificationService.findUnreadNotification(userId));
    }

    @DeleteMapping("/{notificationId}")
    @Operation(
            summary = "내 알림 개별 삭제",
            description = "### - notificationId에 해당하는 알림을 삭제"
    )
    public ResponseEntity<Void> deleteNotification(@AuthenticationPrincipal Long userId, @PathVariable Long notificationId) {
        log.debug("[deleteNotification] userId: {}, notificationId: {}", userId, notificationId);
        notificationService.deleteNotification(userId, notificationId);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @Operation(
            summary = "내 알림 전체 삭제",
            description = "### - 내 모든 알림을 삭제"
    )
    public ResponseEntity<Void> deleteNotifications(@AuthenticationPrincipal Long userId) {
        log.debug("[deleteNotifications] userId: {}", userId);

        notificationService.deleteNotifications(userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/status")
    @Operation(
            summary = "내 알림 설정 상태 변경",
            description = "### - 알림 설정이 활성화 되어있으면 비활성화, 비활성화 되어있으면 활성화로 변경"
    )
    public ResponseEntity<Void> updateNotificationStatus(@AuthenticationPrincipal Long userId) {
        log.debug("[updateNotificationStatus] userId: {}", userId);

        notificationService.updateNotificationStatus(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/status")
    @Operation(
            summary = "내 알림 설정 상태 조회",
            description = "### - 알림 설정이 활성화 되어있으면 true, 비활성화 되어있으면 false 반환"
    )
    public ResponseEntity<Boolean> getNotificationStatus(@AuthenticationPrincipal Long userId) {
        log.debug("[getNotificationStatus] userId: {}", userId);

        return ResponseEntity.ok(notificationService.getNotificationStatus(userId));
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(
            summary = "내 알림 읽음 처리",
            description = "### - notificationId에 해당하는 알림을 읽음 처리"
    )
    public ResponseEntity<Void> markAsRead(@AuthenticationPrincipal Long userId, @PathVariable Long notificationId) {
        log.debug("[markAsRead] userId: {}, notificationId: {}", userId, notificationId);

        notificationService.updateNotificationReadStatus(userId, notificationId);
        return ResponseEntity.noContent().build();
    }

}
