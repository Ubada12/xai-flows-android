/**
 * AuthCookieJar.kt
 * ─────────────────────────────────────────────────────────────────────────────
 * Minimal, single-purpose OkHttp CookieJar. A browser holds the backend's
 * httpOnly refresh_token cookie automatically; a native app has no such
 * mechanism by default (OkHttp drops every cookie unless you supply a
 * CookieJar), so this exists purely to persist that ONE cookie, by the
 * exact name the backend uses (AppConfig.Auth.REFRESH_COOKIE_NAME), and
 * attach it back on requests to the path it's scoped to
 * (AppConfig.Auth.REFRESH_COOKIE_PATH). Every other cookie the backend
 * might ever set is ignored on purpose — this is not a general-purpose jar.
 *
 * Persistence is delegated to TokenStore (EncryptedSharedPreferences), so
 * the value survives process death/app restart, which is what lets
 * AuthRepository.restoreSession() silently re-authenticate on app launch
 * exactly like the frontend's bootstrapSession() does with the browser's
 * cookie.
 */
package com.example.xai_flows.core.data.api

import com.example.xai_flows.core.auth.TokenStore
import com.example.xai_flows.core.config.AppConfig
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

object AuthCookieJar : CookieJar {

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val refreshCookie = cookies.firstOrNull { it.name == AppConfig.Auth.REFRESH_COOKIE_NAME }
            ?: return

        TokenStore.saveRefreshCookie(
            value = refreshCookie.value,
            domain = refreshCookie.domain,
            expiresAtEpochMs = refreshCookie.expiresAt
        )
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        // Only attach it on requests under the path the backend scoped it
        // to server-side — mirrors the browser's own cookie-path scoping,
        // so it's never sent on /predict-flood/, /get-latest-s3-image/, etc.
        if (!url.encodedPath.startsWith(AppConfig.Auth.REFRESH_COOKIE_PATH)) {
            return emptyList()
        }

        val value = TokenStore.getRefreshCookieValue() ?: return emptyList()
        val domain = TokenStore.getRefreshCookieDomain() ?: url.host
        val expiresAt = TokenStore.getRefreshCookieExpiresAt()

        // Already known-expired — stop sending it. The backend would 401
        // anyway, but there's no reason to make that round trip, and this
        // also self-heals a stale entry without needing an explicit logout.
        val now = System.currentTimeMillis()
        if (expiresAt in 1 until now) {
            TokenStore.clear()
            return emptyList()
        }

        val cookie = Cookie.Builder()
            .name(AppConfig.Auth.REFRESH_COOKIE_NAME)
            .value(value)
            .domain(domain)
            .path(AppConfig.Auth.REFRESH_COOKIE_PATH)
            .build()

        return listOf(cookie)
    }
}
