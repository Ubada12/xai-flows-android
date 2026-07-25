/**
 * AuthUiState.kt
 * ─────────────────────────────────────────────────────────────────────────────
 * One-shot UI state for whichever auth screen is currently active. Kept
 * intentionally separate from AuthSession (core/auth/AuthSession.kt), which
 * is the ongoing "is there a valid session" truth the whole app observes —
 * this is scoped to a single form's request lifecycle (idle/loading/error/
 * success) and resets per screen.
 */
package com.example.xai_flows.ui.auth

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class Error(val message: String) : AuthUiState

    /** Register succeeded — [email] is forwarded to VerifyOtpScreen so the
     *  user doesn't have to retype it. */
    data class RegisterSuccess(val email: String) : AuthUiState

    data object VerifyOtpSuccess : AuthUiState

    /** Login succeeded — AuthSession already holds the new access token by
     *  the time this state is emitted (AuthRepository.login publishes it
     *  before returning). Screens use this purely as a navigation signal. */
    data object LoginSuccess : AuthUiState
}
