package com.ioi.myssue.ui.podcast

import com.ioi.myssue.player.AudioState
import java.time.LocalDate

data class PodcastUiState(
    val isMonthlyView: Boolean = false,
    val selectedDate: LocalDate = LocalDate.now(),
    val audio: AudioState = AudioState(),
    val currentLine: String = "",
    val previousLine: String = "",
    val episode: PodcastEpisode = PodcastEpisode()
)

data class PodcastEpisode(
    val audioUrl: String = "",
    val scripts: List<ScriptLine> = emptyList(),
    val articleImage: String = "",
    val title: String = "",
    val category: String = "",
    val keywords: List<String> = emptyList(),
)

data class ScriptLine(
    val startMs: Long,
    val text: String
)

val dummyEpisodes = listOf(
    PodcastEpisode(
        audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
        scripts = listOf(
            ScriptLine(300, "인트로 - 어제의 HOT 뉴스 시작합니다"),
            ScriptLine(3_000, "첫 번째 소식, 오늘의 주요 이슈는...\nweqweqweqweqweqw"),
            ScriptLine(7_000, "다음 소식으로 넘어가 보겠습니다"),
            ScriptLine(11_000, "오늘의 마무리, 내일 다시 찾아뵙겠습니다")
        ),
        articleImage = "https://imgnews.pstatic.net/image/008/2025/09/11/0005248801_001_20250911082312198.jpg?type=w860",
        title = "어제의 HOT 뉴스 모음",
        category = "정치/경제",
        keywords = listOf("정치", "경제", "핫뉴스")
    ),
    PodcastEpisode(
        audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
        scripts = listOf(
            ScriptLine(300, "오늘의 두 번째 에피소드 시작합니다"),
            ScriptLine(4_000, "주요 이슈를 살펴봅니다"),
            ScriptLine(8_000, "심층 분석으로 넘어갑니다"),
            ScriptLine(12_000, "마무리하며 인사드립니다")
        ),
        articleImage = "https://imgnews.pstatic.net/image/008/2025/09/11/0005248978_001_20250911110016021.jpg?type=w860",
        title = "오늘의 심층 뉴스",
        category = "사회",
        keywords = listOf("사회", "분석", "이슈")
    )
)
