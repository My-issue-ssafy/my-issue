package com.ioi.myssue.data.network.api

import com.ioi.myssue.data.dto.response.NotificationPageResponse
import com.ioi.myssue.data.dto.response.NotificationResponse
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

interface NotificationApi {
    // 알림 전체 조회
    @GET("/notification")
    suspend fun getNotifications(
        @Query("lastId") lastId: Int? = null,
        @Query("size") size: Int? = 20
    ): NotificationPageResponse

    // 읽지 않은 알림 조회(헤더)
    @GET("/notification/unread")
    suspend fun getUnreadNotification() : Boolean

    // 알림 단일 삭제
    @DELETE("notification/{id}")
    suspend fun deleteNotification(@Path("id") id: Int): Response<Unit>

    // 알림 전체 삭제
    @DELETE("/notification")
    suspend fun deleteNotificationAll() : Response<Unit>

    // 알림 허용 상태 조회
    @GET("/notification/status")
    suspend fun getNotificationStatus() : Boolean

    // 알림 허용 상태 변경
    @PATCH("/notification/status")
    suspend fun setNotificationStatus(): Response<Unit>

    // 개별 알림 읽음 처리
    @PATCH("notification/{id}/read")
    suspend fun readNotification(@Path("id") id: Int): Response<Unit>
}