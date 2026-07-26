/**
 * AuthErrorParser.kt
 * ─────────────────────────────────────────────────────────────────────────────
 * Turns a failed auth API call into a display-ready message. Line-for-line
 * port of the frontend's features/auth/lib/parse-auth-error.ts — same
 * status-code defaults, same Retry-After-to-human-countdown formatting,
 * same handling of pydantic's 422 validation-error array — so the mobile
 * and web apps show a user the same message for the same failure.
 */
package org.ubada.xaiflows.core.utils

import org.ubada.xaiflows.core.data.models.ApiErrorBody
import com.google.gson.Gson
import retrofit2.HttpException
import java.io.IOException

data class AuthErrorInfo(
    val message: String,
    /** Null when the request never reached the server (no connectivity). */
    val status: Int?,
    /** Seconds until retrying might succeed (from a 423/429's Retry-After
     *  header). Null when not applicable. */
    val retryAfterSeconds: Long?
)

object AuthErrorParser {

    private val gson = Gson()

    /** Entry point — call this from a catch block around any AuthRepository
     *  call. Never throws itself. */
    fun parse(throwable: Throwable): AuthErrorInfo = when (throwable) {
        is HttpException -> parseHttpException(throwable)
        is IOException -> AuthErrorInfo(defaultMessageForStatus(null), null, null)
        else -> AuthErrorInfo("Something went wrong. Please try again.", null, null)
    }

    private fun parseHttpException(exception: HttpException): AuthErrorInfo {
        val status = exception.code()
        val retryAfterSeconds = exception.response()
            ?.headers()
            ?.get("Retry-After")
            ?.toLongOrNull()

        val detail = extractDetail(exception)

        var message = when (detail) {
            is String -> detail.ifBlank { defaultMessageForStatus(status) }
            is List<*> -> summarizeValidationErrors(detail) ?: defaultMessageForStatus(status)
            else -> defaultMessageForStatus(status)
        }

        if (status == 423 || status == 429) {
            message += formatRetrySuffix(retryAfterSeconds)
        }

        return AuthErrorInfo(message, status, retryAfterSeconds)
    }

    /** Reads and parses the { "detail": ... } error body the backend's
     *  centralized exception handler always returns. Swallows any parse
     *  failure — a malformed/empty body just falls back to the generic
     *  per-status message rather than crashing the error path itself. */
    private fun extractDetail(exception: HttpException): Any? {
        return try {
            val bodyString = exception.response()?.errorBody()?.string() ?: return null
            gson.fromJson(bodyString, ApiErrorBody::class.java)?.detail
        } catch (e: Exception) {
            null
        }
    }

    /** Pydantic's 422 body is [{ msg, loc, type, ... }, ...]. Gson decodes
     *  each entry as a raw Map when the target type is Any. */
    @Suppress("UNCHECKED_CAST")
    private fun summarizeValidationErrors(detail: List<*>): String? {
        val first = detail.firstOrNull() as? Map<String, Any?> ?: return null
        val msg = first["msg"] as? String
        val loc = first["loc"] as? List<*>
        val fieldLabel = loc?.lastOrNull()?.toString()?.let { "$it: " } ?: ""
        return "$fieldLabel${msg ?: "Invalid input."}"
    }

    private fun formatRetrySuffix(seconds: Long?): String {
        if (seconds == null) return ""
        return if (seconds < 60) {
            " Try again in ${seconds}s."
        } else {
            val minutes = (seconds + 59) / 60 // ceil
            " Try again in $minutes minute${if (minutes == 1L) "" else "s"}."
        }
    }

    private fun defaultMessageForStatus(status: Int?): String = when (status) {
        400 -> "Verification failed. Please try again."
        401 -> "Invalid credentials."
        403 -> "This account can't sign in right now."
        404 -> "We couldn't find that account or code."
        409 -> "That email is already registered."
        423 -> "This account is temporarily locked after too many failed attempts."
        429 -> "Too many attempts. Please slow down."
        null -> "Can't reach the server. Check your connection and try again."
        else -> "Something went wrong. Please try again."
    }
}
