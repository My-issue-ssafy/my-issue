package com.ioi.myssue.ui.podcast

import androidx.media3.common.Player
import com.ioi.myssue.domain.model.NewsSummary
import com.ioi.myssue.domain.model.PodcastEpisode
import com.ioi.myssue.domain.model.ScriptLine
import com.ioi.myssue.player.AudioState
import java.time.LocalDate

data class PodCastUiState(
    val isMonthlyView: Boolean = false,
    val showPlayer: Boolean = false,
    val contentType: PodcastContentType = PodcastContentType.SCRIPT,
    val selectedDate: LocalDate = LocalDate.now().minusDays(1),
    val audio: AudioState = AudioState(),
    val episode: PodcastEpisode = PodcastEpisode(),
    val detailNewsId: Long? = null,
    val currentScriptIndex: Int = 0,
    val currentLine: ScriptLine = ScriptLine(),
    val previousLine: ScriptLine = ScriptLine(),
    val podcastNewsSummaries: List<NewsSummary> = emptyList()
) {
    val isLoading: Boolean
        get() = audio.playbackState == Player.STATE_IDLE || audio.playbackState == Player.STATE_BUFFERING

    val selectedDateString get() = selectedDate.toString().replace('-', '.')
}

enum class PodcastContentType {
    SCRIPT,
    NEWS
}
