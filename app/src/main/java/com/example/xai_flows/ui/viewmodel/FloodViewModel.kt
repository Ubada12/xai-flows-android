/**
 * FloodViewModel.kt
 * ─────────────────────────────────────────────────────────────────────────────
 * ViewModel for the Predictions screen.
 *
 * Exposes three StateFlows:
 *   - latestImageState   : latest S3 drain image (base64)
 *   - predictFloodState  : full flood prediction + location + weather + SHAP
 *   - countdown / isRefreshing : UI helpers for the 20 s real-time cycle
 *
 * Reverse-geocode state has been removed — the backend now returns
 * location (city + address) inside PredictFloodResponse.location.
 *
 * Cache layer:
 *   On init, restoreFromCache() re-populates states from SharedPreferences so
 *   the UI shows the last known result immediately without waiting for the
 *   network.
 */
package com.example.xai_flows.ui.viewmodel

import android.util.Log
import com.example.xai_flows.core.config.AppConfig
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xai_flows.core.cache.CacheManager
import com.example.xai_flows.core.data.api.ApiClient
import com.example.xai_flows.core.data.models.GetLatestImageResponse
import com.example.xai_flows.core.data.models.PredictFloodRequest
import com.example.xai_flows.core.data.models.PredictFloodResponse
import com.example.xai_flows.core.data.repository.FloodRepository
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MultipartBody

// ─── Sealed result wrapper ────────────────────────────────────────────────────

/**
 * Generic API result state used for all network calls.
 * Idle   → no request made yet (initial / after reset)
 * Loading → request in flight
 * Success → request completed with data
 * Error  → request failed with a message
 */
sealed class ApiState<out T> {
    object Idle    : ApiState<Nothing>()
    object Loading : ApiState<Nothing>()
    data class Success<T>(val data: T)       : ApiState<T>()
    data class Error(val message: String)    : ApiState<Nothing>()
}

// ─── ViewModel ────────────────────────────────────────────────────────────────

class FloodViewModel(
    private val repository: FloodRepository = FloodRepository(ApiClient.apiService)
) : ViewModel() {

    private val TAG  = "FloodViewModel"
    private val gson = Gson()

    // ─── State flows ─────────────────────────────────────────────────────────

    private val _latestImageState =
        MutableStateFlow<ApiState<GetLatestImageResponse>>(ApiState.Idle)
    val latestImageState: StateFlow<ApiState<GetLatestImageResponse>> = _latestImageState

    private val _predictFloodState =
        MutableStateFlow<ApiState<PredictFloodResponse>>(ApiState.Idle)
    val predictFloodState: StateFlow<ApiState<PredictFloodResponse>> = _predictFloodState

    // Countdown helpers for the 20 s real-time monitoring cycle
    private val _countdown    = MutableStateFlow(AppConfig.UI.COUNTDOWN_START)
    val countdown             = _countdown.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing          = _isRefreshing.asStateFlow()

    // ─── Countdown helpers ────────────────────────────────────────────────────

    fun resetCountdown() {
        _countdown.value    = AppConfig.UI.COUNTDOWN_START
        _isRefreshing.value = false
    }

    fun setRefreshing() {
        _isRefreshing.value = true
    }

    fun decrementCountdown() {
        _countdown.value = (_countdown.value - 1).coerceAtLeast(0)
    }

    // ─── Cache restore ────────────────────────────────────────────────────────

    /**
     * Re-hydrates states from SharedPreferences on screen init.
     * Called once from LaunchedEffect(Unit) in PredictionScreen.
     */
    fun restoreFromCache() {
        Log.d(TAG, "restoreFromCache: start")
        CacheManager.getLatestImage()?.let {
            _latestImageState.value = ApiState.Success(it)
            Log.d(TAG, "restoreFromCache: latestImage restored")
        }
        CacheManager.getPredictFlood()?.let {
            _predictFloodState.value = ApiState.Success(it)
            Log.d(TAG, "restoreFromCache: predictFlood restored")
        }
    }

    // ─── Network calls ────────────────────────────────────────────────────────

    /** Fetch the latest S3 drain image. */
    fun fetchLatestImage() {
        viewModelScope.launch {
            Log.d(TAG, "fetchLatestImage: start")
            _latestImageState.value = ApiState.Loading
            try {
                val response = repository.getLatestImage()
                Log.d(TAG, "fetchLatestImage: success")
                _latestImageState.value = ApiState.Success(response)
            } catch (e: Exception) {
                Log.e(TAG, "fetchLatestImage: error → ${e.message}", e)
                _latestImageState.value = ApiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Submit a drain image for flood prediction.
     * On success the response includes prediction + location + weather + SHAP.
     */
    fun predictFlood(image: MultipartBody.Part, request: PredictFloodRequest) {
        viewModelScope.launch {
            Log.d(TAG, "predictFlood: start lat=${request.lat} lon=${request.lon}")
            _predictFloodState.value = ApiState.Loading
            try {
                val response = repository.predictFlood(image, request)
                Log.d(TAG, "predictFlood: success risk=${response.prediction.flood_risk}")
                _predictFloodState.value = ApiState.Success(response)
            } catch (e: Exception) {
                Log.e(TAG, "predictFlood: error → ${e.message}", e)
                _predictFloodState.value = ApiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    // ─── Broadcast-driven state updates (real-time monitoring) ────────────────

    /**
     * Called by PredictionScreen's BroadcastReceiver when the service
     * publishes a new S3 image.  Accepts either a raw base64 string or
     * a JSON-wrapped {"imageBase64":"..."} payload.
     */
    fun updateLatestImageState(base64: String) {
        val cleanBase64 = try {
            if (base64.trim().startsWith("{")) {
                // Service sent Gson-serialised GetLatestImageResponse
                Regex(""""imageBase64"\s*:\s*"(.*?)"""")
                    .find(base64)?.groupValues?.get(1) ?: base64
            } else base64
        } catch (e: Exception) { base64 }

        val response = GetLatestImageResponse(cleanBase64)
        _latestImageState.value = ApiState.Success(response)
        CacheManager.saveLatestImage(response)
        Log.d(TAG, "updateLatestImageState: done (len=${cleanBase64.length})")
    }

    /**
     * Called by the BroadcastReceiver when the service publishes a new
     * flood-prediction JSON payload.
     */
    fun updatePredictFloodState(json: String) {
        try {
            val data = gson.fromJson(json, PredictFloodResponse::class.java)
            _predictFloodState.value = ApiState.Success(data)
            CacheManager.savePredictFlood(data)
            Log.d(TAG, "updatePredictFloodState: risk=${data.prediction.flood_risk}")
        } catch (e: Exception) {
            Log.e(TAG, "updatePredictFloodState: parse error → ${e.message}")
            _predictFloodState.value = ApiState.Error("Parse error: ${e.message}")
        }
    }

    // ─── Error setters ────────────────────────────────────────────────────────

    fun setLatestImageError(message: String) {
        Log.e(TAG, "setLatestImageError: $message")
        _latestImageState.value = ApiState.Error(message)
    }

    fun setPredictFloodError(message: String) {
        Log.e(TAG, "setPredictFloodError: $message")
        _predictFloodState.value = ApiState.Error(message)
    }

    // ─── Reset ────────────────────────────────────────────────────────────────

    /** Resets prediction state to Idle (e.g., on mode toggle). */
    fun resetPredictionStates() {
        Log.d(TAG, "resetPredictionStates: clearing")
        _predictFloodState.value = ApiState.Idle
    }
}
