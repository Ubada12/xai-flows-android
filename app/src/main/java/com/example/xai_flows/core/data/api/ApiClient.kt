/**
 * ApiClient.kt
 * ─────────────────────────────────────────────────────────────────────────────
 * Singleton Retrofit client for the XAI-FLOWS backend.
 * All tunable values (URL, timeouts) live in AppConfig.Network.
 *
 * Logging is NONE in production. Swap to BODY locally for debugging but
 * never commit BODY — it dumps full image base64 to Logcat.
 *
 * Auth wiring:
 *   - AuthCookieJar persists the refresh_token cookie a browser would hold
 *     automatically — see that file for why a native app needs its own.
 *   - AuthInterceptor attaches the current access token as a Bearer header
 *     on every non-auth request.
 *   - TokenAuthenticator handles a 401 by refreshing once and retrying.
 *
 * [refreshOnlyApiService] exists solely to break a circular dependency:
 * [client]'s Authenticator needs an AuthApiService to call /auth/refresh,
 * but that service is normally built FROM [client] itself. Building it from
 * a second, authenticator-free OkHttpClient (same cookie jar, otherwise
 * plain) breaks the cycle — mirrors the frontend's bare `refreshClient` in
 * shared/lib/api-client.ts, built for the same reason.
 */
package com.example.xai_flows.core.data.api

import com.example.xai_flows.core.config.AppConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.NONE  // NEVER commit BODY here
    }

    // ─── Refresh-only client (no Authenticator — breaks the cycle) ──────────

    private val refreshOnlyClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(AppConfig.Network.CONNECT_TIMEOUT_S, TimeUnit.SECONDS)
            .readTimeout(AppConfig.Network.READ_TIMEOUT_S, TimeUnit.SECONDS)
            .writeTimeout(AppConfig.Network.WRITE_TIMEOUT_S, TimeUnit.SECONDS)
            .cookieJar(AuthCookieJar)
            .addInterceptor(logging)
            .build()
    }

    private val refreshOnlyRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(AppConfig.Network.BASE_URL)
            .client(refreshOnlyClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /** Used by both AuthInterceptor (proactive refresh) and TokenAuthenticator
     *  (reactive refresh) — see file header. Not for general use — call
     *  [authApiService] instead everywhere else. */
    private val refreshOnlyApiService: AuthApiService by lazy {
        refreshOnlyRetrofit.create(AuthApiService::class.java)
    }

    // ─── Main client ──────────────────────────────────────────────────────────

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(AppConfig.Network.CONNECT_TIMEOUT_S, TimeUnit.SECONDS)
            .readTimeout(AppConfig.Network.READ_TIMEOUT_S, TimeUnit.SECONDS)
            .writeTimeout(AppConfig.Network.WRITE_TIMEOUT_S, TimeUnit.SECONDS)
            .cookieJar(AuthCookieJar)
            .addInterceptor(AuthInterceptor(refreshOnlyApiService))
            .addInterceptor(logging)
            .authenticator(TokenAuthenticator(refreshOnlyApiService))
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(AppConfig.Network.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /** Flood-prediction / S3 image routes — both Bearer-protected. */
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    /** Account lifecycle routes (register/verify-otp/login/refresh/logout). */
    val authApiService: AuthApiService by lazy {
        retrofit.create(AuthApiService::class.java)
    }
}
