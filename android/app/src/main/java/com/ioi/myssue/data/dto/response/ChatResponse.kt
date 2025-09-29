package com.ioi.myssue.data.dto.response

import com.ioi.myssue.domain.model.Chat
import kotlinx.serialization.Serializable

@Serializable
data class ChatResponse(
    val answer : String,
    val sid: String
)

fun ChatResponse.toDomain() = Chat(
    answer = answer,
    sid = sid
)