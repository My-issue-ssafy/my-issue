package com.ssafy.myissue.news.controller;

import com.ssafy.myissue.news.dto.*;
import com.ssafy.myissue.news.service.NewsScheduler;
import com.ssafy.myissue.news.service.NewsScrapService;
import com.ssafy.myissue.news.service.NewsService;
import com.ssafy.myissue.common.exception.CustomException;
import com.ssafy.myissue.common.exception.ErrorCode;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.ssafy.myissue.news.service.NewsChatService;


import java.util.List;

@RestController
@RequestMapping("/news")
@RequiredArgsConstructor
@Tag(name = "News", description = "뉴스 API - 진현")
public class NewsController {

    private final NewsService newsService;
    private final NewsScrapService scrapService;
    private final NewsScheduler newsScheduler;
    private final NewsChatService newsChatService;

    /** 홈: HOT 5, 추천 5, 최신 5 */
    @GetMapping("/main")
    public ResponseEntity<NewsHomeResponse> getHome(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(newsService.getHome(userId));
    }

    /** HOT 전체 (무한 스크롤) */
    @GetMapping("/hot")
    public ResponseEntity<CursorPage<NewsCardResponse>> getHot(@RequestParam(value = "cursor", required = false) String cursor, @RequestParam(value = "size", required = false, defaultValue = "20") Integer size
    ) {
        return ResponseEntity.ok(newsService.getHotByRedis(cursor, safeSize(size, 20, 50)));
    }

    /** 최신 전체 (무한 스크롤) */
    @GetMapping("/trend")
    public ResponseEntity<CursorPage<NewsCardResponse>> getLatest(@RequestParam(value = "cursor", required = false) String cursor, @RequestParam(value = "size", required = false, defaultValue = "20") Integer size) {
        return ResponseEntity.ok(newsService.getLatest(cursor, safeSize(size, 20, 50)));
    }

    /** 추천 전체 (무한 스크롤) — 추천 엔진 전까지 최신과 동일 동작 */
    @GetMapping("/recommend")
    public ResponseEntity<CursorPage<NewsCardResponse>> getRecommend(@AuthenticationPrincipal Long userId, @RequestParam(value = "cursor", required = false) String cursor, @RequestParam(value = "size", required = false, defaultValue = "20") Integer size) {
        return ResponseEntity.ok(newsService.getRecommendByRedis(userId, cursor, safeSize(size, 20, 50)));
    }

    /** 뉴스 상세 */
    @GetMapping("/{newsId}")
    public ResponseEntity<NewsDetailResponse> getDetail(@PathVariable("newsId") long newsId, @AuthenticationPrincipal Long userId) { // [CHANGED]
        return ResponseEntity.ok(newsService.getDetailAndIncreaseView(newsId, userId));
    }

    /**
     * 뉴스 전체 조회(검색/카테고리)
     * /news?keyword=&category=&size=&lastId=
     *  - 커서 대신 lastId(경계 뉴스 ID) 기반 페이징
     */
    @GetMapping
    public ResponseEntity<CursorPage<NewsCardResponse>> search(@RequestParam(value = "keyword", required = false) String keyword, @RequestParam(value = "category", required = false) String category,
                                                               @RequestParam(value = "size", required = false, defaultValue = "20") Integer size, @RequestParam(value = "lastId", required = false) Long lastId) {
        return ResponseEntity.ok(newsService.search(keyword, category, safeSize(size, 20, 50), lastId));
    }

    /** 스크랩/해제 토글 */
    @PostMapping("/{newsId}/bookmark")
    public ResponseEntity<ScrapToggleResponse> toggleBookmark(@PathVariable("newsId") long newsId, @AuthenticationPrincipal Long userId) {
        if (userId == null) throw new CustomException(ErrorCode.UNAUTHORIZED_ACCESS);
        return ResponseEntity.ok(scrapService.toggle(userId, newsId));
    }

    /** 내가 저장한 뉴스 (lastId = scrapId) */
    @GetMapping("/bookmarks")
    public ResponseEntity<CursorPage<NewsCardResponse>> myBookmarks(@AuthenticationPrincipal Long userId, @RequestParam(value = "cursor", required = false) String cursor) {
        if (userId == null) throw new CustomException(ErrorCode.UNAUTHORIZED_ACCESS);
        return ResponseEntity.ok(scrapService.list(userId, cursor));
    }

    @PostMapping("/hot/update")
    public ResponseEntity<Void> updateHotNews() {
        newsScheduler.manualScheduler();
        return ResponseEntity.ok().build();
    }

    // 인기도 기반 추천 모델 확인 TEST
    @GetMapping("/hot/recommend/top100")
    public ResponseEntity<List<NewsDetailResponse>> getHotRecommendTop100() {
        return ResponseEntity.ok(newsService.getHotRecommendTop100());
    }

    @PostMapping("/{newsId}/chat")                                   // [ADDED]
    public ResponseEntity<NewsChatResponse> chatAboutNews(           // [CHANGED]
                                                                     @PathVariable Long newsId,                               // [ADDED]
                                                                     @RequestBody ChatQuestionRequest req) {                  // [ADDED]
        if (req == null || req.question() == null || req.question().isBlank()) { // [ADDED]
            throw new CustomException(ErrorCode.INVALID_PARAMETER);              // [ADDED]
        }
        return ResponseEntity.ok(                                    // [CHANGED]
                newsChatService.answerAboutNews(newsId, req.question())
        );
    }
    // ---------- helpers ----------

    /** size 하한/상한 고정 */
    private int safeSize(Integer raw, int def, int max) {
        if (raw == null || raw <= 0) return def;
        return Math.min(raw, max);
    }
}
