package com.ioi.myssue.data.dto.request

import kotlinx.serialization.Serializable

@Serializable
data class ChatRequest(
    val question: String,
    val sid: String?
)