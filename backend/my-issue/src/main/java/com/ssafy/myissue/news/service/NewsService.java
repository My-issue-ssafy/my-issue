package com.ssafy.myissue.news.service;

import com.fasterxml.jackson.core.type.TypeReference;               // [NEW]
import com.fasterxml.jackson.databind.ObjectMapper;                // [NEW]
import com.ssafy.myissue.news.dto.CursorCodec;
import com.ssafy.myissue.news.dto.CursorPage;
import com.ssafy.myissue.news.dto.HotCursor;
import com.ssafy.myissue.news.dto.LatestCursor;
import com.ssafy.myissue.news.dto.NewsCardResponse;
import com.ssafy.myissue.news.dto.NewsDetailResponse;
import com.ssafy.myissue.news.dto.NewsHomeResponse;
import com.ssafy.myissue.news.dto.ContentBlock;                    // [NEW]
import com.ssafy.myissue.news.domain.News;
// import com.ssafy.myissue.news.entity.NewsImage;                 // [REMOVED]
import com.ssafy.myissue.news.infrastructure.NewsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;                                       // [NEW]
import java.util.List;
// import java.util.Map;                                           // [REMOVED]
// import java.util.stream.Collectors;                              // [REMOVED]

@Service
@Transactional(readOnly = true)
public class NewsService {

    private final NewsRepository newsRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();  // [NEW] content(jsonb) 파싱용

    public NewsService(NewsRepository newsRepository) {
        this.newsRepository = newsRepository;
    }

    /** 메인 화면: HOT 5, 추천 5(임시 최신), 최신 5 */
    public NewsHomeResponse getHome(Long userId) {
        List<News> hotRows = newsRepository.findHotPage(null, null, null, 5);
        List<NewsCardResponse> hotCards = toCards(hotRows);

        List<News> recommendRows = newsRepository.findLatestPage(null, null, 5);
        List<NewsCardResponse> recommendCards = toCards(recommendRows);

        List<News> latestRows = newsRepository.findLatestPage(null, null, 5);
        List<NewsCardResponse> latestCards = toCards(latestRows);

        return new NewsHomeResponse(hotCards, recommendCards, latestCards);
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
                .orElseThrow(() -> new IllegalArgumentException("news not found: " + newsId));
        n.increaseViews(); // 더티 체킹으로 UPDATE

        // [CHANGED] 이미지 테이블 대신 content(jsonb)를 파싱해서 내려줌
        var blocks = parseBlocks(n.getContent());

        return new NewsDetailResponse(
                n.getNewsId(),
                n.getTitle(),
                blocks,
                n.getCategory(),
                n.getAuthor(),
                n.getNewsPaper(),
                n.getCreatedAt(),
                n.getViews()
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
                    .orElseThrow(() -> new IllegalArgumentException("lastId not found: " + lastId));
            lastAt = base.getCreatedAt();
            lastNewsId = base.getNewsId();
        }

        List<News> rows = newsRepository.searchPage(keyword, category, lastAt, lastNewsId, pageSize + 1);

        boolean hasNext = rows.size() > pageSize;
        if (hasNext) {
            rows = rows.subList(0, pageSize);
        }

        List<NewsCardResponse> items = toCards(rows);
        return new CursorPage<>(items, null, hasNext); // lastId 방식이므로 nextCursor 없음
    }

    // ================= 내부 공통 =================

    private CursorPage<NewsCardResponse> toCursorPageLatest(List<News> rows, int size) {
        boolean hasNext = rows.size() > size;
        if (hasNext) {
            rows = rows.subList(0, size);
        }

        List<NewsCardResponse> items = toCards(rows);

        String next = null;
        if (hasNext && !rows.isEmpty()) {
            News last = rows.get(rows.size() - 1);
            long sec = last.getCreatedAt().toEpochSecond(ZoneOffset.UTC);
            next = CursorCodec.encode(new LatestCursor(sec, last.getNewsId()));
        }
        return new CursorPage<>(items, next, hasNext);
    }

    private CursorPage<NewsCardResponse> toCursorPageHot(List<News> rows, int size) {
        boolean hasNext = rows.size() > size;
        if (hasNext) {
            rows = rows.subList(0, size);
        }

        List<NewsCardResponse> items = toCards(rows);

        String next = null;
        if (hasNext && !rows.isEmpty()) {
            News last = rows.get(rows.size() - 1);
            long sec = last.getCreatedAt().toEpochSecond(ZoneOffset.UTC);
            next = CursorCodec.encode(new HotCursor(last.getViews(), sec, last.getNewsId()));
        }
        return new CursorPage<>(items, next, hasNext);
    }

    /** 엔티티 목록 → 카드 DTO 목록 (썸네일 1장만 사용) */
    private List<NewsCardResponse> toCards(List<News> newsList) {
        return newsList.stream().map(n -> new NewsCardResponse(
                n.getNewsId(),
                n.getTitle(),
                n.getAuthor(),
                n.getNewsPaper(),
                n.getCreatedAt(),
                n.getViews(),
                n.getCategory(),
                n.getThumbnail()// [CHANGED] 카드 이미지는 thumbnail 필드만
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

    // [REMOVED] 이미지 N+1 방지 배치 로딩/preview 유틸 전부 삭제
}
