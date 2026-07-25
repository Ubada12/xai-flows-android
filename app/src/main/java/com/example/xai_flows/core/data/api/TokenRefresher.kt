/**
 * TokenRefresher.kt
 * ─────────────────────────────────────────────────────────────────────────────
 * Single blocking implementation of "call /auth/refresh and, on success,
 * publish the new token into AuthSession" — shared by AuthInterceptor
 * (proactive refresh just before a token is about to expire, per
 * AppConfig.Auth.ACCESS_TOKEN_EXPIRY_SKEW_MS) and TokenAuthenticator
 * (reactive refresh after an actual 401). Kept as one function so those two
 * call sites can never drift into subtly different refresh behavior.
 *
 * Blocking is safe in both callers: OkHttp invokes Interceptor.intercept()
 * and Authenticator.authenticate() on a background dispatcher thread,
 * never the main thread.
 */
package com.example.xai_flows.core.data.api

import com.example.xai_flows.core.auth.AuthSession

internal fun refreshAccessTokenBlocking(authApiService: AuthApiService): String? {
    return try {
        val result = authApiService.refreshBlocking(EmptyRequestBody()).execute()
        val token = if (result.isSuccessful) result.body()?.access_token else null
        if (token != null) AuthSession.setAccessToken(token)
        token
    } catch (e: Exception) {
        null
    }
}
