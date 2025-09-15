package com.ioi.myssue.domain.model

data class CursorPage<News>(
    val items: List<News>,
    val nextCursor: String?,
    val hasNext: Boolean
)