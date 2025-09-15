package com.ssafy.myissue.notification.controller;

import com.ssafy.myissue.notification.dto.NotificationsResponse;
import com.ssafy.myissue.notification.dto.SliceResponseDto;
import com.ssafy.myissue.notification.service.NotificationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/notification")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<SliceResponseDto<NotificationsResponse>> findAllByUserId(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Long lastId,
            @RequestParam(defaultValue = "20") int size
            ) {
        log.debug("[findAllByUserId] userId: {}, lastId: {}, size: {})", userId, lastId, size);
        return ResponseEntity.ok(notificationService.findAllByUserId(userId, lastId, size));
    }

}
