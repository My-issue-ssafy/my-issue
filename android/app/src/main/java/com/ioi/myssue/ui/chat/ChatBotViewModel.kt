package com.ioi.myssue.ui.chat

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ioi.myssue.domain.model.NewsSummary
import com.ioi.myssue.domain.repository.NewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatBotViewModel @Inject constructor(
    private val newsRepository: NewsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChatBotUiState())
    val state = _state.onSubscription { }
        .stateIn(
            scope = viewModelScope,
            started = WhileSubscribed(5_000),
            initialValue = ChatBotUiState()
        )

    fun initData(newsSummary: NewsSummary) {
        _state.update { it.copy(newsSummary = newsSummary) }
    }

    fun updateInputMessage(value: TextFieldValue) {
        _state.update { it.copy(inputMessage = value) }
    }

    fun sendMessage() {
        val userInput = _state.value.inputMessage.text.trim()
        if (userInput.isBlank()) return

        val newsId = _state.value.newsSummary.newsId

        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            text = userInput,
            isUser = true,
            isPending = false
        )

        val placeholderBot = ChatMessage(
            id = UUID.randomUUID().toString(),
            text = "",
            isUser = false,
            isPending = true
        )
        val placeholderId = placeholderBot.id

        _state.update {
            it.copy(
                messages = it.messages + userMsg + placeholderBot,
                inputMessage = TextFieldValue(""),
                isLoading = true
            )
        }

        viewModelScope.launch {
            runCatching {
                newsRepository.chatNews(newsId, _state.value.sid,userInput)
            }.onSuccess { chat ->
                _state.update { st ->
                    val updated = st.messages.map { m ->
                        if (m.id == placeholderId) m.copy(text = chat.answer, isPending = false)
                        else m
                    }
                    st.copy(messages = updated, sid = chat.sid, isLoading = false)
                }
            }.onFailure { e ->
                _state.update { st ->
                    val fallback = "오류가 발생했어요: ${e.localizedMessage ?: e}"
                    val updated = st.messages.map { m ->
                        if (m.id == placeholderId) m.copy(text = fallback, isPending = false)
                        else m
                    }
                    st.copy(messages = updated, isLoading = false)
                }
            }
        }
    }
}

data class ChatBotUiState(
    val messages: List<ChatMessage> = listOf(ChatMessage(text = "어서오십쇼", isUser = false)),
    val newsSummary: NewsSummary = NewsSummary(),
    val inputMessage: TextFieldValue = TextFieldValue(""),
    val sid: String? = null,
    val isLoading: Boolean = false
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val isPending: Boolean = false
)
