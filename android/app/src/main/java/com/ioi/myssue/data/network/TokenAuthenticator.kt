package com.ioi.myssue.data.network

import android.util.Log
import com.ioi.myssue.data.network.api.AuthApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject

class TokenAuthenticator @Inject constructor(
    private val authManager: AuthManager,
    private val authApi: AuthApi
) : Authenticator {

    private val mutex = Mutex()
    private val maxRetryCount = 3

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= maxRetryCount) {
            Log.w("TokenAuthenticator", "최대 재시도 횟수 초과")
            return null
        }

        return runBlocking {
            val access = authManager.getAccessToken()
            if (access.isNullOrBlank()) {
                Log.w("TokenAuthenticator", "토큰 없음")
                return@runBlocking null
            }

            // refresh 수행 (동시에 여러 요청이 들어오면 mutex로 보호)
            mutex.withLock {
                val result = performTokenRefresh()
                when (result) {
                    is RefreshResult.Success -> {
                        authManager.saveToken(result.accessToken)
                        createRequestWithToken(response.request, result.accessToken)
                    }
                    is RefreshResult.Failure -> {
                        Log.w("TokenAuthenticator", "토큰 갱신 실패: ${result.reason}")
                        null
                    }
                }
            }
        }
    }
    private suspend fun performTokenRefresh(): RefreshResult {
        return try {
            val response = authApi.reissueToken()

            if (response.isSuccessful) {
                val accessToken = response.headers()["Authorization"]?.removePrefix("Bearer ")?.trim()

                return if (!accessToken.isNullOrBlank()) {
                    RefreshResult.Success(accessToken)
                } else {
                    RefreshResult.Failure("헤더에 토큰 없음")
                }
            } else {
                RefreshResult.Failure("HTTP 오류: ${response.code()}")
            }
        } catch (e: Exception) {
            RefreshResult.Failure("네트워크 오류: ${e.message}")
        }
    }


    private fun createRequestWithToken(originalRequest: Request, token: String): Request =
        originalRequest.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

    private fun responseCount(response: Response): Int {
        var result = 1
        var priorResponse = response.priorResponse
        while (priorResponse != null) {
            result++
            priorResponse = priorResponse.priorResponse
        }
        return result
    }

    private sealed class RefreshResult {
        data class Success(val accessToken: String) : RefreshResult()
        data class Failure(val reason: String) : RefreshResult()
    }
}

