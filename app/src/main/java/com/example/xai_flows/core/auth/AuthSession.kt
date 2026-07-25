/**
 * AuthSession.kt
 * ─────────────────────────────────────────────────────────────────────────────
 * Holds the current access token in memory ONLY — never persisted to disk.
 * Same rule the frontend enforces in shared/auth/auth-context.tsx ("React
 * state ONLY — never localStorage"): a short-lived (15 min) Bearer token has
 * no business surviving a process death on disk. Session persistence across
 * app restarts instead comes from AuthCookieJar's persisted refresh_token
 * cookie plus AuthRepository.restoreSession() calling /auth/refresh on
 * launch — mirrors the frontend's bootstrapSession() flow exactly.
 *
 * AuthInterceptor reads [accessToken] on every outgoing request.
 * AuthRepository and TokenAuthenticator are the only two writers.
 * UI observes [state] to decide what to render (AuthGateCard vs. the real
 * screen), the same role useAuth().isAuthenticated plays on the frontend.
 */
package com.example.xai_flows.core.auth

import com.example.xai_flows.core.utils.JwtUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Decoded, display-only claims from the current access token. Never used
 *  for authorization decisions — the backend is the only party that can
 *  actually trust a token; this is purely "what name do we show". */
data class AuthUser(val email: String?, val role: String?)

sealed interface SessionState {
    /** No valid access token. Screens that require auth should show
     *  AuthGateCard / route to LoginScreen in this state. */
    data object LoggedOut : SessionState

    /** A valid (or optimistically-valid — the backend still enforces the
     *  real check) access token is held in memory. */
    data class LoggedIn(val user: AuthUser) : SessionState
}

object AuthSession {

    @Volatile
    var accessToken: String? = null
        private set

    private val _state = MutableStateFlow<SessionState>(SessionState.LoggedOut)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    val isLoggedIn: Boolean get() = accessToken != null

    /** Called after a successful login/refresh with the newly-issued token. */
    fun setAccessToken(token: String) {
        accessToken = token
        val payload = JwtUtils.decodePayload(token)
        _state.value = SessionState.LoggedIn(AuthUser(email = payload?.email, role = payload?.role))
    }

    /** Called on logout, or when a refresh attempt definitively fails
     *  (no valid cookie, expired session, disabled account, etc.) — the
     *  user is unambiguously logged out at that point. */
    fun clear() {
        accessToken = null
        _state.value = SessionState.LoggedOut
    }
}
