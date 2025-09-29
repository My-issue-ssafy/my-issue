package com.ioi.myssue.data.repository

import com.ioi.myssue.common.util.TimeConverter
import com.ioi.myssue.data.dto.request.ChatRequest
import com.ioi.myssue.data.dto.response.NewsDetailResponse
import com.ioi.myssue.data.dto.response.NewsMainResponse
import com.ioi.myssue.data.dto.response.toDomain
import com.ioi.myssue.data.network.api.NewsApi
import com.ioi.myssue.domain.model.CursorPage
import com.ioi.myssue.domain.model.MainNewsList
import com.ioi.myssue.domain.model.News
import com.ioi.myssue.domain.model.NewsSummary
import com.ioi.myssue.domain.repository.NewsRepository
import javax.inject.Inject

class NewsRepositoryImpl @Inject constructor(
    private val newsApi: NewsApi,
    private val time: TimeConverter
) : NewsRepository {

    override suspend fun getNews(
        keyword: String?,
        category: String?,
        size: Int,
        cursor: String?
    ): CursorPage<NewsSummary> {
        val res = newsApi.getNews(
            keyword = keyword?.ifBlank { null },
            category = category,
            size = size,
            cursor = cursor
        )
        return res.toDomain(time)
    }

    override suspend fun getMainNews(): MainNewsList {
        val res: NewsMainResponse = newsApi.getMainNews()
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
        val res = newsApi.getHotNews(cursor = cursor, size = size)
        return res.toDomain(time)
    }

    override suspend fun getRecommendNews(cursor: String?, size: Int): CursorPage<NewsSummary> {
        val res = newsApi.getRecommendNews(cursor = cursor, size = size)
        return res.toDomain(time)
    }

    override suspend fun getTrendNews(cursor: String?, size: Int): CursorPage<NewsSummary> {
        val res = newsApi.getTrendNews(cursor = cursor, size = size)
        return res.toDomain(time)
    }

    override suspend fun getNewsDetail(newsId: Long): News {
        val res: NewsDetailResponse = newsApi.getNewsDetail(newsId)
        return res.toDomain(time)
    }

    override suspend fun bookMarkNews(newsId: Long) = newsApi.bookMarkNews(newsId).scrapped

    override suspend fun getBookmarkedNews(
        cursor: String?,
    ) = newsApi.getBookMarkedNews(cursor).toDomain(time)

    override suspend fun chatNews(newsId: Long, sid: String?, question: String) =
        newsApi.chatNews(newsId, ChatRequest(question, sid)).toDomain()
}