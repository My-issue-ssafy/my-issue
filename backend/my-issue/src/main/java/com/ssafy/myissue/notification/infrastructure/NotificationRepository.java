package com.ssafy.myissue.notification.infrastructure;

import com.ssafy.myissue.notification.domain.Notification;
import com.ssafy.myissue.user.domain.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUser_Id(Long userId, Pageable pageable);
    List<Notification> findByUser_IdAndIdLessThan(Long userId, Long lastId, Pageable pageable);
    boolean existsByUser_IdAndReadIsFalse(Long userId);
    void deleteByUser_IdAndId(Long userId, Long notificationId);
    void deleteAllByUser_Id(Long userId);
    boolean existsByUser_IdAndNews_Id(Long userId, Long newsId);

    // Custom existence check methods
    boolean existsById(Long id);
    boolean existsByUser_IdAndId(Long userId, Long notificationId);

    Long user(User user);
}