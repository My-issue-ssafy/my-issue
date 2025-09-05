package com.ioi.myssue.data.repository

import com.ioi.myssue.data.network.api.AuthApi
import com.ioi.myssue.domain.repository.AuthRepository
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi
) : AuthRepository {

    override suspend fun saveUserId(userId: String) = authApi.addUser()
}