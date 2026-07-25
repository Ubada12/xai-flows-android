/**
 * AuthApiService.kt
 * ─────────────────────────────────────────────────────────────────────────────
 * Retrofit interface for the account lifecycle: register -> verify-otp ->
 * login -> refresh -> logout. Paths come from AppConfig.Auth (config-driven,
 * nothing hardcoded here) and mirror the backend 1:1 — see
 * backend/app/api/v1/endpoints/auth.py.
 *
 * refresh/logout take an explicit empty body (see [EmptyRequestBody]) rather
 * than relying on Kotlin default-parameter values, which are unreliable
 * across Retrofit's dynamic proxy — every call site passes one explicitly.
 * Both are authenticated purely by the persisted refresh_token cookie that
 * AuthCookieJar attaches automatically; no JSON field carries it.
 *
 * Two forms of refresh are exposed on purpose:
 *   - [refresh]         suspend, used by AuthRepository for the app-start
 *                        "silent session restore" (mirrors the frontend's
 *                        bootstrapSession in shared/auth/auth-context.tsx).
 *   - [refreshBlocking]  a classic blocking Call, used only by
 *                        TokenAuthenticator — okhttp3.Authenticator.authenticate()
 *                        is a synchronous callback on an OkHttp dispatcher
 *                        thread, not a suspend function, so it cannot await
 *                        a coroutine; it calls .execute() on this instead.
 */
package com.example.xai_flows.core.data.api

import com.example.xai_flows.core.config.AppConfig
import com.example.xai_flows.core.data.models.LoginRequest
import com.example.xai_flows.core.data.models.LoginResponse
import com.example.xai_flows.core.data.models.LogoutResponse
import com.example.xai_flows.core.data.models.RefreshTokenResponse
import com.example.xai_flows.core.data.models.RegisterRequest
import com.example.xai_flows.core.data.models.RegisterResponse
import com.example.xai_flows.core.data.models.VerifyOtpRequest
import com.example.xai_flows.core.data.models.VerifyOtpResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

/** Empty JSON body ("{}") for the cookie-only endpoints (refresh/logout).
 *  See the file header for why this exists instead of a default argument. */
class EmptyRequestBody

interface AuthApiService {

    /** Step 1 of signup. Verifies the CAPTCHA token, creates/re-issues the
     *  user, emails a 6-digit OTP. Errors: 400 bad CAPTCHA, 409 email
     *  already registered & verified, 429 rate-limited. */
    @POST(AppConfig.Auth.REGISTER_PATH)
    suspend fun register(@Body request: RegisterRequest): RegisterResponse

    /** Step 2 of signup. Activates the account. Errors: 404 user/OTP not
     *  found, 400 expired/incorrect OTP, 429 too many attempts. */
    @POST(AppConfig.Auth.VERIFY_OTP_PATH)
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): VerifyOtpResponse

    /** Verifies CAPTCHA + credentials. Returns the access token in the body;
     *  the refresh token arrives as a Set-Cookie header, captured by
     *  AuthCookieJar. Errors: 400 bad CAPTCHA, 401 invalid credentials,
     *  403 unverified/disabled, 423 locked out, 429 rate-limited. */
    @POST(AppConfig.Auth.LOGIN_PATH)
    suspend fun login(@Body request: LoginRequest): LoginResponse

    /** Silent session restore / manual refresh. Authenticated by the
     *  persisted refresh_token cookie alone. Errors: 401 missing/invalid/
     *  expired cookie, 403 unverified/disabled, 404 user not found. */
    @POST(AppConfig.Auth.REFRESH_PATH)
    suspend fun refresh(@Body body: EmptyRequestBody): RefreshTokenResponse

    /** Blocking twin of [refresh] for TokenAuthenticator's synchronous
     *  401-retry path — see file header. */
    @POST(AppConfig.Auth.REFRESH_PATH)
    fun refreshBlocking(@Body body: EmptyRequestBody): Call<RefreshTokenResponse>

    /** Invalidates the session tied to the refresh_token cookie and clears
     *  it server-side. Errors: 401 missing/invalid/unknown session. */
    @POST(AppConfig.Auth.LOGOUT_PATH)
    suspend fun logout(@Body body: EmptyRequestBody): LogoutResponse
}
