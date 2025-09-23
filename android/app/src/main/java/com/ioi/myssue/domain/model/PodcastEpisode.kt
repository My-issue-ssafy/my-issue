package com.ioi.myssue.domain.model

data class PodcastEpisode(
    val podcastId: Long = -1L,
    val thumbnail: String = "",
    val podcastUrl: String = "",
    val keyWord: List<String> = emptyList(),
    val scripts: List<ScriptLine> = emptyList(),
)

data class ScriptLine(
    val startMs: Long = 0L,
    val line: String = "",
    val isLeftSpeaker: Boolean = true
)