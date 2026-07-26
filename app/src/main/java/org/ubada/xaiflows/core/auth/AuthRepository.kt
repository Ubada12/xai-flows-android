/**
 * AuthRepository.kt
 * ─────────────────────────────────────────────────────────────────────────────
 * Orchestrates the account lifecycle for the UI layer (AuthViewModel),
 * mirroring the split of responsibilities in the frontend between
 * features/auth/api/auth.api.ts and shared/auth/auth-context.tsx:
 *
 *   - register / verifyOtp are thin pass-throughs to the backend.
 *   - login calls the backend and immediately publishes the new access
 *     token into AuthSession — callers don't do that bookkeeping themselves.
 *   - restoreSession() is the mobile equivalent of the frontend's
 *     bootstrapSession(): called once on app launch, it silently calls
 *     /auth/refresh. If the persisted refresh_token cookie (AuthCookieJar /
 *     TokenStore) is still valid, the user is re-authenticated with no
 *     visible "logged out" flash. A failure here (401, no cookie, network
 *     error) is an ordinary, expected outcome — not an error to surface to
 *     the user — so it's swallowed and reported as a plain boolean.
 *   - logout() calls the backend best-effort and always clears local state
 *     regardless of whether that call succeeds — the user's intent
 *     ("log me out") is satisfied client-side either way.
 */
package org.ubada.xaiflows.core.auth

import org.ubada.xaiflows.core.cache.CacheManager
import org.ubada.xaiflows.core.data.api.ApiClient
import org.ubada.xaiflows.core.data.api.EmptyRequestBody
import org.ubada.xaiflows.core.data.models.LoginRequest
import org.ubada.xaiflows.core.data.models.RegisterRequest
import org.ubada.xaiflows.core.data.models.RegisterResponse
import org.ubada.xaiflows.core.data.models.VerifyOtpRequest
import org.ubada.xaiflows.core.data.models.VerifyOtpResponse
import retrofit2.HttpException

object AuthRepository {

    private val authApi = ApiClient.authApiService

    /** Step 1 of signup. Throws on failure — callers should catch and pass
     *  the throwable through AuthErrorParser.parse() for a display message. */
    suspend fun register(
        name: String,
        email: String,
        password: String,
        turnstileToken: String
    ): RegisterResponse = authApi.register(
        RegisterRequest(name = name, email = email, password = password, turnstile_token = turnstileToken)
    )

    /** Step 2 of signup — activates the account. */
    suspend fun verifyOtp(email: String, otp: String): VerifyOtpResponse =
        authApi.verifyOtp(VerifyOtpRequest(email = email, otp = otp))

    /** On success, publishes the new access token into AuthSession so every
     *  screen observing AuthSession.state updates immediately. */
    suspend fun login(email: String, password: String, turnstileToken: String) {
        val response = authApi.login(
            LoginRequest(email = email, password = password, turnstile_token = turnstileToken)
        )
        AuthSession.setAccessToken(response.access_token)
    }

    /** Silent session restore for app launch — see class docs.
     *  @return true if a session was restored, false if the user is simply
     *  logged out (expected, not an error). */
    suspend fun restoreSession(): Boolean {
        return try {
            val response = authApi.refresh(EmptyRequestBody())
            AuthSession.setAccessToken(response.access_token)
            true
        } catch (e: HttpException) {
            // The server explicitly rejected the persisted refresh cookie
            // (401 no/invalid/expired cookie, 403 unverified/disabled
            // account, 404 user not found) — this really is "logged out",
            // not a network hiccup. Wipe the persisted cookie and cached
            // predictions too, the same as an explicit logout, so the next
            // person to use this device never sees the previous user's data.
            AuthSession.clear()
            TokenStore.clear()
            CacheManager.clearAll()
            false
        } catch (e: Exception) {
            // No server response at all — no connectivity, DNS failure,
            // timeout, etc. The session might still be perfectly valid;
            // we simply couldn't confirm it right now. Leave the persisted
            // cookie and cache alone so the next attempt (next launch, or
            // once connectivity returns) can still succeed — only the
            // in-memory access token is cleared for this run.
            AuthSession.clear()
            false
        }
    }

    suspend fun logout() {
        try {
            authApi.logout(EmptyRequestBody())
        } catch (e: Exception) {
            // Session already invalid, network hiccup, etc. — the user's
            // intent is satisfied client-side regardless, see class docs.
        } finally {
            AuthSession.clear()
            TokenStore.clear()
            // CacheManager holds the last predict-flood/S3-image response —
            // account-specific data that must not leak to whoever logs into
            // this device next.
            CacheManager.clearAll()
        }
    }
}
