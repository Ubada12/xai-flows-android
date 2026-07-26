/**
 * TokenStore.kt
 * ─────────────────────────────────────────────────────────────────────────────
 * Encrypted, on-disk storage for exactly one thing: the persisted
 * refresh_token cookie value (+ its domain/expiry) that AuthCookieJar reads
 * and writes. This is deliberately separate from core/cache/CacheManager.kt
 * (plain SharedPreferences) — that file caches disposable API responses;
 * this one holds a long-lived credential and is encrypted at rest via
 * Jetpack Security's EncryptedSharedPreferences (AES-256).
 *
 * The short-lived access token is NEVER written here — it lives only in
 * memory (see core/auth/AuthSession.kt), same rule the frontend follows for
 * the same reason: a 15-minute Bearer token has no business surviving a
 * process restart on disk.
 *
 * Usage:
 *   TokenStore.init(context)   // call once, e.g. MainActivity.onCreate()
 */
package org.ubada.xaiflows.core.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.ubada.xaiflows.core.config.AppConfig

object TokenStore {

    private const val KEY_COOKIE_VALUE      = "refresh_cookie_value"
    private const val KEY_COOKIE_DOMAIN     = "refresh_cookie_domain"
    private const val KEY_COOKIE_EXPIRES_AT = "refresh_cookie_expires_at_ms"

    private lateinit var prefs: SharedPreferences

    /** Must be called before any other TokenStore method — same contract as
     *  CacheManager.init(). Safe to call more than once.
     *
     *  Deliberately swallows any Keystore/crypto failure (EncryptedSharedPreferences.create
     *  declares checked GeneralSecurityException/IOException) rather than
     *  letting it crash onCreate(): every other getter here already checks
     *  ::prefs.isInitialized and degrades to "no persisted session" if
     *  init failed, which is far better than the whole app — including the
     *  unauthenticated HomePage — refusing to launch over a device-specific
     *  Keystore quirk. */
    fun init(context: Context) {
        if (::prefs.isInitialized) return
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            prefs = EncryptedSharedPreferences.create(
                context,
                AppConfig.Auth.SECURE_PREF_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Falls through with ::prefs left uninitialized — see doc above.
        }
    }

    /** Persists the refresh_token cookie exactly as OkHttp parsed it from
     *  the backend's Set-Cookie header. Called only by AuthCookieJar. */
    fun saveRefreshCookie(value: String, domain: String, expiresAtEpochMs: Long) {
        if (!::prefs.isInitialized) return
        prefs.edit()
            .putString(KEY_COOKIE_VALUE, value)
            .putString(KEY_COOKIE_DOMAIN, domain)
            .putLong(KEY_COOKIE_EXPIRES_AT, expiresAtEpochMs)
            .apply()
    }

    fun getRefreshCookieValue(): String? =
        if (::prefs.isInitialized) prefs.getString(KEY_COOKIE_VALUE, null) else null

    fun getRefreshCookieDomain(): String? =
        if (::prefs.isInitialized) prefs.getString(KEY_COOKIE_DOMAIN, null) else null

    /** Epoch millis the cookie expires at, or Long.MAX_VALUE if unknown
     *  (treated as "not expired" — the backend's own session lookup is the
     *  real source of truth; this is just an optimization to avoid sending
     *  a cookie we already know is stale). */
    fun getRefreshCookieExpiresAt(): Long =
        if (::prefs.isInitialized) prefs.getLong(KEY_COOKIE_EXPIRES_AT, Long.MAX_VALUE) else Long.MAX_VALUE

    /** Wipes the persisted cookie — called on logout and on a definitively
     *  failed refresh (expired/invalid/unknown session). */
    fun clear() {
        if (!::prefs.isInitialized) return
        prefs.edit().clear().apply()
    }
}
