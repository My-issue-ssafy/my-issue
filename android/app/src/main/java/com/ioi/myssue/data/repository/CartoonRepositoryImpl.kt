package com.ioi.myssue.data.repository

import com.ioi.myssue.data.dto.response.toDomain
import com.ioi.myssue.data.network.api.ToonNewsApiService
import com.ioi.myssue.domain.model.CartoonNews
import com.ioi.myssue.domain.repository.CartoonRepository
import javax.inject.Inject

class CartoonRepositoryImpl @Inject constructor(
    private val toonNewsApiService: ToonNewsApiService
) : CartoonRepository {

    override suspend fun getCartoonNews() = toonNewsApiService.getToons().map { it.toDomain() }

    override suspend fun getLikedCartoons(
        lastId: Long,
        size: Int
    ): List<CartoonNews> {
        TODO("Not yet implemented")
    }

    override suspend fun likeCartoon(toonId: Long) {
        TODO("Not yet implemented")
    }

    override suspend fun hateCartoon(toonId: Long) {
        TODO("Not yet implemented")
    }

    override suspend fun cancelLike(toonId: Long) {
        TODO("Not yet implemented")
    }
}