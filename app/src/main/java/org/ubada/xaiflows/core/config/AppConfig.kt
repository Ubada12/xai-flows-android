/**
 * AppConfig.kt
 * ─────────────────────────────────────────────────────────────────────────────
 * Central configuration object for XAI-FLOWS Android app.
 *
 * All tunable constants live here so developers (and future you) never have to
 * grep through the codebase to find a magic number buried in a service or
 * composable.
 *
 * HOW TO USE:
 *   import org.ubada.xaiflows.core.config.AppConfig
 *   delay(AppConfig.Monitoring.INTERVAL_MS)
 *
 * ADDING SECRETS:
 *   Do NOT add API keys here. Use local.properties + BuildConfig instead:
 *     1. In local.properties:  MY_API_KEY=abc123
 *     2. In build.gradle.kts:  buildConfigField("String","MY_API_KEY","\"${localProperties["MY_API_KEY"]}\"")
 *     3. Access via:           BuildConfig.MY_API_KEY
 *   AppConfig.Turnstile.SITE_KEY below follows exactly this recipe.
 */
package org.ubada.xaiflows.core.config

import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import org.ubada.xaiflows.BuildConfig

object AppConfig {

    // ─── Network ─────────────────────────────────────────────────────────────

    object Network {
        /** Production backend base URL. Change this if the domain changes. */
        const val BASE_URL = "https://api.ubada.org/"

        /** TCP connect + TLS handshake timeout (seconds). */
        const val CONNECT_TIMEOUT_S = 30L

        /**
         * Response read timeout (seconds).
         * Generous because VGG16 + XGBoost cold-start can take ~15–20 s.
         */
        const val READ_TIMEOUT_S = 60L

        /** Multipart image upload timeout (seconds). */
        const val WRITE_TIMEOUT_S = 30L
    }

    // ─── Real-time monitoring ─────────────────────────────────────────────────

    object Monitoring {
        /**
         * How long (ms) to wait between monitoring cycles.
         * Lowering this increases backend load and S3 costs.
         */
        const val INTERVAL_MS = 20_000L

        /**
         * How long (ms) to wait before stopping the service on error
         * (gives the UI time to show the error notification).
         */
        const val ERROR_STOP_DELAY_MS = 10_000L

        /**
         * Default geographic coordinates used when the service is started
         * without explicit lat/lon Intent extras.
         * Currently: central Mumbai, Maharashtra, India.
         */
        const val DEFAULT_LATITUDE  = 19.0760
        const val DEFAULT_LONGITUDE = 72.8777
    }

    // ─── Notifications ────────────────────────────────────────────────────────

    object Notifications {

        // Channel IDs — must be stable across app updates
        const val CHANNEL_MONITORING  = "monitoring_channel"
        const val CHANNEL_ERROR       = "error_channel"
        const val CHANNEL_FLOOD_ALERT = "flood_alert"

        // Notification IDs
        const val ID_FOREGROUND_SERVICE = 1
        const val ID_ERROR              = 2
        // Flood alert IDs are derived from incidentId.hashCode() (unique per event)

        // Foreground service notification importance (low = no sound, no popup)
        val IMPORTANCE_MONITORING = NotificationManager.IMPORTANCE_LOW

        // Error notification importance (high = pops up as heads-up)
        val IMPORTANCE_ERROR      = NotificationManager.IMPORTANCE_HIGH

        // Flood alert importance (high = heads-up + sound)
        val IMPORTANCE_FLOOD      = NotificationManager.IMPORTANCE_HIGH

        /**
         * Flood alert priority for the NotificationCompat builder.
         * PRIORITY_MAX ensures the notification appears as a full-screen
         * intent on lock screens and shows a heads-up banner.
         */
        val FLOOD_ALERT_PRIORITY  = NotificationCompat.PRIORITY_MAX

        /**
         * Flood alert vibration pattern (ms): wait, on, off, on
         * Format: [delay, vibrate, sleep, vibrate, ...]
         */
        val FLOOD_VIBRATION_PATTERN = longArrayOf(0, 700, 250, 700)

        /**
         * Accent colour for flood alert notifications (deep red).
         * Change this to match your brand alert colour if needed.
         */
        const val FLOOD_ACCENT_COLOR = 0xFFDC2626.toInt()

