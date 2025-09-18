package com.ssafy.myissue.news.service;

import com.ssafy.myissue.news.dto.CursorPage;
import com.ssafy.myissue.news.dto.NewsCardResponse;
import com.ssafy.myissue.news.dto.ScrapToggleResponse;
import com.ssafy.myissue.news.domain.News;
import com.ssafy.myissue.news.domain.NewsScrap;
import com.ssafy.myissue.news.infrastructure.NewsRepository;
import com.ssafy.myissue.news.infrastructure.NewsScrapRepository;
import com.ssafy.myissue.common.exception.CustomException;   // [ADDED]
import com.ssafy.myissue.common.exception.ErrorCode;       // [ADDED]
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

    /**
     * 스크랩/해제 토글
     */
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
                    .orElseThrow(() -> new CustomException(ErrorCode.NEWS_NOT_FOUND)); // [CHANGED]
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
     * 내가 저장한 뉴스: /news/bookmarks?lastId&size  (lastId = scrapId)
     */
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

        List<NewsCardResponse> items = newsList.stream()
                .map(n -> new NewsCardResponse(
                        n.getId(),
                        n.getTitle(),
                        n.getAuthor(),
                        n.getNewsPaper(),
                        n.getCreatedAt(),
                        n.getViews(),
                        n.getCategory(),
                        n.getThumbnail()
                ))
                .toList();

        return new CursorPage<>(items, null, hasNext);
    }
}
