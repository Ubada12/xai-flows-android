package com.example.xai_flows.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.xai_flows.core.data.models.GetLatestImageResponse
import com.example.xai_flows.core.data.models.PredictFloodResponse
import com.example.xai_flows.core.data.models.ReverseGeocodeResponse
import com.example.xai_flows.core.data.models.*
import com.google.gson.Gson

object CacheManager {
    private const val PREF_NAME = "FloodCachePrefs"
    private const val KEY_PREDICT_FLOOD = "predict_flood_json"
    private const val KEY_REVERSE_GEO = "reverse_geo_json"
    private const val KEY_LATEST_IMAGE = "latest_image_json"
    private const val KEY_MONITORING = "is_monitoring"

    private lateinit var prefs: SharedPreferences
    private val gson = Gson()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun savePredictFlood(data: PredictFloodResponse) {
        prefs.edit().putString(KEY_PREDICT_FLOOD, gson.toJson(data)).apply()
    }

    fun saveReverseGeo(data: ReverseGeocodeResponse) {
        prefs.edit().putString(KEY_REVERSE_GEO, gson.toJson(data)).apply()
    }

    fun saveLatestImage(data: GetLatestImageResponse) {
        prefs.edit().putString(KEY_LATEST_IMAGE, gson.toJson(data)).apply()
    }

    fun saveMonitoringState(isMonitoring: Boolean) {
        prefs.edit().putBoolean(KEY_MONITORING, isMonitoring).apply()
    }

    fun getPredictFlood(): PredictFloodResponse? {
        return prefs.getString(KEY_PREDICT_FLOOD, null)?.let {
            gson.fromJson(it, PredictFloodResponse::class.java)
        }
    }

    fun getReverseGeo(): ReverseGeocodeResponse? {
        return prefs.getString(KEY_REVERSE_GEO, null)?.let {
            gson.fromJson(it, ReverseGeocodeResponse::class.java)
        }
    }

    fun getLatestImage(): GetLatestImageResponse? {
        return prefs.getString(KEY_LATEST_IMAGE, null)?.let {
            gson.fromJson(it, GetLatestImageResponse::class.java)
        }
    }

    fun isMonitoring(): Boolean {
        return prefs.getBoolean(KEY_MONITORING, false)
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
