package com.ioi.myssue.data.repository.fake

import com.ioi.myssue.common.util.TimeConverter
import com.ioi.myssue.domain.model.CursorPage
import com.ioi.myssue.domain.model.MainNewsList
import com.ioi.myssue.domain.model.News
import com.ioi.myssue.domain.model.NewsBlock
import com.ioi.myssue.domain.model.NewsPage
import com.ioi.myssue.domain.model.NewsSummary
import com.ioi.myssue.domain.repository.NewsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeNewsRepositoryImpl @Inject constructor(
    private val time: TimeConverter
) : NewsRepository {

    private val zone = ZoneId.of("Asia/Seoul")
    private val isoLocal = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    private data class Raw(
        val id: Long,
        val title: String,
        val author: String,
        val press: String,
        val createdAtRaw: String,
        val views: Int,
        val category: String,
        val thumb: String?,
        val blocks: List<NewsBlock>,
        val scrapCount: Int
    )

    private val categories = listOf("정치", "경제", "사회", "세계", "생활/문화", "IT/과학")
    private val thumbs = listOf(
        "https://picsum.photos/id/1015/800/450",
        "https://picsum.photos/id/1025/800/450",
        "https://picsum.photos/id/1043/800/450",
        "https://picsum.photos/id/1050/800/450",
        null // 썸네일 없는 케이스도 포함
    )

    private fun makeBlocks(i: Int): List<NewsBlock> = listOf(
        NewsBlock.Text("페이크 본문 시작 #$i — 영호남 상생협력 화합대축전 개막… 청년 버스킹과 함께 시민 참여가 이어졌다."),
        NewsBlock.Image("https://picsum.photos/id/${100 + (i % 50)}/1280/720"),
        NewsBlock.Desc("개막식 현장 스케치 사진"),
        NewsBlock.Text("지역 화합과 경제 협력 의지를 다지는 자리로, 다양한 체험 부스와 공연이 진행되었다.")
    )

    private val rawAll: List<Raw> by lazy {
        val base = LocalDateTime.now(zone)
        List(120) { idx ->
            Raw(
                id = 1000L + idx,
                title = "페이크 뉴스 제목 #${1000 + idx}",
                author = "홍길동${idx % 7} 기자",
                press = listOf("매일경제", "경향신문", "한국일보", "중앙일보", "이데일리")[idx % 5],
                createdAtRaw = base.minusMinutes((idx * 7).toLong()).format(isoLocal),
                views = 5000 - idx, // 값은 있지만 정렬엔 사용하지 않음
                category = categories[idx % categories.size],
                thumb = thumbs[idx % thumbs.size],
                blocks = makeBlocks(idx),
                scrapCount = 0
            )
        }
    }

    private val hotAll = rawAll
    private val recommendAll = rawAll.drop(20) + rawAll.take(20)
    private val trendAll = rawAll.drop(40) + rawAll.take(40)

    private fun Raw.toSummary(): NewsSummary = NewsSummary(
        newsId = id,
        title = title,
        author = author,
        newspaper = press,
        createdAt = createdAtRaw,
        views = views,
        category = category,
        thumbnail = thumb ?: blocks.firstOrNull { it is NewsBlock.Image }?.let { (it as NewsBlock.Image).url },
        relativeTime = time.toRelative(createdAtRaw)
    )

    private fun Raw.toDetail(): News = News(
        newsId = id,
        title = title,
        author = author,
        newspaper = press,
        createdAt = createdAtRaw,
        views = views,
        category = category,
        thumbnail = thumb
            ?: blocks.firstOrNull { it is NewsBlock.Image }?.let { (it as NewsBlock.Image).url },
        content = blocks,
        displayTime = time.toDisplay(createdAtRaw),
        scrapCount = scrapCount,
        isScraped = false
    )

    private fun <T> pageOf(list: List<T>, cursor: String?, size: Int?): CursorPage<T> {
        val start = cursor?.toIntOrNull() ?: 0
        val safeSize = size ?: 20
        val slice = list.drop(start).take(safeSize)
        val next = if (start + safeSize < list.size) (start + safeSize).toString() else null
        return CursorPage(items = slice, nextCursor = next, hasNext = next != null)
    }

    override suspend fun getNews(
        keyword: String?,
        category: String?,
        size: Int,
        lastId: Long?
    ): NewsPage {
        TODO("Not yet implemented")
    }


    /* ---------- NewsRepository ---------- */

    override suspend fun getMainNews(): MainNewsList {
        return MainNewsList(
            hot = hotAll.take(5).map { it.toSummary() },
            recommend = recommendAll.take(5).map { it.toSummary() },
            latest = trendAll.take(5).map { it.toSummary() }
        )
    }

    override suspend fun getHotNews(cursor: String?, size: Int): CursorPage<NewsSummary> {
        val summaries = hotAll.map { it.toSummary() }
        return pageOf(summaries, cursor, size)
    }

    override suspend fun getRecommendNews(cursor: String?, size: Int): CursorPage<NewsSummary> {
        val summaries = recommendAll.map { it.toSummary() }
        return pageOf(summaries, cursor, size)
    }

    override suspend fun getTrendNews(cursor: String?, size: Int): CursorPage<NewsSummary> {
        val summaries = trendAll.map { it.toSummary() }
        return pageOf(summaries, cursor, size)
    }

    override suspend fun getNewsDetail(newsId: Long): News {
        val raw = rawAll.firstOrNull { it.id == newsId }
            ?: error("News not found: $newsId")
        return raw.toDetail()
    }


    // 북마크
    private val bookmarkedIds = MutableStateFlow<Set<Long>>(emptySet())

    override suspend fun bookMarkNews(newsId: Long): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun getBookmarkedNews(
        cursor: String?,
        size: Int
    ): CursorPage<NewsSummary> {
        TODO("Not yet implemented")
    }

    override suspend fun chatNews(newsId: Long, question: String): String {
        TODO("Not yet implemented")
    }
}
