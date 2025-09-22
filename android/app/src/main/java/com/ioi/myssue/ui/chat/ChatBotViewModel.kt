package com.ioi.myssue.ui.chat

import android.R.id.message
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ioi.myssue.domain.model.NewsSummary
import com.ioi.myssue.domain.repository.ChatBotRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatBotViewModel @Inject constructor(
    private val getChatBotRepository: ChatBotRepository
): ViewModel() {

    private var _state = MutableStateFlow(ChatBotUiState())
    val state = _state.onSubscription {

    }.stateIn(
        scope = viewModelScope,
        started = WhileSubscribed(5_000),
        initialValue = ChatBotUiState()
    )

    fun sendMessage() {
        val currentMessages = _state.value.messages
        val newMessage = ChatMessage(
            id = System.currentTimeMillis(),
            text = _state.value.inputMessage.toString(),
            isUser = true,
            timestamp = System.currentTimeMillis()
        )
        _state.value = _state.value.copy(
            messages = currentMessages + newMessage,
            inputMessage = TextFieldValue(""),
            isLoading = true
        )

        // Simulate bot response after a delay
        viewModelScope.launch {
            val botResponse = ChatMessage(
                id = System.currentTimeMillis(),
                text = "This is a bot response to: $message",
                isUser = false,
                timestamp = System.currentTimeMillis()
            )
            _state.value = _state.value.copy(
                messages = _state.value.messages + botResponse,
                isLoading = false
            )
        }
    }

    fun updateInputMessage(value: TextFieldValue) {
        _state.update { it.copy(inputMessage = value) }
    }
}

data class ChatBotUiState(
    val messages: List<ChatMessage> = listOf(
        ChatMessage(
            id = 1L,
            text = "안녕하세요! 무엇을 도와드릴까요?",
            isUser = false,
            timestamp = System.currentTimeMillis() - 1000 * 60 * 5 // 5분 전
        ),
        ChatMessage(
            id = 2L,
            text = "오늘 날씨가 어때?",
            isUser = true,
            timestamp = System.currentTimeMillis() - 1000 * 60 * 4 // 4분 전
        ),
        ChatMessage(
            id = 3L,
            text = "서울은 맑고, 현재 기온은 24도입니다 ☀️",
            isUser = false,
            timestamp = System.currentTimeMillis() - 1000 * 60 * 3 // 3분 전
        ),
        ChatMessage(
            id = 4L,
            text = "좋네! 내일은?",
            isUser = true,
            timestamp = System.currentTimeMillis() - 1000 * 60 * 2 // 2분 전
        ),
        ChatMessage(
            id = 5L,
            text = "내일도 맑고 따뜻할 예정이에요 🌼",
            isUser = false,
            timestamp = System.currentTimeMillis() - 1000 * 60 // 1분 전
        ),
        ChatMessage(
            id = 4L,
            text = "좋네! 내일은?",
            isUser = true,
            timestamp = System.currentTimeMillis() - 1000 * 60 * 2 // 2분 전
        ),
        ChatMessage(
            id = 5L,
            text = "내일도 맑고 따뜻할 예정이에요 🌼",
            isUser = false,
            timestamp = System.currentTimeMillis() - 1000 * 60 // 1분 전
        ),
        ChatMessage(
            id = 4L,
            text = "좋네! 내일은?",
            isUser = true,
            timestamp = System.currentTimeMillis() - 1000 * 60 * 2 // 2분 전
        ),
        ChatMessage(
            id = 5L,
            text = "내일도 맑고 따뜻할 예정이에요 🌼",
            isUser = false,
            timestamp = System.currentTimeMillis() - 1000 * 60 // 1분 전
        ),
        ChatMessage(
            id = 4L,
            text = "좋네! 내일은?",
            isUser = true,
            timestamp = System.currentTimeMillis() - 1000 * 60 * 2 // 2분 전
        ),
        ChatMessage(
            id = 5L,
            text = "내일도 맑고 따뜻할 예정이에요 🌼",
            isUser = false,
            timestamp = System.currentTimeMillis() - 1000 * 60 // 1분 전
        )
    ),
    val newsSummary: NewsSummary = NewsSummary(),
    val inputMessage: TextFieldValue = TextFieldValue("qweqweq"),
    val isLoading: Boolean = false,
)

data class ChatMessage(
    val id: Long,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long,
)