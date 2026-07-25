/**
 * TokenAuthenticator.kt
 * ─────────────────────────────────────────────────────────────────────────────
 * OkHttp Authenticator — fires only when a request comes back 401. Mirrors
 * the frontend's response interceptor in shared/lib/api-client.ts:
 *
 *   - never retries an auth-route 401 (a bad password on /auth/login must
 *     reach the login form unmodified, not trigger a refresh attempt that
 *     can only ever fail the exact same way),
 *   - never retries more than once per request chain,
 *   - coalesces concurrent 401s behind [lock] so several requests failing
 *     at once trigger a single /auth/refresh call, not one each.
 *
 * Calls [AuthApiService.refreshBlocking] rather than the suspend [AuthApiService.refresh]
 * because Authenticator.authenticate() is a synchronous callback invoked on
 * an OkHttp dispatcher thread — never the main thread, but also never a
 * coroutine — so it cannot await a suspend function.
 *
 * [authApiService] here is deliberately built on a *separate*,
 * authenticator-free OkHttpClient (see ApiClient.kt) purely to break the
 * circular dependency of building a client whose own authenticator needs a
 * Retrofit service built from that same client. It is not a safety
 * mechanism against recursion — the isAuthRoute check above already
 * guarantees this Authenticator never re-triggers itself on the refresh
 * call's own response, regardless of which client made it.
 */
package com.example.xai_flows.core.data.api

import com.example.xai_flows.core.auth.AuthSession
import com.example.xai_flows.core.config.AppConfig
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class TokenAuthenticator(
    private val authApiService: AuthApiService
) : Authenticator {

    /** Guards [refreshSynchronously] so concurrent 401s coalesce into one
     *  network call instead of firing one refresh per failed request. */
    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        val isAuthRoute = response.request.url.encodedPath.contains(AppConfig.Auth.AUTH_PATH_SEGMENT)
        if (isAuthRoute || responseChainLength(response) >= 2) {
            return null
        }

        val newToken = synchronized(lock) {
            // Another thread may have already refreshed while this one
            // waited for the lock. If the session's token has moved on
            // since the failed request was first sent, just reuse it
            // instead of hitting the network again.
            val tokenTriedByFailedRequest = response.request.header("Authorization")
            val currentToken = AuthSession.accessToken
            if (currentToken != null && "Bearer $currentToken" != tokenTriedByFailedRequest) {
                currentToken
            } else {
                refreshSynchronously()
            }
        }

        if (newToken == null) {
            // No valid cookie, expired session, disabled account, etc. —
            // the user is definitively logged out; clear stale state so the
            // UI (observing AuthSession.state) reacts immediately.
            AuthSession.clear()
            return null
        }

        return response.request.newBuilder()
            .header("Authorization", "Bearer $newToken")
            .build()
    }

    /** Delegates to the same blocking refresh AuthInterceptor's proactive
     *  path uses (see TokenRefresher.kt) — one implementation, two callers,
     *  so reactive (here) and proactive refresh can never drift apart. */
    private fun refreshSynchronously(): String? = refreshAccessTokenBlocking(authApiService)

    private fun responseChainLength(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
