package com.ioi.myssue.common.util

import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimeConverter @Inject constructor(
    private val zone: ZoneId
) {
    // 상대 시간으로 변환
    /** 입력 문자열 -> 상대시간 문자열 */
    fun toRelative(createdAtRaw: String, now: Instant = Instant.now()): String {
        val published = parseToInstant(createdAtRaw) ?: return createdAtRaw
        val d = Duration.between(published, now)

        if (d.isNegative) {
            // 미래값이면 날짜로
            return fmtDate(published)
        }

        val minutes = d.toMinutes()
        val hours = d.toHours()
        val days = d.toDays()

        return when {
            minutes < 1 -> "방금 전"
            minutes < 60 -> "${minutes}분 전"
            hours   < 24 -> "${hours}시간 전"
            days    < 7  -> "${days}일 전"
            else         -> fmtDate(published)
        }
    }

    private fun fmtDate(instant: Instant): String =
        DateTimeFormatter.ofPattern("yyyy.MM.dd").withZone(zone).format(instant)

    private fun parseToInstant(value: String): Instant? {
        // ISO 계열 우선
        try { return Instant.parse(value) } catch (_: Exception) {}
        try { return OffsetDateTime.parse(value).toInstant() } catch (_: Exception) {}
        try { return ZonedDateTime.parse(value).toInstant() } catch (_: Exception) {}

        // 로컬 포맷들 (서버가 시간대 없이 줄 때)
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy/MM/dd HH:mm",
            "yyyy-MM-dd"
        )
        for (p in patterns) {
            try {
                val dt = LocalDateTime.parse(value, DateTimeFormatter.ofPattern(p))
                return dt.atZone(zone).toInstant()
            } catch (_: DateTimeParseException) {}
        }
        return null
    }

    // 화면 표시용 시간으로 변환
    private val DISPLAY_DATE_TIME =
        DateTimeFormatter.ofPattern("yyyy.MM.dd. HH:mm").withZone(zone)

    /** 입력 문자열 -> "yyyy.MM.dd. HH:mm" */
    fun toDisplay(createdAtRaw: String): String {
        val instant = parseToInstant(createdAtRaw) ?: return createdAtRaw
        return DISPLAY_DATE_TIME.format(instant)
    }
}