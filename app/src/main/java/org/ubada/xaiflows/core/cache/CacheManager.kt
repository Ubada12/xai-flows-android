/**
 * CacheManager.kt
 * ─────────────────────────────────────────────────────────────────────────────
 * Lightweight persistence layer using SharedPreferences + Gson.
 *
 * Caches API responses so the UI can show stale data immediately on app
 * relaunch while a fresh fetch runs in the background.
 *
 * Usage:
 *   CacheManager.init(context)   // call once in Application.onCreate()
 *   CacheManager.savePredictFlood(response)
 *   val cached = CacheManager.getPredictFlood()
 *
 * Thread safety: SharedPreferences.edit().apply() is async and thread-safe.
 * Reads happen on the calling thread; call from a background coroutine if
 * the deserialization cost matters.
 */
package org.ubada.xaiflows.core.cache

import org.ubada.xaiflows.core.config.AppConfig

import android.content.Context
import android.content.SharedPreferences
import org.ubada.xaiflows.core.data.models.GetLatestImageResponse
import org.ubada.xaiflows.core.data.models.PredictFloodResponse
import com.google.gson.Gson

object CacheManager {

    private val PREF_NAME = AppConfig.Cache.PREF_NAME

    // SharedPreferences keys
    private const val KEY_PREDICT_FLOOD = "predict_flood_json"
    private const val KEY_LATEST_IMAGE  = "latest_image_json"
    private const val KEY_MONITORING    = "is_monitoring"

    private lateinit var prefs: SharedPreferences
    private val gson = Gson()

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    /**
     * Must be called before any other CacheManager method.
     * Recommended place: Application.onCreate() or MainActivity.onCreate().
     */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    // ─── Save ─────────────────────────────────────────────────────────────────

    /** Persist the latest flood prediction result. */
    fun savePredictFlood(data: PredictFloodResponse) {
        prefs.edit().putString(KEY_PREDICT_FLOOD, gson.toJson(data)).apply()
    }

    /** Persist the latest S3 drain image (base64). */
    fun saveLatestImage(data: GetLatestImageResponse) {
        prefs.edit().putString(KEY_LATEST_IMAGE, gson.toJson(data)).apply()
    }

    /** Persist real-time monitoring running state. */
    fun saveMonitoringState(isMonitoring: Boolean) {
        prefs.edit().putBoolean(KEY_MONITORING, isMonitoring).apply()
    }

    // ─── Load ─────────────────────────────────────────────────────────────────

    /** Returns the last cached flood prediction, or null if none. */
    fun getPredictFlood(): PredictFloodResponse? {
        return prefs.getString(KEY_PREDICT_FLOOD, null)
            ?.let { gson.fromJson(it, PredictFloodResponse::class.java) }
    }

    /** Returns the last cached S3 image, or null if none. */
    fun getLatestImage(): GetLatestImageResponse? {
        return prefs.getString(KEY_LATEST_IMAGE, null)
            ?.let { gson.fromJson(it, GetLatestImageResponse::class.java) }
    }

    /** Returns whether real-time monitoring was active at last app exit. */
    fun isMonitoring(): Boolean = prefs.getBoolean(KEY_MONITORING, false)

    // ─── Clear ────────────────────────────────────────────────────────────────

    /** Wipe all cached data (called on mode toggle or manual reset). */
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
