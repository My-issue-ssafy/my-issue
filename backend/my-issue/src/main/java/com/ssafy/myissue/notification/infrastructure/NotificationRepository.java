package com.ssafy.myissue.notification.infrastructure;

import com.ssafy.myissue.notification.domain.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);
    List<Notification> findByUserIdAndIdLessThanOrderByIdDesc(Long userId, Long lastId, Pageable pageable);
}