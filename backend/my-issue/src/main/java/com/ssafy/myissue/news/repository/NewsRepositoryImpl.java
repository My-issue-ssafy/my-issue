package com.ssafy.myissue.news.repository;

import com.querydsl.core.BooleanBuilder; // 동적 where 조건을 필요할 때만 붙이고 싶을 때 쓰는 조건 조립기
import com.querydsl.jpa.impl.JPAQueryFactory; // QueryDSL 쿼리를 시작하는 팩토리. 내부적으로 JPA의 EntityManager를 사용해 SQL 실행.
import com.ssafy.myissue.news.entity.News;
import com.ssafy.myissue.news.entity.NewsCategory;
import com.ssafy.myissue.news.entity.NewsImage;
import com.ssafy.myissue.news.entity.NewsScrap;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

// QueryDSL이 컴파일 타임에 생성한 Q-메타모델 클래스들을 정적 임포트.
import static com.ssafy.myissue.news.entity.QNews.news;
import static com.ssafy.myissue.news.entity.QNewsImage.newsImage;
import static com.ssafy.myissue.news.entity.QNewsScrap.newsScrap;

@Repository
public class NewsRepositoryImpl implements NewsCustomRepository {
    // 이 클래스가 커스텀 레포 구현체임을 표시. 이름 규칙 중요함 뒤에 Impl 붙이는 거.

    private final JPAQueryFactory query; // 생성자 주입
    public NewsRepositoryImpl(JPAQueryFactory query) { this.query = query; }

    @Override
    public List<News> findLatestPage(LocalDateTime lastCreatedAt, Long lastNewsId, int size) {
        // 최신 전체 뉴스를 가져오는 메서드. lastCreatedAt, lastNewsId는 커서 경계값.
        var where = new BooleanBuilder(); // 동적 where 시작. var는 지역 변수 타입.
        if (lastCreatedAt != null && lastNewsId != null) {
            where.and(news.createdAt.lt(lastCreatedAt)
                    .or(news.createdAt.eq(lastCreatedAt).and(news.newsId.lt(lastNewsId))));
        } // 커서가 있을 떄만 키셋 조건 추가. 직전 마지막 (createdAt, newsId)보다 작은 것만 가져오라는 뜻.
        // 더 과거 시각: createdAt < lastCreatedAt
        // 만일 시간이 같다면 id를 더 작은 거: createdAt = lastCreatedAt AND newsId < lastNewsId
        return query.selectFrom(news) // FROM news
                .where(where) // 첫 페이지면 빈 where -> 전체, 다음 페이지면 키셋 where 적용
                .orderBy(news.createdAt.desc(), news.newsId.desc()) // 불변 정렬
                .limit(size) // 요청 개수만금
                .fetch(); // List<news> 반환
    }

    @Override
    public List<News> findHotPage(Integer lastViews, LocalDateTime lastCreatedAt, Long lastNewsId, int size) {
        // 정렬 키 3개임 (views 많은 순에 createdAt 최신순에 newsId 최신순인. ??newsId 최신순은 필요없지 않나? 그냥 createdAt 최신순과 views 많은 순만 고려하면 되는데?)
        var where = new BooleanBuilder();
        if (lastViews != null && lastCreatedAt != null && lastNewsId != null) {
            where.and(
                    news.views.lt(lastViews)
                            .or(news.views.eq(lastViews).and(news.createdAt.lt(lastCreatedAt)))
                            .or(news.views.eq(lastViews).and(news.createdAt.eq(lastCreatedAt)).and(news.newsId.lt(lastNewsId)))
            );
            // 조회수가 더 작거나
            // 조회수가 같으면 createdAt이 더 과거거나
            // 둘 다 같으면 id가 더 작거나
        }
        return query.selectFrom(news)
                .where(where)
                .orderBy(news.views.desc(), news.createdAt.desc(), news.newsId.desc())
                .limit(size)
                .fetch();
    }

    @Override
    public List<News> findCategoryLatestPage(NewsCategory category, LocalDateTime lastCreatedAt, Long lastNewsId, int size) {
        // 카테고리별 최신 기사
        var where = new BooleanBuilder().and(news.category.eq(category));
        if (lastCreatedAt != null && lastNewsId != null) {
            where.and(news.createdAt.lt(lastCreatedAt)
                    .or(news.createdAt.eq(lastCreatedAt).and(news.newsId.lt(lastNewsId))));
        }
        return query.selectFrom(news)
                .where(where)
                .orderBy(news.createdAt.desc(), news.newsId.desc())
                .limit(size)
                .fetch();
    }

    @Override
    public List<NewsImage> findImagesByNewsId(Long newsId) {
        return query.selectFrom(newsImage)
                .where(newsImage.news.newsId.eq(newsId))
                .orderBy(newsImage.news.newsId.asc(), newsImage.imageIndex.asc())
                .fetch();
    }

    @Override
    public List<NewsImage> findImagesByNewsIds(Collection<Long> newsIds) {
        if (newsIds == null || newsIds.isEmpty()) return Collections.emptyList();
        return query.selectFrom(newsImage)
                .where(newsImage.news.newsId.in(newsIds))
                .orderBy(newsImage.news.newsId.asc(), newsImage.imageIndex.asc())
                .fetch();
    }

    @Override
    public Optional<NewsScrap> findScrapByUserIdAndNewsId(Long userId, Long newsIdVal) {
        // 스크랩 중복 확인
        var row = query.selectFrom(newsScrap)
                .where(newsScrap.userId.eq(userId).and(newsScrap.news.newsId.eq(newsIdVal)))
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
