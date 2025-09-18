package com.ssafy.myissue.news.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.myissue.news.dto.CursorCodec;
import com.ssafy.myissue.news.dto.CursorPage;
import com.ssafy.myissue.news.dto.HotCursor;
import com.ssafy.myissue.news.dto.LatestCursor;
import com.ssafy.myissue.news.dto.NewsCardResponse;
import com.ssafy.myissue.news.dto.NewsDetailResponse;
import com.ssafy.myissue.news.dto.NewsHomeResponse;
import com.ssafy.myissue.news.dto.ContentBlock;
import com.ssafy.myissue.news.domain.News;
import com.ssafy.myissue.news.infrastructure.NewsRepository;
import com.ssafy.myissue.common.exception.CustomException;      // [ADDED]
import com.ssafy.myissue.common.exception.ErrorCode;          // [ADDED]
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NewsService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String HOT_KEY= "hot:news";
    private final NewsRepository newsRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();


    /** 메인 화면: HOT 5, 추천 5(임시 최신), 최신 5 */
    public NewsHomeResponse getHome(Long userId) {
        List<NewsCardResponse> hotCards = getMainHotNews();

        List<News> recommendRows = newsRepository.findLatestPage(null, null, 5);
        List<NewsCardResponse> recommendCards = toCards(recommendRows);

        List<News> latestRows = newsRepository.findLatestPage(null, null, 5);
        List<NewsCardResponse> latestCards = toCards(latestRows);

        return new NewsHomeResponse(hotCards, recommendCards, latestCards);
    }

    private List<NewsCardResponse> getMainHotNews() {
        List<Long> ids = getNewsIdListByRedis(0,4);

        // DB 조회
        List<News> newsList = newsRepository.findAllById(ids);

        // Map으로 변환 (id → News 매핑)
        Map<Long, News> newsMap = newsList.stream()
                .collect(Collectors.toMap(News::getId, n -> n));

        // Redis 순서 보존 + DTO 변환
        return toCards(newsList);
    }

    /** 최신 전체(무한 스크롤, cursor 기반) */
    public CursorPage<NewsCardResponse> getLatest(String cursor, int size) {
        LatestCursor c = null;
        if (cursor != null && !cursor.isBlank()) {
            c = CursorCodec.decode(cursor, LatestCursor.class);
        }

        LocalDateTime lastAt = null;
        Long lastId = null;
        if (c != null) {
            lastAt = LocalDateTime.ofEpochSecond(c.createdAtSec(), 0, ZoneOffset.UTC);
            lastId = c.newsId();
        }

        List<News> rows = newsRepository.findLatestPage(lastAt, lastId, size + 1);
        return toCursorPageLatest(rows, size);
    }

    /** HOT 전체(무한 스크롤, cursor 기반) */
    public CursorPage<NewsCardResponse> getHot(String cursor, int size) {
        HotCursor c = null;
        if (cursor != null && !cursor.isBlank()) {
            c = CursorCodec.decode(cursor, HotCursor.class);
        }

        Integer lastViews = null;
        LocalDateTime lastAt = null;
        Long lastId = null;
        if (c != null) {
            lastViews = c.views();
            lastAt = LocalDateTime.ofEpochSecond(c.createdAtSec(), 0, ZoneOffset.UTC);
            lastId = c.newsId();
        }

        List<News> rows = newsRepository.findHotPage(lastViews, lastAt, lastId, size + 1);
        return toCursorPageHot(rows, size);
    }

    /** 카테고리 최신(무한 스크롤, cursor 기반) */
    public CursorPage<NewsCardResponse> getByCategory(String category, String cursor, int size) {
        LatestCursor c = null;
        if (cursor != null && !cursor.isBlank()) {
            c = CursorCodec.decode(cursor, LatestCursor.class);
        }

        LocalDateTime lastAt = null;
        Long lastId = null;
        if (c != null) {
            lastAt = LocalDateTime.ofEpochSecond(c.createdAtSec(), 0, ZoneOffset.UTC);
            lastId = c.newsId();
        }

        List<News> rows = newsRepository.findCategoryLatestPage(category, lastAt, lastId, size + 1);
        return toCursorPageLatest(rows, size);
    }

    /** 상세 + 조회수 증가 (이미지 테이블 대신 content(JSON) 파싱) */
    @Transactional
    public NewsDetailResponse getDetailAndIncreaseView(long newsId) {
        News n = newsRepository.findById(newsId)
                .orElseThrow(() -> new CustomException(ErrorCode.NEWS_NOT_FOUND)); // [CHANGED]
        n.increaseViews();

        var blocks = parseBlocks(n.getContent());

        return new NewsDetailResponse(
                n.getId(),
                n.getTitle(),
                blocks,
                n.getCategory(),
                n.getAuthor(),
                n.getNewsPaper(),
                n.getCreatedAt(),
                n.getViews(),
                n.getScrapCount()
        );
    }

    /**
     * 뉴스 전체 조회(검색/카테고리) — /news?keyword&category&size&lastId
     *  - 커서는 쓰지 않고 lastId 기준으로 페이징
     */
    public CursorPage<NewsCardResponse> search(String keyword, String category, Integer size, Long lastId) {
        int pageSize = (size == null || size <= 0) ? 20 : size;

        LocalDateTime lastAt = null;
        Long lastNewsId = null;
        if (lastId != null) {
            News base = newsRepository.findById(lastId)
                    .orElseThrow(() -> new CustomException(ErrorCode.INVALID_PARAMETER)); // [CHANGED]
            lastAt = base.getCreatedAt();
            lastNewsId = base.getId();
        }

        List<News> rows = newsRepository.searchPage(keyword, category, lastAt, lastNewsId, pageSize + 1);

        boolean hasNext = rows.size() > pageSize;
        if (hasNext) {
            rows = rows.subList(0, pageSize);
        }

        List<NewsCardResponse> items = toCards(rows);
        return new CursorPage<>(items, null, hasNext);
    }

    // ================= 내부 공통 =================

    private CursorPage<NewsCardResponse> toCursorPageLatest(List<News> rows, int size) {
        boolean hasNext = rows.size() > size;
        if (hasNext) rows = rows.subList(0, size);

        List<NewsCardResponse> items = toCards(rows);

        String next = null;
        if (hasNext && !rows.isEmpty()) {
            News last = rows.get(rows.size() - 1);
            long sec = last.getCreatedAt().toEpochSecond(ZoneOffset.UTC);
            next = CursorCodec.encode(new LatestCursor(sec, last.getId()));
        }
        return new CursorPage<>(items, next, hasNext);
    }

    private CursorPage<NewsCardResponse> toCursorPageHot(List<News> rows, int size) {
        boolean hasNext = rows.size() > size;
        if (hasNext) rows = rows.subList(0, size);

        List<NewsCardResponse> items = toCards(rows);

        String next = null;
        if (hasNext && !rows.isEmpty()) {
            News last = rows.get(rows.size() - 1);
            long sec = last.getCreatedAt().toEpochSecond(ZoneOffset.UTC);
            next = CursorCodec.encode(new HotCursor(last.getViews(), sec, last.getId()));
        }
        return new CursorPage<>(items, next, hasNext);
    }

    /** 엔티티 목록 → 카드 DTO 목록 (썸네일 1장만 사용) */
    private List<NewsCardResponse> toCards(List<News> newsList) {
        return newsList.stream().map(n -> new NewsCardResponse(
                n.getId(),
                n.getTitle(),
                n.getNewsPaper(),
                n.getCreatedAt(),
                n.getViews(),
                n.getCategory(),
                n.getThumbnail()
        )).toList();
    }

    // content(JSON) → List<ContentBlock>
    private List<ContentBlock> parseBlocks(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<ContentBlock>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<Long> getNewsIdListByRedis(int min, int max){
        ZSetOperations<String, Object> zset = redisTemplate.opsForZSet();

        // Redis에서 상위 100개 ID 가져오기
        Set<Object> newsIds = zset.reverseRange(HOT_KEY, min, max);

        if (newsIds == null || newsIds.isEmpty()) {
            return List.of(); // HOT 뉴스 없으면 빈 리스트 반환
        }

        // Long 변환
        return newsIds.stream()
                .map(id -> Long.valueOf(id.toString()))
                .toList();
    }
    public List<NewsDetailResponse> getHotRecommendTop100() {
        List<Long> ids = getNewsIdListByRedis(0,99);

        List<News> newsList = newsRepository.findAllById(ids);
        // Map으로 변환 (id → News 매핑)
        Map<Long, News> newsMap = newsList.stream()
                .collect(Collectors.toMap(News::getId, n -> n));

        // Redis 순서 보존 + DTO 변환
        return ids.stream()
                .map(newsMap::get)
                .filter(Objects::nonNull)
                .map(n -> NewsDetailResponse.from(n, parseBlocks(n.getContent()))) // 엔티티 → DTO 변환
                .toList();
    }
}
