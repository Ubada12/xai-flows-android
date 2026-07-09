/**
 * ApiClient.kt
 * ─────────────────────────────────────────────────────────────────────────────
 * Singleton Retrofit client for the XAI-FLOWS backend.
 * All tunable values (URL, timeouts) live in AppConfig.Network.
 *
 * Logging is NONE in production. Swap to BODY locally for debugging but
 * never commit BODY — it dumps full image base64 to Logcat.
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

    private val client = OkHttpClient.Builder()
        .connectTimeout(AppConfig.Network.CONNECT_TIMEOUT_S, TimeUnit.SECONDS)
        .readTimeout(AppConfig.Network.READ_TIMEOUT_S,       TimeUnit.SECONDS)
        .writeTimeout(AppConfig.Network.WRITE_TIMEOUT_S,     TimeUnit.SECONDS)
        .addInterceptor(logging)
        .build()

    /** Lazily-constructed Retrofit service. Thread-safe. */
    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(AppConfig.Network.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
