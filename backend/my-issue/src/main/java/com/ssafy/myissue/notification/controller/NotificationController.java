package com.ssafy.myissue.notification.controller;

import com.ssafy.myissue.notification.dto.NotificationsResponse;
import com.ssafy.myissue.notification.dto.SliceResponseDto;
import com.ssafy.myissue.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
            summary = "내 알림 전체 조회 API",
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
            summary = "내 알림 중 읽지 않은 알림 존재 여부 조회 API",
            description = "### - 읽지 않은 알림이 하나라도 있으면 true, 없으면 false 반환"
    )
    public ResponseEntity<Boolean> findUnreadNotification(@AuthenticationPrincipal Long userId) {
        log.debug("[findUnreadNotification] userId: {}", userId);
        return ResponseEntity.ok(notificationService.findUnreadNotification(userId));
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(@AuthenticationPrincipal Long userId, @PathVariable Long notificationId) {
        log.debug("[deleteNotification] userId: {}, notificationId: {}", userId, notificationId);
        notificationService.deleteNotification(userId, notificationId);

        return ResponseEntity.noContent().build();
    }

}
