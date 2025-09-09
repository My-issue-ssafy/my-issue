package com.ioi.myssue.data.repository

import com.ioi.myssue.common.util.TimeConverter
import com.ioi.myssue.domain.model.News
import com.ioi.myssue.domain.repository.NewsRepository
import javax.inject.Inject

class FakeNewsRepositoryImpl @Inject constructor(
    private val time: TimeConverter
): NewsRepository {
    override suspend fun getHotNews(): List<News> =
        List(5) { idx ->
            News(
                title = "HOT 뉴스 $idx",
                content = "내용",
                url = "https://example.com/$idx",
                img = "",
                category = "이슈",
                createdAt = "2025-09-08T10:00:00Z",
                views = 0
            )
        }

    override suspend fun getRecommendedNews(): List<News> =
        List(5) { idx ->
            News(
                title = "맞춤 뉴스 $idx",
                content = "내용",
                url = "https://example.com/$idx",
                img = "",
                category = "사회",
                createdAt = "2025-09-08T09:00:00",
                views = 0
            )
        }.map { it.copy(createdAt = time.toRelative(it.createdAt)) }

    override suspend fun getRecentNews(): List<News> =
        List(5) { idx ->
            News(
                title = "HOT 뉴스 $idx",
                content = "내용",
                url = "https://example.com/$idx",
                img = "",
                category = "이슈",
                createdAt = "2025-09-09T03:00:00Z",
                views = 0
            )
        }.map { it.copy(createdAt = time.toRelative(it.createdAt)) }
}
