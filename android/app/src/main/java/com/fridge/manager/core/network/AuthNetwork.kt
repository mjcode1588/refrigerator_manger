package com.fridge.manager.core.network

import com.fridge.manager.core.datastore.TokenStorage
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Provider

class AuthInterceptor @Inject constructor(
    private val tokenStorage: TokenStorage,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val path = original.url.encodedPath
        if (path.endsWith("/auth/refresh") || path.contains("/auth/social/") || path.endsWith("/auth/dev-login")) {
            return chain.proceed(original)
        }
        val token = runBlocking { tokenStorage.getAccessToken() }
        val request = if (!token.isNullOrBlank()) {
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            original
        }
        return chain.proceed(request)
    }
}

class TokenAuthenticator @Inject constructor(
    private val tokenStorage: TokenStorage,
    private val apiServiceProvider: Provider<ApiService>,
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) return null

        val refreshToken = runBlocking { tokenStorage.getRefreshToken() } ?: return null
        val newTokens = runBlocking {
            val res = apiServiceProvider.get().refreshTokens(RefreshRequest(refreshToken))
            if (res.isSuccessful) res.body() else null
        }

        if (newTokens == null) {
            runBlocking { tokenStorage.clear() }
            return null
        }

        runBlocking {
            tokenStorage.saveTokens(
                accessToken = newTokens.accessToken,
                refreshToken = newTokens.refreshToken,
                nickname = newTokens.user.nickname,
            )
        }

        return response.request.newBuilder()
            .header("Authorization", "Bearer ${newTokens.accessToken}")
            .build()
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
