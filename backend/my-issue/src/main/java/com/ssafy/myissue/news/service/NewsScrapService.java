package com.ssafy.myissue.news.service;

import com.ssafy.myissue.news.dto.CursorPage;
import com.ssafy.myissue.news.dto.NewsCardResponse;
import com.ssafy.myissue.news.dto.ScrapToggleResponse;
import com.ssafy.myissue.news.domain.News;
import com.ssafy.myissue.news.domain.NewsScrap;
import com.ssafy.myissue.news.infrastructure.NewsRepository;
import com.ssafy.myissue.news.infrastructure.NewsScrapRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class NewsScrapService {

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
            return new ScrapToggleResponse(false, null);
        } else {
            News news = newsRepository.findById(newsId)
                    .orElseThrow(() -> new IllegalArgumentException("news not found: " + newsId));
            NewsScrap saved = scrapRepository.save(
                    NewsScrap.builder()
                            .userId(userId)
                            .news(news)
                            .build()
            );
            return new ScrapToggleResponse(true, saved.getScrapId());
        }
    }

    /** 내가 저장한 뉴스: /news/bookmarks?lastId&size  (lastId = scrapId) */
    public CursorPage<NewsCardResponse> list(Long userId, Integer size, Long lastId) {
        int pageSize = (size == null || size <= 0) ? 20 : size;

        List<NewsScrap> rows = newsRepository.findScrapsWithNewsByUser(userId, lastId, pageSize + 1);

        boolean hasNext = rows.size() > pageSize;
        if (hasNext) {
            rows = rows.subList(0, pageSize);
        }

        List<News> newsList = rows.stream()
                .map(NewsScrap::getNews)
                .toList();

//        Map<Long, List<String>> images = batchImages(newsList);

        List<NewsCardResponse> items = newsList.stream()
                .map(n -> new NewsCardResponse(
                        n.getNewsId(),
                        n.getTitle(),
                        n.getAuthor(),     // ← 3번째는 author
                        n.getNewspaper(),
                        n.getCreatedAt(),
                        n.getViews(),
                        n.getCategory(),   // ← 7번째가 category
                        n.getThumbnail()
                ))
                .toList();

        // lastId 방식이므로 nextCursor는 null
        return new CursorPage<>(items, null, hasNext);
    }

    /** 여러 뉴스의 이미지를 배치 로딩 → newsId -> [image...] (N+1 방지) */
//    private Map<Long, List<String>> batchImages(List<News> rows) {
//        if (rows == null || rows.isEmpty()) {
//            return Map.of();
//        }
//
//        List<Long> ids = rows.stream()
//                .map(News::getNewsId)
//                .toList();
//
//        return newsRepository.findImagesByNewsIds(ids).stream()
//                .collect(Collectors.groupingBy(
//                        img -> img.getNews().getNewsId(),
//                        Collectors.mapping(i -> i.getImage(), Collectors.toList())
//                ));
//    }
//
//    /** 본문에서 앞 n자 미리보기: 태그 제거, 공백 정리, 코드포인트 안전 절단 */
//    private String preview(String content, int maxChars) {
//        if (content == null || content.isBlank()) return "";
//
//        String noTags = content.replaceAll("<[^>]+>", " ");
//        String text = noTags.replaceAll("\\s+", " ").trim();
//        if (text.isEmpty()) return "";
//
//        int cpCount = text.codePointCount(0, text.length());
//        if (cpCount <= maxChars) return text;
//
//        int endIndex = text.offsetByCodePoints(0, maxChars);
//        return text.substring(0, endIndex) + "…";
//    }
}
