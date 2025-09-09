package com.ioi.myssue.data.network.api

import com.ioi.myssue.data.dto.response.ToonResponse
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ToonNewsApiService {

    @GET("toons")
    suspend fun getToons(
        @Query("lastId") lastId: Long? = null
    ): List<ToonResponse>

    @GET("toons/likes")
    suspend fun getLikedToons(
        @Query("lastId") lastId: Long? = null,
        @Query("size") size: Int? = null
    ): List<ToonResponse>

    @POST("toons/{toon_id}/like")
    suspend fun likeToon(
        @Path("toon_id") toonId: Long
    )

    @POST("toons/{toon_id}/hate")
    suspend fun hateToon(
        @Path("toon_id") toonId: Long
    )

    @DELETE("toons/{toon_id}/like")
    suspend fun cancelLikedToon(
        @Path("toon_id") toonId: Long
    )
}
