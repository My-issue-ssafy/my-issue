package com.ssafy.myissue.news.infrastructure;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.ssafy.myissue.news.domain.News;
import com.ssafy.myissue.news.domain.NewsScrap;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.ssafy.myissue.news.domain.QNews.news;
import static com.ssafy.myissue.news.domain.QNewsScrap.newsScrap;

@Repository
public class NewsRepositoryImpl implements NewsCustomRepository {

    private final JPAQueryFactory query;
    public NewsRepositoryImpl(JPAQueryFactory query) { this.query = query; }

    @Override
    public List<News> findLatestPage(LocalDateTime lastCreatedAt, Long lastNewsId, int size) {
        var where = new BooleanBuilder();
        if (lastCreatedAt != null && lastNewsId != null) {
            where.and(news.createdAt.lt(lastCreatedAt)
                    .or(news.createdAt.eq(lastCreatedAt).and(news.id.lt(lastNewsId))));
        }
        return query.selectFrom(news)
                .where(where)
                .orderBy(news.createdAt.desc(), news.id.desc())
                .limit(size)
                .fetch();
    }

    @Override
    public List<News> findHotPage(Integer lastViews, LocalDateTime lastCreatedAt, Long lastNewsId, int size) {
        var where = new BooleanBuilder();
        if (lastViews != null && lastCreatedAt != null && lastNewsId != null) {
            where.and(
                    news.views.lt(lastViews)
                            .or(news.views.eq(lastViews).and(news.createdAt.lt(lastCreatedAt)))
                            .or(news.views.eq(lastViews).and(news.createdAt.eq(lastCreatedAt)).and(news.id.lt(lastNewsId)))
            );
        }
        return query.selectFrom(news)
                .where(where)
                .orderBy(news.views.desc(), news.createdAt.desc(), news.id.desc())
                .limit(size)
                .fetch();
    }

    @Override
    public List<News> findCategoryLatestPage(String category, LocalDateTime lastCreatedAt, Long lastNewsId, int size) {
        var where = new BooleanBuilder().and(news.category.eq(category));
        if (lastCreatedAt != null && lastNewsId != null) {
            where.and(news.createdAt.lt(lastCreatedAt)
                    .or(news.createdAt.eq(lastCreatedAt).and(news.id.lt(lastNewsId))));
        }
        return query.selectFrom(news)
                .where(where)
                .orderBy(news.createdAt.desc(), news.id.desc())
                .limit(size)
                .fetch();
    }

    @Override
    public List<News> searchPage(String keyword, String category,
                                 LocalDateTime lastCreatedAt, Long lastNewsId, int size) {
        BooleanBuilder where = new BooleanBuilder();

        if (category != null) {
            where.and(news.category.eq(category));
        }
        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            where.and(
                    news.title.lower().like(pattern)
                            .or(news.content.lower().like(pattern))
            );
        }
        if (lastCreatedAt != null && lastNewsId != null) {
            where.and(
                    news.createdAt.lt(lastCreatedAt)
                            .or(news.createdAt.eq(lastCreatedAt).and(news.id.lt(lastNewsId)))
            );
        }

        return query.selectFrom(news)
                .where(where)
                .orderBy(news.createdAt.desc(), news.id.desc())
                .limit(size)
                .fetch();
    }

    @Override
    public Optional<NewsScrap> findScrapByUserIdAndNewsId(Long userId, Long newsIdVal) {
        var row = query.selectFrom(newsScrap)
                .where(newsScrap.userId.eq(userId).and(newsScrap.news.id.eq(newsIdVal)))
                .fetchOne();
        return Optional.ofNullable(row);
    }

    @Override
    public List<NewsScrap> findScrapsWithNewsByUser(Long userId, Long lastScrapId, int size) {
        var where = new BooleanBuilder().and(newsScrap.userId.eq(userId));
        if (lastScrapId != null) where.and(newsScrap.scrapId.lt(lastScrapId)); // scrap_id DESC 키셋
        return query.selectFrom(newsScrap)
                .join(newsScrap.news, news).fetchJoin()
                .where(where)
                .orderBy(newsScrap.scrapId.desc())
                .limit(size)
                .fetch();
    }
}
