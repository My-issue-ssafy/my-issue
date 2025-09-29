package com.ioi.myssue.ui.notification

import com.ioi.myssue.domain.model.Notification

data class NotificationUiState(
    val items: List<NotificationListItem> = emptyList(),
    val isInitialLoading: Boolean = true,
    val hasNext: Boolean = true,
    val selectedNewsId: Long? = null
)

enum class SectionType { TODAY, YESTERDAY, LAST_30_DAYS }

fun SectionType.label(): String = when (this) {
    SectionType.TODAY -> "오늘"
    SectionType.YESTERDAY -> "어제"
    SectionType.LAST_30_DAYS -> "최근 30일"
}

sealed class NotificationListItem {
    data class Header(val type: SectionType) : NotificationListItem()
    data class Row(val item: Notification) : NotificationListItem()
}
