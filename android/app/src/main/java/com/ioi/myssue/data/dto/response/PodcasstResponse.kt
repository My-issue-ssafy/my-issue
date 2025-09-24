package com.ioi.myssue.data.dto.response

import com.ioi.myssue.domain.model.PodcastEpisode
import com.ioi.myssue.domain.model.ScriptLine
import kotlinx.serialization.Serializable

@Serializable
data class PodcastResponse(
    val keyword: List<String>,
    val podcastId: Long,
    val podcastUrl: String,
    val subtitles: List<Subtitle>,
    val thumbnail: String
)

@Serializable
data class Subtitle(
    val line: String,
    val speaker: Int,
    val startTime: Long
)

fun PodcastResponse.toDomain() = PodcastEpisode(
    podcastId = podcastId,
    thumbnail = thumbnail,
    podcastUrl = podcastUrl,
    keyWords = keyword,
    scripts = subtitles.map { it.toDomain() }
)

fun Subtitle.toDomain() = ScriptLine(
    startMs = startTime,
    line = line,
    isLeftSpeaker = if(speaker == 1) true else false
)