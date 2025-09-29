package com.ioi.myssue.data.repository

import com.ioi.myssue.data.dto.response.toDomain
import com.ioi.myssue.data.network.api.CartoonApi
import com.ioi.myssue.domain.model.CartoonNews
import com.ioi.myssue.domain.repository.CartoonRepository
import javax.inject.Inject

class CartoonRepositoryImpl @Inject constructor(
    private val cartoonApi: CartoonApi
) : CartoonRepository {

    override suspend fun getCartoonNews() = cartoonApi.getToons().map { it.toDomain() }

    override suspend fun getLikedCartoons(): List<CartoonNews> = cartoonApi.getLikedToons().map { it.toDomain() }

    override suspend fun likeCartoon(toonId: Long) = cartoonApi.likeToon(toonId)

    override suspend fun hateCartoon(toonId: Long) = cartoonApi.hateToon(toonId)

    override suspend fun cancelLike(toonId: Long) = cartoonApi.cancelLikedToon(toonId)
}