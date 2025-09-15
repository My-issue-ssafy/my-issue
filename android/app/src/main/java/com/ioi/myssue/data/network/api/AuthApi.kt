package com.ioi.myssue.data.network.api

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/device")
    suspend fun addUser(
        @Body request: AddUserRequest
    ) : Response<AddUserResponse>

    @POST("auth/reissue")
    suspend fun reissueToken(
    ): Response<Unit>
}

@Serializable
data class AddUserRequest(
    val deviceUuid: String
)

@Serializable
data class AddUserResponse(
    val userId: Long
)