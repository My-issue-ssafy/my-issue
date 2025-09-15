package com.ssafy.myissue.toons.infrastructure;

import com.ssafy.myissue.toons.domain.Toons;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ToonsRepository extends JpaRepository<Toons, Long> {
}