        /**
         * Risk levels that trigger a push notification.
         * "Low" is intentionally excluded to avoid alert fatigue.
         */
        val ALERT_RISK_LEVELS = setOf("High", "Moderate")

        /**
         * Only "High" risk triggers a full-screen intent (shows even on
         * lock screen without unlocking). "Moderate" gets a heads-up banner.
         */
        const val FULL_SCREEN_RISK_LEVEL = "High"
    }

    // ─── UI / Prediction screen ───────────────────────────────────────────────

    object UI {
        /**
         * Default longitude shown in the coordinate inputs when the user
         * hasn't entered custom coordinates.
         * Matches Monitoring.DEFAULT_LONGITUDE.
         */
        const val DEFAULT_LONGITUDE_STR = "72.8777"

        /**
         * Default latitude shown in the coordinate inputs.
         * Matches Monitoring.DEFAULT_LATITUDE.
         */
        const val DEFAULT_LATITUDE_STR  = "19.0760"

        /**
         * Initial countdown value (seconds) shown in the LiveFeed tile.
         * Should match Monitoring.INTERVAL_MS / 1000.
         */
        const val COUNTDOWN_START = 20
    }

    // ─── Cache ────────────────────────────────────────────────────────────────

    object Cache {
        /** SharedPreferences file name for the flood cache. */
        const val PREF_NAME = "FloodCachePrefs"
    }

    // NOTE: there used to be a Links object here (WEB_BASE_URL +
    // INTERNAL_ROUTES) that FooterSectionComponent fell back to for any
    // footer link without an in-app screen. Every footer page now has a
    // real native screen (ui/screens/site/, routed via
    // core.navigation.AppRoute), so that fallback had nothing left to do
    // and was removed rather than left as unreachable dead config — see
    // AppRoute.kt's doc comment for the bug this used to cause.

    // ─── Auth ─────────────────────────────────────────────────────────────────

    /**
     * Everything the auth feature needs that would otherwise be a magic
     * literal scattered across AuthApiService / AuthRepository / the screens.
     * Mirrors the backend's app/models/auth.py constraints and
     * app/core/config.py cookie/session settings field-for-field, and the
     * frontend's config/api.config.ts AUTH_ENDPOINTS, so all three repos
     * agree on one contract.
     */
    object Auth {
        // Endpoint paths, relative to Network.BASE_URL — same convention as
        // ApiService's existing "api/v1/..." paths (no leading slash).
        const val REGISTER_PATH    = "api/v1/auth/register"
        const val VERIFY_OTP_PATH  = "api/v1/auth/verify-otp"
        const val LOGIN_PATH       = "api/v1/auth/login"
        const val REFRESH_PATH     = "api/v1/auth/refresh"
        const val LOGOUT_PATH      = "api/v1/auth/logout"

        /** Any request whose URL contains this segment is an auth-lifecycle
         *  call (register/verify-otp/login/refresh/logout) and must be
         *  skipped by AuthInterceptor/TokenAuthenticator — a 401 from
         *  /auth/login means "wrong password", not "expired session", and
         *  retrying it via refresh can only ever fail the same way again.
         *  Mirrors the frontend's AUTH_PATH_SEGMENT in shared/lib/api-client.ts. */
        const val AUTH_PATH_SEGMENT = "/auth/"

        // Field-length constraints — mirror backend/app/models/auth.py so the
        // app rejects garbage input locally with the same rules the server
        // will otherwise enforce, giving instant feedback instead of a
        // round-trip 422.
        const val NAME_MIN_LENGTH     = 2
        const val NAME_MAX_LENGTH     = 100
        const val PASSWORD_MIN_LENGTH = 8
        const val PASSWORD_MAX_LENGTH = 128
        const val OTP_LENGTH          = 6

        // Cookie contract — must match the backend's REFRESH_COOKIE_NAME /
        // REFRESH_COOKIE_PATH (app/core/config.py defaults). The mobile app
        // has no browser to hold an httpOnly cookie for it, so AuthCookieJar
        // persists this exact cookie by name instead — see that file for why.
        const val REFRESH_COOKIE_NAME = "refresh_token"
        const val REFRESH_COOKIE_PATH = "/api/v1/auth"

