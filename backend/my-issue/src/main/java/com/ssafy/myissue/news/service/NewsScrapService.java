package com.ssafy.myissue.news.service;

import com.ssafy.myissue.news.dto.CursorPage;
import com.ssafy.myissue.news.dto.NewsCardResponse;
import com.ssafy.myissue.news.dto.ScrapToggleResponse;
import com.ssafy.myissue.news.dto.CursorCodec;   // [ADDED]
import com.ssafy.myissue.news.dto.ScrapCursor;  // [ADDED]
import com.ssafy.myissue.news.domain.News;
import com.ssafy.myissue.news.domain.NewsScrap;
import com.ssafy.myissue.news.infrastructure.NewsRepository;
import com.ssafy.myissue.news.infrastructure.NewsScrapRepository;
import com.ssafy.myissue.common.exception.CustomException;
import com.ssafy.myissue.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class NewsScrapService {

    private static final int PAGE_SIZE = 20; // [ADDED] 규격 통일 (고정 페이지 크기)

    private final NewsRepository newsRepository;
    private final NewsScrapRepository scrapRepository;

    public NewsScrapService(NewsRepository newsRepository, NewsScrapRepository scrapRepository) {
        this.newsRepository = newsRepository;
        this.scrapRepository = scrapRepository;
    }

    /** 스크랩/해제 토글 */
    @Transactional
    public ScrapToggleResponse toggle(Long userId, long newsId) {
        Optional<NewsScrap> existing = newsRepository.findScrapByUserIdAndNewsId(userId, newsId);

        if (existing.isPresent()) {
            scrapRepository.delete(existing.get());
            News news = existing.get().getNews();
            news.decreaseScrapCount();
            newsRepository.save(news);
            return new ScrapToggleResponse(false, null);
        } else {
            News news = newsRepository.findById(newsId)
                    .orElseThrow(() -> new CustomException(ErrorCode.NEWS_NOT_FOUND));
            NewsScrap saved = scrapRepository.save(
                    NewsScrap.builder()
                            .userId(userId)
                            .news(news)
                            .build()
            );
            news.increaseScrapCount();
            newsRepository.save(news);
            return new ScrapToggleResponse(true, saved.getScrapId());
        }
    }

    /**
     * 내가 저장한 뉴스: GET /news/bookmarks?cursor={cursor}
     *  - cursor(String, Base64 JSON) ⇄ scrapId(Long)
     */
    public CursorPage<NewsCardResponse> list(Long userId, String cursor) { // [CHANGED] 시그니처 변경: size/lastId 제거, cursor 추가
        Long lastScrapId = decodeCursor(cursor);                            // [ADDED] 커서 디코드

        List<NewsScrap> rows = newsRepository.findScrapsWithNewsByUser(
                userId, lastScrapId, PAGE_SIZE + 1);                        // [CHANGED] PAGE_SIZE 고정 사용

        boolean hasNext = rows.size() > PAGE_SIZE;                          // [UNCHANGED-LOGIC] 초과분으로 다음 페이지 판정
        if (hasNext) rows = rows.subList(0, PAGE_SIZE);                     // [UNCHANGED-LOGIC]

        List<NewsCardResponse> items = rows.stream()
                .map(s -> {
                    News n = s.getNews();
                    return new NewsCardResponse(
                            n.getId(),
                            n.getTitle(),
                            /* author 필드가 DTO에 없다면 제거 */              // [NOTE]
                            n.getNewsPaper(),
                            n.getCreatedAt(),
                            n.getViews(),
                            n.getCategory(),
                            n.getThumbnail()
                    );
                })
                .toList();

        String nextCursor = null;                                           // [ADDED]
        if (hasNext && !rows.isEmpty()) {                                   // [ADDED]
            Long nextScrapId = rows.get(rows.size() - 1).getScrapId();      // [ADDED]
            nextCursor = CursorCodec.encode(new ScrapCursor(nextScrapId));  // [ADDED]
        }

        return new CursorPage<>(items, nextCursor, hasNext);
    }

    /** Base64 String cursor -> scrapId(Long) */
    private Long decodeCursor(String cursor) {                               // [ADDED]
        if (cursor == null || cursor.isBlank()) return null;
        try {
            return CursorCodec.decode(cursor, ScrapCursor.class).scrapId();
        } catch (IllegalArgumentException ex) {
            throw new CustomException(ErrorCode.INVALID_CURSOR);             // [ADDED] 프로젝트에 없으면 적절한 코드로 교체
        }
    }
}
