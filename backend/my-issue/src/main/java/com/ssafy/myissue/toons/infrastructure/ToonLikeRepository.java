package com.ssafy.myissue.toons.infrastructure;

import com.ssafy.myissue.toons.domain.ToonLike;
import com.ssafy.myissue.toons.domain.Toons;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ToonLikeRepository extends JpaRepository<ToonLike, Long> {

    Optional<ToonLike> findByUserIdAndToon(Long userId, Toons toon);
    List<ToonLike> findByUserIdAndLikedTrue(Long userId);
}
