package com.ioi.myssue.domain.repository

interface ChatBotRepository {

    suspend fun getChatResponse(message: String): String

}