        // Secure, encrypted storage for the persisted refresh-token cookie
        // (core/auth/TokenStore.kt) — deliberately separate from
        // CacheManager's plain SharedPreferences, since this file holds a
        // credential, not disposable UI cache.
        const val SECURE_PREF_NAME = "XaiFlowsSecureAuthPrefs"

        /**
         * Safety margin (ms) subtracted from a decoded access token's `exp`
         * claim before treating it as expired. Avoids a race where the
         * token is still technically valid when checked but expires in the
         * few hundred ms it takes the request to reach the backend.
         */
        const val ACCESS_TOKEN_EXPIRY_SKEW_MS = 5_000L
    }

    // ─── Cloudflare Turnstile (CAPTCHA) ───────────────────────────────────────

    /**
     * Backend requires a Turnstile token on /auth/register and /auth/login
     * (see backend/app/api/v1/endpoints/auth.py) — same requirement the web
     * frontend meets with a visible widget (config/api.config.ts,
     * TURNSTILE_SITE_KEY). There is no native Android Turnstile SDK, so
     * ui/auth/TurnstileWebView.kt renders Cloudflare's JS widget inside a
     * small embedded WebView and bridges the resulting token back to Kotlin.
     */
    object Turnstile {
        /**
         * Public Cloudflare Turnstile site key — safe to embed client-side,
         * it is the public half of the pair (the secret half lives only in
         * the backend's TURNSTILE_SECRET_KEY, never here).
         *
         * Provided via BuildConfig (see app/build.gradle.kts +
         * local.properties) instead of being hardcoded, so swapping between
         * a dev/test key and the real production key never touches source.
         *
         * Falls back to Cloudflare's OWN documented dummy testing key —
         * "1x00000000000000000000AA" ("always passes", works from ANY
         * domain, no allow-list entry needed:
         * https://developers.cloudflare.com/turnstile/troubleshooting/testing/).
         * NOTE: an earlier version of this fallback used the frontend's own
         * real site key under the mistaken assumption it was a domain-agnostic
         * test key — it isn't (real keys look like "0x4AAAAAAA...";
         * Cloudflare's actual dummy keys are always in the "1x.../2x.../3x..."
         * shape above), which is exactly what produced Turnstile error 110200
         * ("domain not authorized") the first time this ran on a device.
         * This dummy key always passes with ZERO real bot protection — it
         * is for local testing ONLY. Before shipping to real users, replace
         * it with a real site key whose Cloudflare dashboard allow-list
         * includes WIDGET_URL's hostname.
         */
        val SITE_KEY: String =
            BuildConfig.TURNSTILE_SITE_KEY.ifBlank { "1x00000000000000000000AA" }

        /**
         * Real, hosted page the WebView navigates to for the Turnstile
         * challenge — NOT a local asset. This is the load-bearing fix for
         * Turnstile error 110200 ("domain not authorized"): Cloudflare
         * checks the challenge's origin against an allow-list of real
         * hostnames in the dashboard, and a native app's WebView has no
         * hosted origin of its own to offer. Cloudflare's own documented
         * mobile pattern (developers.cloudflare.com/turnstile/get-started/
         * mobile-implementation/) is to point the WebView at a real page
         * on a domain you control instead of faking one — an earlier
         * version of this app used loadDataWithBaseURL with a synthetic,
         * non-resolving "https://app.xaiflows.local/" origin, which is
         * NOT what Cloudflare's hostname allow-list validation expects
         * (it wants a real FQDN, not a made-up .local one) and is not the
         * officially sanctioned approach even where it happens to work.
         *
         * Points at streamlit-frontend's public/mobile-turnstile.html,
         * deployed on Vercel behind app.ubada.org — see that file for the
         * widget page itself. TurnstileWebView.kt calls webView.loadUrl()
         * on this exact string (with ?sitekey=... appended), it is never
         * combined with a local asset.
         *
         * Provided via BuildConfig (see app/build.gradle.kts +
         * local.properties), same recipe as SITE_KEY above — override
         * TURNSTILE_WIDGET_URL in local.properties to test against a
         * different deployment (e.g. a Vercel preview URL) without
         * touching source.
         */
        val WIDGET_URL: String = BuildConfig.TURNSTILE_WIDGET_URL

        /** How long to wait for the widget to produce a token before
         *  surfacing a timeout error to the user (ms). */
        const val CHALLENGE_TIMEOUT_MS = 15_000L
    }
}
