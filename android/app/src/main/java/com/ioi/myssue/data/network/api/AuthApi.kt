package com.ioi.myssue.data.network.api

import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST
    suspend fun addUser()
}