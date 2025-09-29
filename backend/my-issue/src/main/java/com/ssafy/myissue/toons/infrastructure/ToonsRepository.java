package com.ssafy.myissue.toons.infrastructure;

import com.ssafy.myissue.toons.domain.Toons;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ToonsRepository extends JpaRepository<Toons, Long> {
    List<Toons> findByDate(LocalDate date);
}
