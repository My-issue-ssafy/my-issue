package com.ioi.myssue.data.network

import android.util.Log
import com.ioi.myssue.data.datastore.AuthDataStore
import com.ioi.myssue.data.network.api.AuthApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import kotlin.text.removePrefix

class AuthManager @Inject constructor(
    private val authDataStore: AuthDataStore,
    private val authApi: AuthApi,
    applicationScope: CoroutineScope
) {
    @Volatile
    private var cachedAccessToken: String? = null

    @Volatile
    private var cachedUserId: Long? = null

    @Volatile
    private var cachedUUID: String? = null

    init {
        applicationScope.launch {
            launch {
                authDataStore.accessTokenFlow.collect { token ->
                    Log.d("Init AuthManager access token", "$token")
                    cachedAccessToken = token
                }
            }

            launch {
                authDataStore.userIdFlow.collect { id ->
                    Log.d("Init AuthManager user id", "$id")
                    cachedUserId = id
                }
            }

            launch {
                authDataStore.uuidFlow.collect { id ->
                    Log.d("Init AuthManager user id", "$id")
                    cachedUUID = id

                    if (id == null) {
                        val newId = UUID.randomUUID().toString()
                        Log.d("AuthManager", "Generated new UUID: $newId")
                        saveUUID(newId)

                        val response = authApi.addUser(newId)
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
                        }
                    }
                }
            }
        }
    }

    fun getAccessToken(): String? = cachedAccessToken

    fun getUserId(): Long? = cachedUserId

    fun getUUID(): String? = cachedUUID

    fun saveToken(accessToken: String) {
        Log.d("AuthManager", "save token")
        cachedAccessToken = accessToken

        CoroutineScope(Dispatchers.IO).launch {
            authDataStore.saveAccessToken(accessToken)
        }
    }

    fun saveUserId(userId: Long) {
        Log.d("AuthManager", "save userId")
        cachedUserId = userId

        CoroutineScope(Dispatchers.IO).launch {
            authDataStore.saveUserId(userId)
        }
    }

    fun saveUUID(uuid: String) {
        Log.d("AuthManager", "save userInfo")
        cachedUUID = uuid

        CoroutineScope(Dispatchers.IO).launch {
            authDataStore.saveUUID(uuid)
        }
    }
}
