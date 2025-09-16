package com.ssafy.myissue.toons.service;

import com.ssafy.myissue.news.infrastructure.NewsRepository;
import com.ssafy.myissue.toons.domain.ToonLike;
import com.ssafy.myissue.toons.domain.Toons;
import com.ssafy.myissue.toons.dto.ToonResponse;
import com.ssafy.myissue.toons.infrastructure.ToonLikeRepository;
import com.ssafy.myissue.toons.infrastructure.ToonsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.ssafy.myissue.common.exception.CustomException;
import com.ssafy.myissue.common.exception.ErrorCode;

@Service
@RequiredArgsConstructor
@Transactional
public class ToonsService {

    private final ToonsRepository toonsRepository;
    private final ToonLikeRepository toonLikeRepository;

    // 네컷뉴스 전체 조회
    public List<ToonResponse> getToons() {
        return toonsRepository.findAll()
                .stream()
                .map(ToonResponse::from)
                .collect(Collectors.toList());
    }

    // 좋아요
    public void likeToon(Long userId, Long toonId) {
        Toons toon = toonsRepository.findById(toonId)
                .orElseThrow(() -> new CustomException(ErrorCode.TOON_NOT_FOUND));

        Optional<ToonLike> likeOpt = toonLikeRepository.findByUserIdAndToon(userId, toon);

        if (likeOpt.isPresent()) {
            ToonLike like = likeOpt.get();
            like.setLiked(true); // 좋아요로 변경
        } else {
            ToonLike like = ToonLike.builder()
                    .userId(userId)
                    .toon(toon)
                    .liked(true)
                    .build();
            toonLikeRepository.save(like);
        }
    }

    // 싫어요
    public void hateToon(Long userId, Long toonId) {
        Toons toon = toonsRepository.findById(toonId)
                .orElseThrow(() -> new CustomException(ErrorCode.TOON_NOT_FOUND));;

        Optional<ToonLike> likeOpt = toonLikeRepository.findByUserIdAndToon(userId, toon);

        if (likeOpt.isPresent()) {
            ToonLike like = likeOpt.get();
            like.setLiked(false); // 싫어요로 변경
        } else {
            ToonLike like = ToonLike.builder()
                    .userId(userId)
                    .toon(toon)
                    .liked(false)
                    .build();
            toonLikeRepository.save(like);
        }
    }

    // 좋아요/싫어요 취소
    public void cancelLike(Long userId, Long toonId) {
        Toons toon = toonsRepository.findById(toonId)
                .orElseThrow(() -> new CustomException(ErrorCode.TOON_NOT_FOUND));

        ToonLike like = toonLikeRepository.findByUserIdAndToon(userId, toon)
                .orElseThrow(() -> new CustomException(ErrorCode.TOON_LIKE_NOT_FOUND));

        toonLikeRepository.delete(like); // DB에서 삭제 처리
    }

    // 내가 좋아요한 네컷뉴스 조회
    public List<ToonResponse> getUserLikedToons(Long userId) {
        return toonLikeRepository.findByUserIdAndLikedTrue(userId)
                .stream()
                .map(like -> ToonResponse.from(like.getToon()))
                .collect(Collectors.toList());
    }
}
