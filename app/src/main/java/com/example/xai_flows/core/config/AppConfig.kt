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
 *   import com.example.xai_flows.core.config.AppConfig
 *   delay(AppConfig.Monitoring.INTERVAL_MS)
 *
 * ADDING SECRETS:
 *   Do NOT add API keys here. Use local.properties + BuildConfig instead:
 *     1. In local.properties:  MY_API_KEY=abc123
 *     2. In build.gradle.kts:  buildConfigField("String","MY_API_KEY","\"${localProperties["MY_API_KEY"]}\"")
 *     3. Access via:           BuildConfig.MY_API_KEY
 */
package com.example.xai_flows.core.config

import android.app.NotificationManager
import androidx.core.app.NotificationCompat

object AppConfig {

    // ─── Network ─────────────────────────────────────────────────────────────

    object Network {
        /** Production backend base URL. Change this if the domain changes. */
        const val BASE_URL = "https://api.ubada.shop/"

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
    // ─── External Links ──────────────────────────────────────────────────────

    object Links {
        /**
         * Vercel-hosted web frontend URL.
         * Used by footer section links that have no in-app screen equivalent.
         */
        const val WEB_BASE_URL = "https://streamlit-frontend.vercel.app"

        /**
         * Routes that are handled by in-app navigation (via onNavigate callback)
         * instead of opening the browser.
         * All other hrefs are opened as [WEB_BASE_URL] + href.
         */
        val INTERNAL_ROUTES = setOf("/", "/predictions", "/analytics")
    }
}
