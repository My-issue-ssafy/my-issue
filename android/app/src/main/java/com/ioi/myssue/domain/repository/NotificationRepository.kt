package com.ioi.myssue.domain.repository

import com.ioi.myssue.data.network.api.NotificationApi
import com.ioi.myssue.domain.model.NotificationPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

interface NotificationRepository {
    suspend fun getNotifications(lastId: Int?, size: Int = 20): NotificationPage
    suspend fun getUnreadNotification() : Boolean
    suspend fun deleteNotification(id: Int)
    suspend fun deleteNotificationAll()
    suspend fun getStatus(): Boolean
    suspend fun setStatus()
    suspend fun readNotification(id: Int)
}