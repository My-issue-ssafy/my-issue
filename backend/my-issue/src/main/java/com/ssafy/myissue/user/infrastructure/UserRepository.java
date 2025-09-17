package com.ssafy.myissue.user.infrastructure;

import com.ssafy.myissue.user.domain.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByUuid(String uuid);
    List<User> findByFcmTokenIsNotNull();

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update User u set u.fcmToken = null where u.fcmToken in :tokens")
    int clearFcmTokens(@Param("tokens") List<String> tokens);
}
