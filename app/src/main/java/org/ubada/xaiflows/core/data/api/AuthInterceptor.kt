/**
 * AuthInterceptor.kt
 * ─────────────────────────────────────────────────────────────────────────────
 * Attaches an access token as an Authorization header on every outgoing
 * request except the auth-lifecycle endpoints themselves. Mirrors the
 * frontend's request interceptor in shared/lib/api-client.ts, plus one
 * addition the frontend doesn't need: since this app's access token isn't
 * a browser-managed thing, it proactively refreshes a token that's about
 * to expire (AppConfig.Auth.ACCESS_TOKEN_EXPIRY_SKEW_MS) BEFORE sending the
 * request, instead of only reacting to a 401 after the fact. That avoids a
 * guaranteed failed round trip for the common case where we already know
 * locally that the token won't be accepted — TokenAuthenticator's reactive
 * 401 handling remains the fallback for everything this can't predict
 * (server-side revocation, clock skew, etc.).
 *
 * Auth routes (register/verify-otp/login/refresh/logout) are skipped
 * entirely because none of them need a Bearer token: register/verify-otp/
 * login are how a client GETS one, and refresh/logout authenticate via the
 * refresh_token cookie instead (see AuthCookieJar).
 */
package org.ubada.xaiflows.core.data.api

import org.ubada.xaiflows.core.auth.AuthSession
import org.ubada.xaiflows.core.config.AppConfig
import org.ubada.xaiflows.core.utils.JwtUtils
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val authApiService: AuthApiService
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val isAuthRoute = original.url.encodedPath.contains(AppConfig.Auth.AUTH_PATH_SEGMENT)

        if (isAuthRoute) {
            return chain.proceed(original)
        }

        val token = currentOrRefreshedToken()
        if (token == null) {
            return chain.proceed(original)
        }

        val authorized = original.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        return chain.proceed(authorized)
    }

    /** Returns null if logged out. Returns the current token as-is if it's
     *  not close to expiring. If it IS close to expiring, refreshes first
     *  and returns the new one — falling back to the stale token (rather
     *  than null) if that refresh attempt itself fails, so a transient
     *  network hiccup here doesn't block the request outright; if the
     *  token really has expired by the time this reaches the backend,
     *  TokenAuthenticator's reactive path still catches it. */
    private fun currentOrRefreshedToken(): String? {
        val token = AuthSession.accessToken ?: return null
        if (!JwtUtils.isExpired(token, AppConfig.Auth.ACCESS_TOKEN_EXPIRY_SKEW_MS)) {
            return token
        }
        return refreshAccessTokenBlocking(authApiService) ?: token
    }
}
