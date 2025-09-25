package com.ssafy.myissue.news.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.json.JsonData;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.myissue.news.dto.*;
import com.ssafy.myissue.news.domain.News;
import com.ssafy.myissue.news.infrastructure.NewsRepository;
import com.ssafy.myissue.common.exception.CustomException;      // [ADDED]
import com.ssafy.myissue.common.exception.ErrorCode;          // [ADDED]
import com.ssafy.myissue.news.infrastructure.NewsScrapRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NewsService {
    private static final String HOT_KEY= "hot:news";
    private static final String RECOMMEND_KEY_PREFIX = "recommend:news:";
    private static final String RECOMMEND_TS_PREFIX = "recommend:timestamp:";
    @Value("${app.recommend.url}")
    private String recommendUrl;
    @Value("${app.recommend.params}")
    private String recommendParams;
    private final RedisTemplate<String, Object> redisTemplate;
    private final NewsRepository newsRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final NewsScrapRepository scrapRepository;
    private final ElasticsearchClient elasticsearchClient;

    /** 메인 화면: HOT 5, 추천 5(임시 최신), 최신 5 */
    public NewsHomeResponse getHome(Long userId) {
        List<NewsCardResponse> hotCards = getMainHotNews();

        List<NewsCardResponse> recommendCards = getMainRecommendNews(userId);
        List<NewsCardResponse> latestCards = toCards(newsRepository.findLatestPage(null, null, 5));

        return new NewsHomeResponse(hotCards, recommendCards, latestCards);
    }

    private List<NewsCardResponse> getMainRecommendNews(Long userId) {
        String redisKey = RECOMMEND_KEY_PREFIX + userId;
        String tsKey = RECOMMEND_TS_PREFIX + userId;

        String cachedTs = (String) redisTemplate.opsForValue().get(tsKey);

        String url = recommendUrl + userId + recommendParams;
        RestTemplate restTemplate = new RestTemplate();
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);

        List<Map<String, Object>> recommendations = (List<Map<String, Object>>) response.get("recommendations");
        if (recommendations == null || recommendations.isEmpty()) {
            return List.of();
        }

        String apiTimestamp = (String) response.get("timestamp");

        // 3. Redis 갱신 여부 확인
        boolean needUpdate = (cachedTs == null) || apiTimestamp.compareTo(cachedTs) > 0;

        if (needUpdate) {
            redisTemplate.delete(redisKey);

            for (Map<String, Object> rec : recommendations) {
                Long newsId = Long.valueOf(rec.get("news_id").toString());
                Double score = Double.valueOf(rec.get("score").toString());
                redisTemplate.opsForList().rightPush(redisKey, newsId + ":" + score);
            }
            redisTemplate.opsForValue().set(tsKey, apiTimestamp);
        }

        // 4. Redis에서 상위 5개 꺼내서 DTO 변환

        List<Long> topIds = getIdsFromList(redisKey, 0, 4);
        List<News> newsList = newsRepository.findAllById(topIds);
        return toCards(newsList);
    }

    // HOT: Redis ZSET 기반 무한스크롤 (순서 보존)
    public CursorPage<NewsCardResponse> getHotByRedis(String cursor, int size) {
        final int pageSize = (size <= 0) ? 10 : size;

        // 1) 커서 해석 (offset)
        int offset = 0;
        if (cursor != null && !cursor.isBlank()) {
            HotZOffsetCursor c = CursorCodec.decode(cursor, HotZOffsetCursor.class);
            offset = Math.max(0, c.offset());
        }

        // 2) 총 개수
        ZSetOperations<String, Object> zset = redisTemplate.opsForZSet();
        Long totalL = zset.zCard(HOT_KEY);
        int total = (totalL == null) ? 0 : totalL.intValue();
        if (total == 0) return new CursorPage<>(List.of(), null, false);

        // 3) 범위 계산 (ZSET은 end inclusive)
        int start = offset;
        int endInclusive = Math.min(offset + pageSize, total) - 1;
        if (start > endInclusive) return new CursorPage<>(List.of(), null, false);

        // 4) Redis ZSET → ID들 (이미 reverseRange 순서 유지됨)
        List<Long> ids = getNewsIdListByRedis(HOT_KEY, start, endInclusive);
        if (ids.isEmpty()) return new CursorPage<>(List.of(), null, false);

        // 5) DB 조회 후 Redis 순서대로 재정렬
        List<News> found   = newsRepository.findAllById(ids);
        List<News> ordered = reorderByIds(found, ids);

        // 6) nextCursor 구성
        boolean hasNext = (endInclusive + 1) < total;
        String next = hasNext ? CursorCodec.encode(new HotZOffsetCursor(endInclusive + 1)) : null;

        return new CursorPage<>(toCards(ordered), next, hasNext);
    }

    // 추천: Redis LIST("id:score") 기반 무한스크롤 (10개씩)
    public CursorPage<NewsCardResponse> getRecommendByRedis(Long userId, String cursor, int size) {
        final int pageSize = (size <= 0) ? 10 : size;

        String listKey = RECOMMEND_KEY_PREFIX + userId; // LIST: "id:score"

        RecommendListOffsetCursor c = null;
        if (cursor != null && !cursor.isBlank()) {
            c = CursorCodec.decode(cursor, RecommendListOffsetCursor.class);
        }
        int start = (c == null) ? 0 : Math.max(0, c.offset());

        Long llenL = redisTemplate.opsForList().size(listKey);
        int llen = (llenL == null) ? 0 : llenL.intValue();
        if (llen == 0) return new CursorPage<>(List.of(), null, false);

        int endExclusive = Math.min(start + pageSize, llen);
        if (start >= endExclusive) return new CursorPage<>(List.of(), null, false);

        List<Long> ids = getIdsFromList(listKey, start, endExclusive - 1); // inclusive
        if (ids.isEmpty()) return new CursorPage<>(List.of(), null, false);

        List<News> found = newsRepository.findAllById(ids);
        List<News> ordered = reorderByIds(found, ids); // 리스트 순서 보존

        boolean hasNext = endExclusive < llen;
        String next = hasNext ? CursorCodec.encode(new RecommendListOffsetCursor(endExclusive)) : null;

        return new CursorPage<>(toCards(ordered), next, hasNext);
    }

    // Redis에서 받은 id 순서대로 DB 결과 재정렬 (반드시 사용!)
    private List<News> reorderByIds(List<News> found, List<Long> ids) {
        Map<Long, News> map = found.stream().collect(Collectors.toMap(News::getId, n -> n));
        List<News> ordered = new ArrayList<>(ids.size());
        for (Long id : ids) {
            News n = map.get(id);
            if (n != null) ordered.add(n);
        }
        return ordered;
    }


    private List<NewsCardResponse> getMainHotNews() {
        List<Long> ids = getNewsIdListByRedis(HOT_KEY, 0, 4); // TOP5
        if (ids.isEmpty()) return List.of();

        List<News> found   = newsRepository.findAllById(ids);
        List<News> ordered = reorderByIds(found, ids);        // ★ 순서 보존
        return toCards(ordered);
    }


    private List<Long> getIdsFromList(String key, int start, int end) {
        List<Object> items = redisTemplate.opsForList().range(key, start, end);
        if (items == null || items.isEmpty()) return List.of();
        List<Long> ids = new ArrayList<>(items.size());
        for (Object it : items) {
            String s = String.valueOf(it);
            int colon = s.indexOf(':');
            String idPart = (colon >= 0) ? s.substring(0, colon) : s;
            try { ids.add(Long.valueOf(idPart)); } catch (NumberFormatException ignore) {}
        }
        return ids;
    }

    private List<Long> getNewsIdListByRedis(String redisKey, int min, int max){
        ZSetOperations<String, Object> zset = redisTemplate.opsForZSet();

        // Redis에서 상위 100개 ID 가져오기
        Set<Object> newsIds = zset.reverseRange(redisKey, min, max);

        if (newsIds == null || newsIds.isEmpty()) {
            return List.of(); // HOT 뉴스 없으면 빈 리스트 반환
        }

        // Long 변환
        return newsIds.stream()
                .map(id -> Long.valueOf(id.toString()))
                .toList();
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
    public NewsDetailResponse getDetailAndIncreaseView(Long newsId, Long userId) {
        News n = newsRepository.findById(newsId)
                .orElseThrow(() -> new CustomException(ErrorCode.NEWS_NOT_FOUND)); // [CHANGED]
        n.increaseViews();

        var blocks = parseBlocks(n.getContent());

        boolean isScraped = scrapRepository.existsByNewsIdAndUserId(newsId, userId);

        return new NewsDetailResponse(
                n.getId(),
                n.getTitle(),
                blocks,
                n.getCategory(),
                n.getAuthor(),
                n.getNewsPaper(),
                n.getCreatedAt(),
                n.getViews(),
                n.getScrapCount(),
                isScraped
        );
    }

    /**
     * 뉴스 전체 조회(검색/카테고리) — LIKE 검색, cursor 기반 페이징
     */
    public CursorPage<NewsCardResponse> searchByLike(String keyword, String category, Integer size, String cursor) {
        int pageSize = (size == null || size <= 0) ? 20 : size;

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

        List<News> rows = newsRepository.searchPage(keyword, category, lastAt, lastId, pageSize + 1);
        return toCursorPageLatest(rows, pageSize);
    }

    /**
     * 뉴스 전체 조회 — 데이터베이스 인덱스 활용, cursor 기반 페이징
     */
    public CursorPage<NewsCardResponse> searchByIndex(String keyword, String category, Integer size, String cursor) {
        int pageSize = (size == null || size <= 0) ? 20 : size;

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

        List<News> rows;
        if (keyword != null && !keyword.isBlank()) {
            // 키워드가 있으면 searchPage (인덱스 활용하며 LIKE 검색)
            rows = newsRepository.searchPage(keyword, category, lastAt, lastId, pageSize + 1);
        } else if (category != null && !category.isBlank()) {
            // idx_news_category_cursor 인덱스 활용
            rows = newsRepository.findCategoryLatestPage(category, lastAt, lastId, pageSize + 1);
        } else {
            // idx_news_created_at 인덱스 활용
            rows = newsRepository.findLatestPage(lastAt, lastId, pageSize + 1);
        }

        return toCursorPageLatest(rows, pageSize);
    }

    public CursorPage<NewsCardResponse> search(String keyword, String category, Integer size, String cursor) {
        int pageSize = (size == null || size <= 0) ? 20 : size;

        // 키워드가 없으면 DB 인덱스 활용 (더 효율적)
        if (keyword == null || keyword.isBlank()) {
            return searchByIndex(keyword, category, size, cursor);
        }

        long start = System.currentTimeMillis();

        try {
            // 최적화된 쿼리 구조 (키워드가 있을 때만)
            BoolQuery optimizedQuery = BoolQuery.of(b -> {
                // title을 우선순위로, content는 가중치 낮게
                b.should(s -> s.match(m -> m
                        .field("title")
                        .query(keyword)
                        .boost(2.0f) // 제목 가중치 높게
                ));
                b.should(s -> s.match(m -> m
                        .field("content")
                        .query(keyword)
                        .boost(1.0f)
                ));
                b.minimumShouldMatch("1"); // 최소 하나는 매칭

                // category 조건 - filter context로 캐싱 활용
                if (category != null && !category.isBlank()) {
                    b.filter(f -> f.term(t -> t
                            .field("category")
                            .value(category)
                    ));
                }

                return b;
            });

            // 커서 파싱
            final SearchAfterCursor searchAfterCursor = (cursor != null && !cursor.isBlank()) 
                    ? CursorCodec.decode(cursor, SearchAfterCursor.class) 
                    : null;

            // ES 검색 실행 (search_after 기반 페이징)
            SearchResponse<Map> response = elasticsearchClient.search(s -> {
                var builder = s.index("news")
                        .query(q -> q.bool(optimizedQuery))
                        .size(pageSize + 1)
                        .source(src -> src
                                .filter(f -> f
                                        .includes("id", "title", "category", "createdAt", "thumbnail", "newsPaper", "author")
                                        .excludes("content", "embedding") // 큰 필드만 제외
                                )
                        );
                
                // 정렬: 관련성 스코어 > 생성일 > ID
                if (keyword != null && !keyword.isBlank()) {
                    // 키워드 검색시: 스코어 내림차순
                    builder = builder.sort(sort -> sort.score(sc -> sc.order(SortOrder.Desc)))
                                   .sort(sort -> sort.field(f -> f.field("createdAt").order(SortOrder.Desc)))
                                   .sort(sort -> sort.field(f -> f.field("id").order(SortOrder.Desc)));
                } else {
                    // 카테고리만 필터링시: 생성일 내림차순
                    builder = builder.sort(sort -> sort.field(f -> f.field("createdAt").order(SortOrder.Desc)))
                                   .sort(sort -> sort.field(f -> f.field("id").order(SortOrder.Desc)));
                }

                // search_after 적용
                if (searchAfterCursor != null) {
                    if (keyword != null && !keyword.isBlank()) {
                        builder = builder.searchAfter(searchAfterCursor.toSearchAfterValues());
                    } else {
                        // 키워드 없을 때는 스코어 제외하고 생성일, ID만 사용
                        builder = builder.searchAfter(searchAfterCursor.toSearchAfterValuesWithoutScore());
                    }
                }

                return builder;
            }, Map.class);

            List<Long> ids = response.hits().hits().stream()
                    .map(hit -> Long.valueOf(hit.id()))
                    .toList();

            boolean hasNext = ids.size() > pageSize;
            if (hasNext) {
                ids = ids.subList(0, pageSize);
            }

            long end = System.currentTimeMillis();
            log.info("소요시간 = {} ms", (end - start));
            // DB에서 최신 데이터 조회 후 ES 순서 보존
            List<News> newsList = newsRepository.findAllById(ids);
            Map<Long, News> map = newsList.stream().collect(Collectors.toMap(News::getId, n -> n));
            List<NewsCardResponse> items = ids.stream()
                    .map(map::get)
                    .filter(Objects::nonNull)
                    .map(NewsCardResponse::from)
                    .toList();

            // nextCursor 생성
            String next = null;
            if (hasNext && !response.hits().hits().isEmpty()) {
                var lastHit = response.hits().hits().get(Math.min(pageSize - 1, response.hits().hits().size() - 1));
                double score = keyword != null && !keyword.isBlank() ? lastHit.score() : 0.0;
                
                // createdAt을 초 단위로 변환 (ES에서 가져온 값 파싱)
                Long createdAtSec = null;
                Object createdAtObj = lastHit.source().get("createdAt");
                if (createdAtObj != null) {
                    // ISO 형식 문자열을 LocalDateTime으로 파싱 후 초 변환
                    try {
                        LocalDateTime createdAt = LocalDateTime.parse(createdAtObj.toString());
                        createdAtSec = createdAt.toEpochSecond(ZoneOffset.UTC);
                    } catch (Exception e) {
                        log.warn("Failed to parse createdAt: {}", createdAtObj.toString(), e);
                        createdAtSec = System.currentTimeMillis() / 1000; // fallback
                    }
                }
                
                Long newsId = Long.valueOf(lastHit.id());
                next = CursorCodec.encode(new SearchAfterCursor(score, createdAtSec, newsId));
            }

            return new CursorPage<>(items, next, hasNext);

        } catch (IOException e) {
            throw new CustomException(ErrorCode.ELASTICSEARCH_ERROR);
        }
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
}
