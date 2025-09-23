package com.ioi.myssue.data.network

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.ioi.myssue.data.datastore.AuthDataStore
import com.ioi.myssue.data.dto.request.AddUserRequest
import com.ioi.myssue.data.network.api.AuthApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import java.util.UUID
import javax.inject.Singleton

@Singleton
class AuthManager @Inject constructor(
    private val authDataStore: AuthDataStore,
    private val authApi: AuthApi,
    private val applicationScope: CoroutineScope
) {
    @Volatile
    private var cachedAccessToken: String? = null

    @Volatile
    private var cachedUserId: Long? = null

    @Volatile
    private var cachedUUID: String? = null

    init {
        applicationScope.launch {
            // 캐시 업데이트
            launch {
                authDataStore.accessTokenFlow.collect { token ->
                    cachedAccessToken = token
                    Log.d("AuthManager", "Init access token: $token")
                }
            }

            launch {
                authDataStore.userIdFlow.collect { id ->
                    cachedUserId = id
                    Log.d("AuthManager", "Init userId: $id")
                }
            }

            launch {
                authDataStore.uuidFlow.collect { id ->
                    cachedUUID = id
                    Log.d("AuthManager", "Init uuid: $id")
                }
            }
        }
    }

    suspend fun registerNewDeviceIfNeeded(): Boolean {
        val existingUserId = authDataStore.userIdFlow.first()
        if (existingUserId != null) {
            Log.d("AuthManager", "Device already registered: $existingUserId")
            return true
        }

        // fcm 토큰 생성
        val token = FirebaseMessaging.getInstance().token.await()
        Log.d("AuthManager", "Generated FcmToken: $token")

        val newId = UUID.randomUUID().toString()
        Log.d("AuthManager", "Generated new UUID: $newId")
        saveUUID(newId)

        return try {
            val response = authApi.addUser(
                AddUserRequest(newId, token)
            )
            if (response.isSuccessful) {
                val accessToken =
                    response.headers()["Authorization"]?.removePrefix("Bearer ")?.trim()
                accessToken?.let {
                    Log.d("AuthManager", "Received access token from server")
                    saveToken(it)
                }
                response.body()?.let {
                    saveUserId(it.userId)
                }
                true
            } else {
                Log.e("AuthManager", "Failed to add user: ${response.code()}")
                false
            }
        } catch (e: Exception) {
            Log.e("AuthManager", "Device registration error", e)
            false
        }
    }

    fun getAccessToken(): String? = cachedAccessToken
    fun getUserId(): Long? = cachedUserId
    fun getUUID(): String? = cachedUUID

    fun saveToken(accessToken: String) {
        cachedAccessToken = accessToken
        applicationScope.launch(Dispatchers.IO) {
            authDataStore.saveAccessToken(accessToken)
        }
    }

    fun saveUserId(userId: Long) {
        cachedUserId = userId
        applicationScope.launch(Dispatchers.IO) {
            authDataStore.saveUserId(userId)
        }
    }

    fun saveUUID(uuid: String) {
        cachedUUID = uuid
        applicationScope.launch(Dispatchers.IO) {
            authDataStore.saveUUID(uuid)
        }
    }
}
