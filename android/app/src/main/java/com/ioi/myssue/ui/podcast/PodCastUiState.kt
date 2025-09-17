package com.ioi.myssue.ui.podcast

import androidx.media3.common.Player
import com.ioi.myssue.player.AudioState
import java.time.LocalDate

data class PodCastUiState(
    val isMonthlyView: Boolean = false,
    val showPlayer: Boolean = false,
    val contentType: PodcastContentType = PodcastContentType.SCRIPT,
    val selectedDate: LocalDate = LocalDate.now(),
    val audio: AudioState = AudioState(),
    val episode: PodcastEpisode = PodcastEpisode(),
    val currentIndex: Int = 0,
    val currentLine: ScriptLine = ScriptLine(),
    val previousLine: ScriptLine = ScriptLine(),
) {
    val isLoading: Boolean
        get() = audio.playbackState == Player.STATE_IDLE || audio.playbackState == Player.STATE_BUFFERING

    val selectedDateString get() = selectedDate.toString().replace('-', '.')
}

enum class PodcastContentType {
    SCRIPT,
    NEWS
}
data class PodcastEpisode(
    val audioUrl: String = "",
    val scripts: List<ScriptLine> = emptyList(),
    val articleImage: String = "",
    val title: String = "",
    val category: String = "",
    val keywords: List<String> = emptyList(),
)

data class ScriptLine(
    val startMs: Long = 0L,
    val text: String = "",
    val isLeftSpeaker: Boolean = true
)

val dummyEpisodes = listOf(
    PodcastEpisode(
        audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
        scripts = listOf(
            ScriptLine(300, "인트로 - 어제의 HOT 뉴스 시작합니다", true),
            ScriptLine(3_000, "첫 번째 소식, 오늘의 주요 이슈는...", true),
            ScriptLine(7_000, "다음 소식으로 넘어가 보겠습니다", false),
            ScriptLine(11_000, "오늘의 마무리, 내일 다시 찾아뵙겠습니다", true),

            ScriptLine(15_000, "두 번째 뉴스, 경제 동향을 짚어봅니다", true),
            ScriptLine(19_000, "오늘 주식시장은 큰 폭의 변동을 보였습니다", false),
            ScriptLine(23_000, "환율 역시 눈여겨볼 만한 수준입니다", true),
            ScriptLine(27_000, "전문가들은 단기 조정에 불과하다고 전망했습니다", false),

            ScriptLine(31_000, "세 번째 뉴스, 사회 이슈를 살펴봅니다", true),
            ScriptLine(35_000, "서울 도심에서는 교통 대란이 발생했습니다", true),
            ScriptLine(39_000, "출근길 시민들은 큰 불편을 겪었습니다", false),
            ScriptLine(43_000, "이에 대한 대책 마련이 시급하다는 목소리가 큽니다", true),

            ScriptLine(47_000, "네 번째 소식, 국제 뉴스입니다", true),
            ScriptLine(51_000, "미국과 유럽의 정상회담 소식이 전해졌습니다", false),
            ScriptLine(55_000, "기후 변화 대응에 대한 논의가 이어졌습니다", true),
            ScriptLine(59_000, "각국은 탄소 배출 감축 목표를 재확인했습니다", false),

            ScriptLine(63_000, "다섯 번째 뉴스, IT 업계 소식입니다", true),
            ScriptLine(67_000, "신형 스마트폰이 공개되며 큰 화제를 모았습니다", false),
            ScriptLine(71_000, "혁신적인 카메라 기술과 배터리 성능이 주목됩니다", true),
            ScriptLine(75_000, "소비자 반응도 뜨겁게 이어지고 있습니다", false),

            ScriptLine(79_000, "여섯 번째 소식, 문화계 뉴스를 전합니다", true),
            ScriptLine(83_000, "올해를 빛낸 영화와 음악 시상식이 열렸습니다", true),
            ScriptLine(87_000, "다양한 작품들이 수상의 영예를 안았습니다", false),
            ScriptLine(91_000, "관객과 팬들의 환호 속에 막을 내렸습니다", true),

            ScriptLine(95_000, "마지막 뉴스, 오늘의 날씨 소식입니다", true),
            ScriptLine(99_000, "전국적으로 맑고 선선한 날씨가 이어지겠습니다", false),
            ScriptLine(103_000, "다만 일부 지역에는 비 소식이 예보되어 있습니다", true),
            ScriptLine(107_000, "건강 관리에 유의하시길 바랍니다", false),

            ScriptLine(111_000, "이상으로 오늘의 뉴스를 모두 마치겠습니다", true),
            ScriptLine(115_000, "청취해주셔서 감사합니다. 내일 다시 뵙겠습니다", true)
        ),
        articleImage = "https://imgnews.pstatic.net/image/008/2025/09/11/0005248801_001_20250911082312198.jpg?type=w860",
        title = "어제의 HOT 뉴스 모음",
        category = "정치/경제",
        keywords = listOf("정치", "경제", "핫뉴스")
    ),
    PodcastEpisode(
        audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
        scripts = listOf(
            ScriptLine(300, "오늘의 두 번째 에피소드 시작합니다", true),
            ScriptLine(4_000, "주요 이슈를 살펴봅니다", false),
            ScriptLine(8_000, "심층 분석으로 넘어갑니다", true),
            ScriptLine(12_000, "마무리하며 인사드립니다", false)
        ),
        articleImage = "https://imgnews.pstatic.net/image/008/2025/09/11/0005248978_001_20250911110016021.jpg?type=w860",
        title = "오늘의 심층 뉴스",
        category = "사회",
        keywords = listOf("사회", "분석", "이슈")
    )
)
