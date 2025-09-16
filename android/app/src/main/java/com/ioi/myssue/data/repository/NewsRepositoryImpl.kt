package com.ioi.myssue.data.repository

import com.ioi.myssue.common.util.TimeConverter
import com.ioi.myssue.data.dto.response.NewsDetailResponse
import com.ioi.myssue.data.dto.response.NewsMainResponse
import com.ioi.myssue.data.dto.response.toDomain
import com.ioi.myssue.data.network.api.NewsApiService
import com.ioi.myssue.domain.model.CursorPage
import com.ioi.myssue.domain.model.MainNewsList
import com.ioi.myssue.domain.model.News
import com.ioi.myssue.domain.model.NewsSummary
import com.ioi.myssue.domain.repository.NewsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class NewsRepositoryImpl @Inject constructor(
    private val newsApiService: NewsApiService,
    private val time: TimeConverter
) : NewsRepository {

    override suspend fun getMainNews(): MainNewsList {
        val res: NewsMainResponse = newsApiService.getMainNews()
        return MainNewsList(
            hot = res.hot.map { it.toDomain(time) },
            recommend = res.recommend.map { it.toDomain(time) },
            latest = res.latest.map { it.toDomain(time) },
        )
    }

    override suspend fun getHotNews(
        cursor: String?,
        size: Int
    ): CursorPage<NewsSummary> {
        val res = newsApiService.getHotNews(cursor = cursor, size = size)
        return res.toDomain(time)
    }

    override suspend fun getRecommendNews(cursor: String?, size: Int): CursorPage<NewsSummary> {
        val res = newsApiService.getRecommendNews(cursor = cursor, size = size)
        return res.toDomain(time)
    }

    override suspend fun getTrendNews(cursor: String?, size: Int): CursorPage<NewsSummary> {
        val res = newsApiService.getTrendNews(cursor = cursor, size = size)
        return res.toDomain(time)
    }

    override suspend fun getNewsDetail(newsId: Long): News {
        val res: NewsDetailResponse = newsApiService.getNewsDetail(newsId)
        return res.toDomain(time)
    }

    private val bookmarkedIds = MutableStateFlow<Set<Long>>(emptySet())

    override suspend fun isBookmarked(newsId: Long): Boolean =
        newsId in bookmarkedIds.value

    override suspend fun setBookmarked(newsId: Long, target: Boolean): Boolean {
        bookmarkedIds.update { old ->
            if (target) old + newsId else old - newsId
        }
        return newsId in bookmarkedIds.value
    }
}