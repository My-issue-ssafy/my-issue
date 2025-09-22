package com.ioi.myssue.data.repository

import com.ioi.myssue.domain.repository.ChatBotRepository
import javax.inject.Inject

class ChatBotRepositoryImpl @Inject constructor(

) : ChatBotRepository {
    override suspend fun getChatResponse(message: String): String {
        TODO("Not yet implemented")
    }
}