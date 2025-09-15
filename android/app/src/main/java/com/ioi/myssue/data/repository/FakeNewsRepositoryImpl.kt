package com.ioi.myssue.data.repository

import com.ioi.myssue.common.util.TimeConverter
import com.ioi.myssue.domain.model.MainNewsList
import com.ioi.myssue.domain.model.News
import com.ioi.myssue.domain.model.NewsBlock
import com.ioi.myssue.domain.model.NewsPage
import com.ioi.myssue.domain.repository.NewsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeNewsRepositoryImpl @Inject constructor(
    private val time: TimeConverter
) : NewsRepository {
    // 썸네일 이미지 추출
    private fun List<NewsBlock>.firstImageUrl(): String? =
        firstNotNullOfOrNull { (it as? NewsBlock.Image)?.url }

    val blocks = listOf(
        NewsBlock.Text(
            "[이데일리 박기주 방보경 김윤정 이영민 기자] “아내랑 사별하고 여기 나와서 장기도 보고 얘기도 하는 게 낙이었는데 이제 적적하지 뭐.”\n" +
                    "\n" +
                    "노인 인구가 급증하며 초고령사회로 진입했지만 정작 이들이 쉴 곳은 사라지고 있다. 노인들 모임의 장소의 대명사였던 ‘탑골공원’에서 노인들은 내쫓겨났고 다른 노인 복지 시설은 포화 상태인데도 확충되지 않고 있다. 여기에 사회·문화적으로 ‘노인 혐오’ 분위기가 커지다 보니 부작용도 커지는 모양새다."
        ),
        NewsBlock.Image("https://img2.daumcdn.net/thumb/R658x0.q70/?fname=https://t1.daumcdn.net/news/202509/05/Edaily/20250905060256110qyqt.jpg"),
        NewsBlock.Desc("지난 7월 서울 종로구 탑골공원 앞에서 노인들이 장기를 두고 있는 모습(왼쪽). 종로구청과 경찰이 최근 환경 정비 사업을 한 이후 이 모습은 사라졌다. (사진= 연합뉴스, 김현재 수습기자)"),
        NewsBlock.Text(
            "서울 종로구와 경찰은 최근 탑골공원 북문 담벼락에 놓여 있던 장기판들을 모두 철거했다. 현재 공원 인근에는 ‘공원 내 관람 분위기를 저해하는 바둑·장기 등 오락행위, 흡연, 음주가무, 상거래 행위 등은 모두 금지됩니다’라는 안내판이 노인들이 모여 있던 자리를 대신하고 있다.\n" +
                    "4일 탑골공원 앞에서 만난 김모(82)씨는 “장기나 바둑을 모두 금지하니 아주 서운하다”며 “술을 마시고 소란을 피우는 건 일부 노숙인인데 우린 같은 급으로 매도하는 게 안타깝다”고 했다. 유모(80)씨도 “집에 있으면 무기력하게 있을 뿐이고 우리 나이에 놀거리가 뭐가 있겠느냐”며 “외로움을 달랠 공간이었는데 구청에서 금지하니 서운하고 허탈하다”고 토로했다.\n" +
                    "\n" +
                    "탑골공원 전체가 국가유산 보호구역인 만큼 인근에서 장기를 두며 모여 있는 것이 관람 분위기를 해치기 때문에 정리할 필요가 있다는 게 당국의 설명이다. 노인들을 인근 복지관으로 안내하고 있다곤 하지만 이들을 수용하기엔 역부족이다. 실제 노인 인구가 2020년 849만명에서 지난해 1025만명으로 20% 이상 급증하는 동안 이들을 위한 복지시설은 2.8% 늘어나는 데 그쳤다.\n" +
                    "\n"
        ),
        NewsBlock.Image("https://img2.daumcdn.net/thumb/R658x0.q70/?fname=https://t1.daumcdn.net/news/202509/05/Edaily/20250905060257434wwmj.jpg"),
        NewsBlock.Desc("(그래픽=김정훈 기자)"),
        NewsBlock.Text(
            "더욱이 ‘노 시니어존’으로 대변되는 노인혐오 현상도 확산하고 있다. 노인들에겐 영업이 끝났다거나 준비중이라고 하던 업주들이 다른 젊은 손님들은 받는 문화가 만연하다는 게 이들의 설명이다. 최근 OTT를 중심으로 콘텐츠 제작이 늘어나다보니 문화 접근성도 점차 악화하고 있다. 이성 간의 소통에서 발생하는 자연스러운 연애 감정도 가족이나 사회의 눈치를 보며 감춰야 하는 것이 현실이라고 노인들은 토로한다.\n" +
                    "이런 상황에 노인들은 극한으로 내몰리고 있다. 경제협력개발기구(OECD) 회원국 중 우리나라 노인빈곤율(39.3%)이 가장 높은 상황에서 하루 평균 노인 자살 인구는 10.5명에 달한다.\n" +
                    "\n" +
                    "전용호 인천대 사회복지학과 교수는 “한국 노인의 높은 자살률은 문화적 인프라 부족과 직결된다”며 “건강한 노후를 위해서는 타인과의 교류를 통해 신체적·정신적 건강을 유지하는 것이 필수”라고 말했다. 이어 “노인 인구가 급격히 증가하는 상황에서 노인의 요구와 여건에 맞는 여가·사회참여 프로그램이 마련돼야 한다”고 짚었다.\n" +
                    "\n" +
                    "박기주 (kjpark85@edaily.co.kr)\n" +
                    "\n" +
                    "Copyright © 이데일리. 무단전재 및 재배포 금지."
        )
    )

    // 기사 id 생성용
    private companion object {
        const val HOT_ID_START: Long = 10_000
        const val RECOMMEND_ID_START: Long  = 20_000
        const val RECENT_ID_START: Long  = 30_000
    }

    // 더미 데이터 뉴스 생성
    private fun makeNews(newsId: Long, count: Int, content: List<NewsBlock>): List<News> =
        List(count) { idx ->
            val id = newsId + idx
            News(
                newsId = id,
                title = "$idx 갈 곳도, 볼 것도, 사랑도 없다”…절벽으로 내몰리는 노인들",
                content = content,
                url = "https://example.com/$idx",
                img = content.firstImageUrl() ?: "",
                category = "사회",
                createdAt = "2025-09-08T10:00:00Z",
                relativeTime = "",
                views = 100 + idx,
                author = "박대기 기자",
                newspaper = "중앙일보",
            )
        }.map { n ->
            val raw = n.createdAt
            n.copy(createdAt = time.toDisplay(raw), relativeTime = time.toRelative(raw))
        }

    private val hotAll = makeNews(HOT_ID_START, 100, blocks)
    private val recommendAll = makeNews(RECOMMEND_ID_START, 100, blocks)
    private val recentAll = makeNews(RECENT_ID_START, 100, blocks)

    // 커서 페이징 (cursor = 시작 인덱스 문자열)
    private fun page(all: List<News>, cursor: String?, pageSize: Int): NewsPage {
        val start = cursor?.toIntOrNull() ?: 0
        val slice = all.drop(start).take(pageSize)
        val next = if (start + pageSize < all.size) (start + pageSize).toString() else null
        return NewsPage(items = slice, nextCursor = next)
    }

    override suspend fun getMainNews(): MainNewsList = MainNewsList (
        hot = hotAll.take(5),
        recommend = recommendAll.take(5),
        recent = recentAll.take(5)
    )

    override suspend fun getHotNews(cursor: String?, pageSize: Int): NewsPage =
        page(hotAll, cursor, pageSize)

    override suspend fun getRecommendNews(cursor: String?, pageSize: Int): NewsPage =
        page(recommendAll, cursor, pageSize)

    override suspend fun getRecentNews(cursor: String?, pageSize: Int): NewsPage =
        page(recentAll, cursor, pageSize)

    override suspend fun getNewsById(newsId: Long): News {
        return (hotAll + recommendAll + recentAll)
            .firstOrNull { it.newsId == newsId }
            ?: throw NoSuchElementException("News not found")
    }

    private val bookmarkedIds = MutableStateFlow<Set<Long>>(emptySet())

    override suspend fun isBookmarked(newsId: Long): Boolean {
        return newsId in bookmarkedIds.value
    }

    override suspend fun setBookmarked(newsId: Long, target: Boolean): Boolean {
        bookmarkedIds.update { old ->
            if (target) old + newsId else old - newsId
        }
        return newsId in bookmarkedIds.value
    }
}
