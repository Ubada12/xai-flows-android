/**
 * AuthModels.kt
 * ─────────────────────────────────────────────────────────────────────────────
 * Request/response schemas for the account lifecycle: register -> verify-otp
 * -> login -> refresh -> logout.
 *
 * Mirrors backend/app/models/auth.py field-for-field (same field names,
 * same nullability) — same philosophy the frontend's auth.types.ts already
 * documents: kept intentionally 1:1 rather than remapped to camelCase, so a
 * diff against the backend model is trivial and Gson can map JSON directly
 * without @SerializedName annotations.
 *
 * Refresh token handling: the refresh token is NEVER present in any of these
 * shapes. POST /auth/login sets it as an httpOnly Set-Cookie header, which
 * OkHttp's CookieJar contract intercepts automatically (see AuthCookieJar.kt)
 * — no model class ever holds it directly, mirroring how frontend JS never
 * touches it either.
 */
package com.example.xai_flows.core.data.models

// ─── Register ─────────────────────────────────────────────────────────────────

/** POST /api/v1/auth/register body. Field-length limits are enforced client
 *  side too — see AppConfig.Auth — but the backend is the source of truth. */
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    /** Cloudflare Turnstile challenge token from TurnstileWebView. */
    val turnstile_token: String
)

data class RegisterResponse(
    val message: String,
    val email: String
)

// ─── Verify OTP ───────────────────────────────────────────────────────────────

/** POST /api/v1/auth/verify-otp body. Step 2 of signup — activates the
 *  account created by /register. */
data class VerifyOtpRequest(
    val email: String,
    val otp: String
)

data class VerifyOtpResponse(
    val message: String
)

// ─── Login ────────────────────────────────────────────────────────────────────

/** POST /api/v1/auth/login body. */
data class LoginRequest(
    val email: String,
    val password: String,
    /** Cloudflare Turnstile challenge token from TurnstileWebView. */
    val turnstile_token: String
)

/**
 * Body returned by POST /api/v1/auth/login.
 *
 * Deliberately has no refresh_token field — that half of the token pair
 * arrives as a Set-Cookie header instead, captured by AuthCookieJar, not by
 * this data class.
 */
data class LoginResponse(
    val access_token: String,
    val token_type: String = "bearer"
)

// ─── Refresh ──────────────────────────────────────────────────────────────────

/** Body returned by POST /api/v1/auth/refresh. No request body — the
 *  persisted refresh_token cookie (AuthCookieJar) is what authenticates
 *  this call. */
data class RefreshTokenResponse(
    val access_token: String,
    val token_type: String = "bearer"
)

// ─── Logout ───────────────────────────────────────────────────────────────────

/** Body returned by POST /api/v1/auth/logout. No request body, same as
 *  refresh — the cookie is the credential. */
data class LogoutResponse(
    val message: String
)

// ─── Errors ───────────────────────────────────────────────────────────────────

/**
 * Shape of every error response from the backend's centralized exception
 * handler (see backend/app/core/exception_handlers.py) — always
 * { "detail": "..." }. A 422 validation error instead returns
 * { "detail": [{ "msg": ..., "loc": [...] }, ...] }, so `detail` is typed
 * loosely here and narrowed by AuthErrorParser — mirrors the frontend's
 * ApiErrorBody in features/auth/types/auth.types.ts.
 */
data class ApiErrorBody(
    val detail: Any?
)
