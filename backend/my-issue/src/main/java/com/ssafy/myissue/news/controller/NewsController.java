package com.ssafy.myissue.news.controller;

import com.ssafy.myissue.news.dto.CursorPage;
import com.ssafy.myissue.news.dto.NewsCardResponse;
import com.ssafy.myissue.news.dto.NewsDetailResponse;
import com.ssafy.myissue.news.dto.NewsHomeResponse;
import com.ssafy.myissue.news.dto.ScrapToggleResponse;
import com.ssafy.myissue.news.domain.NewsCategory;
import com.ssafy.myissue.news.service.NewsScrapService;
import com.ssafy.myissue.news.service.NewsService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/news")
public class NewsController {

    private final NewsService newsService;
    private final NewsScrapService scrapService;

    public NewsController(NewsService newsService, NewsScrapService scrapService) {
        this.newsService = newsService;
        this.scrapService = scrapService;
    }

    /** 홈: HOT 5, 추천 5, 최신 5 */
    @GetMapping("/main")
    public NewsHomeResponse getHome(
            @RequestHeader(value = "X-USER-ID", required = false) Long userId
    ) {
        return newsService.getHome(userId);
    }

    /** HOT 전체 (무한 스크롤) */
    @GetMapping("/hot")
    public CursorPage<NewsCardResponse> getHot(
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "size", required = false, defaultValue = "20") Integer size
    ) {
        return newsService.getHot(cursor, safeSize(size, 20, 50));
    }

    /** 최신 전체 (무한 스크롤) */
    @GetMapping("/trend")
    public CursorPage<NewsCardResponse> getLatest(
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "size", required = false, defaultValue = "20") Integer size
    ) {
        return newsService.getLatest(cursor, safeSize(size, 20, 50));
    }

    /** 추천 전체 (무한 스크롤) — 추천 엔진 전까지 최신과 동일 동작 */
    @GetMapping("/recommend")
    public CursorPage<NewsCardResponse> getRecommend(
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "size", required = false, defaultValue = "20") Integer size
    ) {
        return newsService.getLatest(cursor, safeSize(size, 20, 50));
    }

    /** 뉴스 상세 */
    @GetMapping("/{newsId}")
    public NewsDetailResponse getDetail(@PathVariable("newsId") long newsId) {
        return newsService.getDetailAndIncreaseView(newsId);
    }

    /**
     * 뉴스 전체 조회(검색/카테고리)
     * /news?keyword=&category=&size=&lastId=
     *  - 커서 대신 lastId(경계 뉴스 ID) 기반 페이징
     */
    @GetMapping
    public CursorPage<NewsCardResponse> search(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "category", required = false) NewsCategory category,
            @RequestParam(value = "size", required = false, defaultValue = "20") Integer size,
            @RequestParam(value = "lastId", required = false) Long lastId
    ) {
        return newsService.search(keyword, category, safeSize(size, 20, 50), lastId);
    }

    /** 스크랩/해제 토글 */
    @PostMapping("/{newsId}/bookmark")
    @ResponseStatus(HttpStatus.OK)
    public ScrapToggleResponse toggleBookmark(
            @PathVariable("newsId") long newsId,
            @RequestHeader("X-USER-ID") Long userId // 임시: 헤더에서 user_id 직접 수신
    ) {
        if (userId == null) throw new IllegalArgumentException("X-USER-ID header required");
        return scrapService.toggle(userId, newsId);
    }

    /** 내가 저장한 뉴스 (lastId = scrapId) */
    @GetMapping("/bookmarks")
    public CursorPage<NewsCardResponse> myBookmarks(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestParam(value = "size", required = false, defaultValue = "20") Integer size,
            @RequestParam(value = "lastId", required = false) Long lastId
    ) {
        if (userId == null) throw new IllegalArgumentException("X-USER-ID header required");
        return scrapService.list(userId, safeSize(size, 20, 50), lastId);
    }

    // ---------- helpers ----------

    /** size 하한/상한 고정 */
    private int safeSize(Integer raw, int def, int max) {
        if (raw == null || raw <= 0) return def;
        return Math.min(raw, max);
    }

    /** 간단한 400 응답 (커서/파라미터 오류 등) */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleBadRequest(IllegalArgumentException e) {
        return Map.of("error", e.getMessage());
    }
}
