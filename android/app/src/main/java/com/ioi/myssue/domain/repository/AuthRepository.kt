package com.ioi.myssue.domain.repository

interface AuthRepository {

    suspend fun saveUserId(userId: String)
}