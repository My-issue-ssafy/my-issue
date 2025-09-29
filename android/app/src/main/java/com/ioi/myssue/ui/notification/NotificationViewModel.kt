package com.ioi.myssue.ui.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ioi.myssue.common.util.TimeConverter
import com.ioi.myssue.domain.model.Notification
import com.ioi.myssue.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

private const val TAG = "NotificationViewModel"
@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val time: TimeConverter
) : ViewModel() {
    private val _state = MutableStateFlow(NotificationUiState())
    val state: StateFlow<NotificationUiState> = _state.asStateFlow()

    private val raw = mutableListOf<Notification>()
    private var lastId: Int? = null
    private val seenIds = mutableSetOf<Int>()

    fun refresh() {
        lastId = null
        raw.clear()
        seenIds.clear()
        _state.value = NotificationUiState(isInitialLoading = true)
        loadMore()
    }

    fun loadMore() {
        val cur = _state.value
        if (lastId != null && !cur.hasNext) return
        viewModelScope.launch {
            val page = notificationRepository.getNotifications(lastId, size = 20)

            // 이미 본 알림 제거 (중복 방지)
            val newOnes = page.content.filter { seenIds.add(it.notificationId) }
            raw += newOnes

            _state.update {
                it.copy(
                    items = buildUi(raw),
                    isInitialLoading = false,
                    hasNext = page.hasNext
                )
            }
            lastId = page.content.lastOrNull()?.notificationId
        }
    }


    fun formatTime(raw: String): String = time.toNotificationDisplay(raw)

    private fun sectionOf(raw: String, nowDate: LocalDate = LocalDate.now()): SectionType? {
        val date = time.toLocalDateOrNull(raw) ?: return null
        return when {
            date.isEqual(nowDate) -> SectionType.TODAY
            date.isEqual(nowDate.minusDays(1)) -> SectionType.YESTERDAY
            !date.isBefore(nowDate.minusDays(30)) -> SectionType.LAST_30_DAYS
            else -> null
        }
    }

    private fun buildUi(list: List<Notification>): List<NotificationListItem> {
        val today = mutableListOf<Notification>()
        val yesterday = mutableListOf<Notification>()
        val last30 = mutableListOf<Notification>()
        val now = LocalDate.now()

        list.forEach { n ->
            when (sectionOf((n.createdAt), now)) {
                SectionType.TODAY -> today += n
                SectionType.YESTERDAY -> yesterday += n
                SectionType.LAST_30_DAYS -> last30 += n
                null -> {}
            }
        }

        val ui = mutableListOf<NotificationListItem>()
        fun addSection(items: List<Notification>, type: SectionType) {
            if (items.isNotEmpty()) {
                ui += NotificationListItem.Header(type)
                ui += items.map { NotificationListItem.Row(it) }
            }
        }
        addSection(today, SectionType.TODAY)
        addSection(yesterday, SectionType.YESTERDAY)
        addSection(last30, SectionType.LAST_30_DAYS)
        return ui
    }

    fun onItemClick(n: Notification) {
        if (!n.read) {
            val idx = raw.indexOfFirst { it.notificationId == n.notificationId }
            if (idx >= 0) {
                val updated = raw[idx].copy(read = true)
                raw[idx] = updated
                _state.update { it.copy(items = buildUi(raw)) }
            }
        }
        // 읽음 처리
        viewModelScope.launch {
            runCatching { notificationRepository.readNotification(n.notificationId) }
                .onFailure { }
        }
        // 기사 시트 열기
        _state.update { it.copy(selectedNewsId = n.newsId.toLong()) }
    }
    fun onItemClose() {
        _state.update { it.copy(selectedNewsId = null) }
    }

    // 단일 삭제
    fun deleteNotification(n: Notification) {
        val removed = raw.removeIf { it.notificationId == n.notificationId }
        if (removed) {
            seenIds.remove(n.notificationId)
            _state.update { it.copy(items = buildUi(raw)) }
        }
        viewModelScope.launch {
            runCatching { notificationRepository.deleteNotification(n.notificationId) }
                .onFailure { }
        }
    }

    // 전체 삭제
    fun deleteNotificationAll() {
        raw.clear()
        seenIds.clear()
        lastId = null
        _state.update {
            it.copy(
                items = emptyList(),
                hasNext = false,
                selectedNewsId = null,
                isInitialLoading = false
            )
        }

        viewModelScope.launch {
            runCatching { notificationRepository.deleteNotificationAll() }
                .onFailure {

                }
        }
    }
}

