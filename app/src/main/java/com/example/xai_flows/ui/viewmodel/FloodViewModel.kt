package com.example.xai_flows.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xai_flows.core.data.api.ApiClient
import com.example.xai_flows.core.data.models.GetLatestImageResponse
import com.example.xai_flows.core.data.models.PredictFloodRequest
import com.example.xai_flows.core.data.models.PredictFloodResponse
import com.example.xai_flows.core.data.models.ReverseGeocodeResponse
import com.example.xai_flows.core.data.repository.FloodRepository
import com.example.xai_flows.utils.CacheManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import com.google.gson.Gson
import kotlinx.coroutines.flow.asStateFlow

sealed class ApiState<out T> {
    object Idle : ApiState<Nothing>()
    object Loading : ApiState<Nothing>()
    data class Success<T>(val data: T) : ApiState<T>()
    data class Error(val message: String) : ApiState<Nothing>()
}

class FloodViewModel(
    private val repository: FloodRepository = FloodRepository(ApiClient.apiService)
) : ViewModel() {

    private val TAG = "FloodViewModel"
    private val gson = Gson()

    // States
    private val _latestImageState = MutableStateFlow<ApiState<GetLatestImageResponse>>(ApiState.Idle)
    val latestImageState: StateFlow<ApiState<GetLatestImageResponse>> = _latestImageState

    private val _reverseGeocodeState = MutableStateFlow<ApiState<ReverseGeocodeResponse>>(ApiState.Idle)
    val reverseGeocodeState: StateFlow<ApiState<ReverseGeocodeResponse>> = _reverseGeocodeState

    private val _predictFloodState = MutableStateFlow<ApiState<PredictFloodResponse>>(ApiState.Idle)
    val predictFloodState: StateFlow<ApiState<PredictFloodResponse>> = _predictFloodState

    private val _countdown = MutableStateFlow(20)
    val countdown = _countdown.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    fun resetCountdown() {
        _countdown.value = 20
        _isRefreshing.value = false
    }
    fun setRefreshing() {
        _isRefreshing.value = true
    }
    fun decrementCountdown() {
        _countdown.value = (_countdown.value - 1).coerceAtLeast(0)
    }

    fun restoreFromCache() {
        Log.d("ViewModel", "Restoring from cache...")
        CacheManager.getLatestImage()?.let {
            _latestImageState.value = ApiState.Success(it)
            Log.d("ViewModel", "PredictFlood restored")
        }
        CacheManager.getPredictFlood()?.let {
            _predictFloodState.value = ApiState.Success(it)
            Log.d("ViewModel", "ReverseGeocode restored")
        }
        CacheManager.getReverseGeo()?.let {
            _reverseGeocodeState.value = ApiState.Success(it)
            Log.d("ViewModel", "LatestImage restored")
        }
    }

    /** Fetch latest image */
    fun fetchLatestImage() {
        viewModelScope.launch {
            Log.d(TAG, "fetchLatestImage: Starting API call")
            _latestImageState.value = ApiState.Loading
            try {
                val response = repository.getLatestImage()
                Log.d(TAG, "fetchLatestImage: Success -> $response")
                _latestImageState.value = ApiState.Success(response)
            } catch (e: Exception) {
                Log.e(TAG, "fetchLatestImage: Error -> ${e.message}", e)
                _latestImageState.value = ApiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /** Reverse geocode */
    fun fetchReverseGeocode(lat: Double, lon: Double) {
        viewModelScope.launch {
            Log.d(TAG, "fetchReverseGeocode: Starting API call for lat=$lat lon=$lon")
            _reverseGeocodeState.value = ApiState.Loading
            try {
                val response = repository.reverseGeocode(lat, lon)
                Log.d(TAG, "fetchReverseGeocode: Success -> ${response.city}, ${response.address}")
                _reverseGeocodeState.value = ApiState.Success(response)
            } catch (e: Exception) {
                Log.e(TAG, "fetchReverseGeocode: Error -> ${e.message}", e)
                _reverseGeocodeState.value = ApiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /** Predict flood */
    fun predictFlood(image: MultipartBody.Part, request: PredictFloodRequest) {
        viewModelScope.launch {
            Log.d(TAG, "predictFlood: Starting API call with request=$request")
            _predictFloodState.value = ApiState.Loading
            try {
                val response = repository.predictFlood(image, request)
                Log.d(TAG, "predictFlood: Success -> $response")
                _predictFloodState.value = ApiState.Success(response)
            } catch (e: Exception) {
                Log.e(TAG, "predictFlood: Error -> ${e.message}", e)
                _predictFloodState.value = ApiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun updateLatestImageState(base64: String) {
        // Extract clean base64 if wrapped in JSON
        Log.d(TAG, "updateLatestImageState: $base64")
        val cleanBase64 = try {
            if (base64.trim().startsWith("{")) {
                Regex("\"imageBase64\"\\s*:\\s*\"(.*?)\"")
                    .find(base64)?.groupValues?.get(1) ?: base64
            } else base64
        } catch (e: Exception) {
            base64
        }
        Log.d(TAG, "updateLatestImageState: $cleanBase64")

        _latestImageState.value = ApiState.Success(GetLatestImageResponse(cleanBase64))
        CacheManager.saveLatestImage(GetLatestImageResponse(cleanBase64))
    }

    fun resetPredictionStates() {
        Log.d(TAG, "resetPredictionStates: Resetting prediction states")
        _predictFloodState.value = ApiState.Idle
    }

    fun setLatestImageError(message: String) {
        Log.e(TAG, "setLatestImageError: $message")
        _latestImageState.value = ApiState.Error(message)
    }

    fun setReverseGeocodeError(message: String) {
        Log.e(TAG, "setReverseGeocodeError: $message")
        _reverseGeocodeState.value = ApiState.Error(message)
    }

    fun updateReverseGeocodeState(json: String) {
        try {
            val data = parseReverseGeoJson(json)
            Log.d(TAG, "updateReverseGeocodeState: $data")
            _reverseGeocodeState.value = ApiState.Success(data)
        } catch (e: Exception) {
            Log.e(TAG, "updateReverseGeocodeState: Parse error: ${e.message}")
            _reverseGeocodeState.value = ApiState.Error("Parse error: ${e.message}")
        }
    }

    fun updatePredictFloodState(json: String) {
        try {
            val data = parsePredictFloodJson(json)
            _predictFloodState.value = ApiState.Success(data)
        } catch (e: Exception) {
            _predictFloodState.value = ApiState.Error("Parse error: ${e.message}")
        }
    }

    fun setPredictFloodError(message: String) {
        Log.e(TAG, "setPredictFloodError: $message")
        _predictFloodState.value = ApiState.Error(message)
    }

    fun parseReverseGeoJson(json: String): ReverseGeocodeResponse {
        Log.d(TAG, "parseReverseGeoJson: $json")
        return gson.fromJson(json, ReverseGeocodeResponse::class.java)
    }

    fun parsePredictFloodJson(json: String): PredictFloodResponse {
        Log.d(TAG, "parsePredictFloodJson: $json")
        return gson.fromJson(json, PredictFloodResponse::class.java)
    }
}
