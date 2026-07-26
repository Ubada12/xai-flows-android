/**
 * JwtUtils.kt
 * ─────────────────────────────────────────────────────────────────────────────
 * Client-side JWT payload decoder — NOT a verifier. Mirrors the frontend's
 * shared/lib/jwt.ts: reads the base64url-encoded claims out of the access
 * token purely for display (e.g. showing the logged-in user's email). It
 * never checks the signature — the backend is the only party that can
 * actually trust a token; this just borrows its claims to render the UI.
 */
package org.ubada.xaiflows.core.utils

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/** Claims this app cares about. Any other claim present on the token but
 *  not listed here is simply ignored by Gson. */
data class JwtPayload(
    @SerializedName("sub") val subject: String? = null,
    val email: String? = null,
    val role: String? = null,
    /** Standard "exp" claim — seconds since epoch. */
    val exp: Long? = null
)

object JwtUtils {

    /** Decodes the middle (payload) segment of a JWT. Returns null on any
     *  malformed input rather than throwing — a decode failure should
     *  degrade to "no display name", never crash the app. */
    fun decodePayload(token: String): JwtPayload? {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return null
            Gson().fromJson(decodeBase64Url(parts[1]), JwtPayload::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /** True when the token's `exp` claim is in the past, [skewMs] early to
     *  avoid a race where the token expires in the few hundred ms it takes
     *  a request to reach the backend. Returns false (assume valid) if
     *  there's no exp claim or decoding fails — TokenAuthenticator's
     *  401-triggered refresh is the real safety net regardless. */
    fun isExpired(token: String, skewMs: Long): Boolean {
        val exp = decodePayload(token)?.exp ?: return false
        return System.currentTimeMillis() + skewMs >= exp * 1000
    }

    /** Manually re-pads base64url input before decoding — more portable
     *  across API levels than relying on Base64.URL_SAFE's own padding
     *  handling, since JWT segments are unpadded by spec. */
    private fun decodeBase64Url(segment: String): String {
        val standard = segment.replace('-', '+').replace('_', '/')
        val padded = when (standard.length % 4) {
            2 -> "$standard=="
            3 -> "$standard="
            else -> standard
        }
        return String(Base64.decode(padded, Base64.DEFAULT))
    }
}
