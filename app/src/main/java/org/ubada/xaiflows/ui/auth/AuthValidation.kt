/**
 * AuthValidation.kt
 * ─────────────────────────────────────────────────────────────────────────────
 * Client-side field validation mirroring backend/app/models/auth.py's
 * constraints (surfaced here via AppConfig.Auth, never re-hardcoded) —
 * gives instant feedback instead of a round-trip 422. The backend remains
 * the source of truth; this is a UX nicety, not a security boundary.
 */
package org.ubada.xaiflows.ui.auth

import android.util.Patterns
import org.ubada.xaiflows.core.config.AppConfig

object AuthValidation {

    fun isValidEmail(email: String): Boolean =
        email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()

    fun nameError(name: String): String? = when {
        name.length < AppConfig.Auth.NAME_MIN_LENGTH ->
            "Name must be at least ${AppConfig.Auth.NAME_MIN_LENGTH} characters."
        name.length > AppConfig.Auth.NAME_MAX_LENGTH ->
            "Name must be under ${AppConfig.Auth.NAME_MAX_LENGTH} characters."
        else -> null
    }

    /** Used by SignupScreen — the backend hashes and stores under this
     *  same policy (RegisterRequest.password in auth.py). */
    fun registrationPasswordError(password: String): String? = when {
        password.length < AppConfig.Auth.PASSWORD_MIN_LENGTH ->
            "Password must be at least ${AppConfig.Auth.PASSWORD_MIN_LENGTH} characters."
        password.length > AppConfig.Auth.PASSWORD_MAX_LENGTH ->
            "Password must be under ${AppConfig.Auth.PASSWORD_MAX_LENGTH} characters."
        else -> null
    }

    /** Used by LoginScreen — intentionally does NOT enforce the *current*
     *  registration password *minimum*-length policy. Mirrors the
     *  backend's own LoginRequest.password comment: a looser/older policy
     *  must never lock out an existing account just because the policy
     *  changed since they registered. It still matches the backend's
     *  max_length=128 (LoginRequest.password is Field(min_length=1,
     *  max_length=128)), since that bound never changes retroactively —
     *  only the lower bound is deliberately not enforced. */
    fun loginPasswordError(password: String): String? = when {
        password.isEmpty() -> "Password is required."
        password.length > AppConfig.Auth.PASSWORD_MAX_LENGTH ->
            "Password must be under ${AppConfig.Auth.PASSWORD_MAX_LENGTH} characters."
        else -> null
    }

    fun otpError(otp: String): String? =
        if (otp.length != AppConfig.Auth.OTP_LENGTH || !otp.all(Char::isDigit)) {
            "Enter the ${AppConfig.Auth.OTP_LENGTH}-digit code."
        } else null
}
