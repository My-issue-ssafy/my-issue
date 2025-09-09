package com.ioi.myssue.domain.repository

import com.ioi.myssue.domain.model.CartoonNews

interface CartoonRepository {

    suspend fun getCartoonNews(): List<CartoonNews>

    suspend fun getLikedCartoons(lastId: Long, size: Int): List<CartoonNews>

    suspend fun likeCartoon(toonId: Long)

    suspend fun hateCartoon(toonId: Long)

    suspend fun cancelLike(toonId: Long)
}