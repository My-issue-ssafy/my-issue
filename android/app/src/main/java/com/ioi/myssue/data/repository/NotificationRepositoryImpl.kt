package com.ioi.myssue.data.repository

import android.util.Log
import com.ioi.myssue.data.dto.response.toDomain
import com.ioi.myssue.data.network.api.NotificationApi
import com.ioi.myssue.domain.model.NotificationPage
import com.ioi.myssue.domain.repository.NotificationRepository
import javax.inject.Inject

private const val TAG = "NotificationRepositoryImpl"
class NotificationRepositoryImpl @Inject constructor(
    private val notificationApi: NotificationApi,
) : NotificationRepository {
    override suspend fun getNotifications(
        lastId: Int?,
        size: Int
    ): NotificationPage {
        val dto = notificationApi.getNotifications(lastId, size)
        val domain = dto.toDomain()
        return domain
    }

    override suspend fun getUnreadNotification(): Boolean = notificationApi.getUnreadNotification()

    override suspend fun deleteNotification(id: Int) {
        val res = notificationApi.deleteNotification(id)
        if (!res.isSuccessful) error("delete fail code=${res.code()}")
    }

    override suspend fun deleteNotificationAll() {
        notificationApi.deleteNotificationAll()
    }

    override suspend fun getStatus() = notificationApi.getNotificationStatus()
    override suspend fun setStatus() {
        val res = notificationApi.setNotificationStatus()
        if (!res.isSuccessful) {
            error("Failed to update notification status. code=${res.code()}")
        }
    }

    override suspend fun readNotification(id: Int) {
        notificationApi.readNotification(id)
    }
}