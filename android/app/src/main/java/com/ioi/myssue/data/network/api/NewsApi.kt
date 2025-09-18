package com.ioi.myssue.data.network.api

import com.ioi.myssue.data.dto.response.CursorPageNewsResponse
import com.ioi.myssue.data.dto.response.NewsCardResponse
import com.ioi.myssue.data.dto.response.NewsDetailResponse
import com.ioi.myssue.data.dto.response.NewsMainResponse
import com.ioi.myssue.data.dto.response.ScrapResponse
import kotlinx.serialization.Serializable

import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface NewsApi {
    // 뉴스 전체 조회
    @GET("/news")
    suspend fun getNews(
        @Query("keyword") keyword: String? = null,
        @Query("category") category: String? = null,
        @Query("size") size: Int? = 20,
        @Query("lastId") lastId: Long? = null
    ): CursorPageNewsResponse

    // HOT 뉴스 전체 조회
    @GET("news/hot")
    suspend fun getHotNews(
        @Query("cursor") cursor: String? = null,
        @Query("size") size: Int? = 20
    ): CursorPageNewsResponse

    // 추천 뉴스 전체 조회
    @GET("news/recommend")
    suspend fun getRecommendNews(
        @Query("cursor") cursor: String? = null,
        @Query("size") size: Int? = 20
    ): CursorPageNewsResponse

    // 최신 뉴스 전체 조회
    @GET("news/trend")
    suspend fun getTrendNews(
        @Query("cursor") cursor: String? = null,
        @Query("size") size: Int? = 20
    ): CursorPageNewsResponse

    // 메인 화면 뉴스 조회
    @GET("news/main")
    suspend fun getMainNews(
        @Query("userId") userId: Int? = null
    ): NewsMainResponse

    // 뉴스 상세보기
    @GET("news/{newsId}")
    suspend fun getNewsDetail(
        @Path("newsId") newsId: Long
    ): NewsDetailResponse

    @GET("news/bookmarks")
    suspend fun getBookMarkedNews(
        @Query("size") size: Int? = null,
        @Query("lastId") lastId: Long? = null
    ): CursorPageNewsResponse

    @POST("news/{newsId}/bookmark")
    suspend fun bookMarkNews(
        @Path("newsId") newsId: Long
    ): ScrapResponse
}
