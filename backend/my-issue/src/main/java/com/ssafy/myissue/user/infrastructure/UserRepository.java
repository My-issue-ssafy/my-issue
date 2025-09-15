package com.ssafy.myissue.user.infrastructure;

import com.ssafy.myissue.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByUuid(String uuid);
}
