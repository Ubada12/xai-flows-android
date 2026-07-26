/**
 * PredictionScreen.kt
 * ─────────────────────────────────────────────────────────────────────────────
 * Main prediction UI. Supports two modes:
 *
 *   Live mode    — starts RealTimeMonitoringService; the foreground service
 *                  fetches an S3 image every 20 s, runs the full ML pipeline
 *                  on the backend, and broadcasts results back here via a
 *                  BroadcastReceiver.
 *
 *   Manual mode  — user picks an image from the gallery; pressing "Get
 *                  Prediction" calls the backend directly via FloodViewModel.
 *
 * Changes in this version:
 *   - RealTimeMonitoringContract moved to core.service; import instead of
 *     duplicating the object definition here.
 *   - ACTION_REVERSE_GEOCODE removed (backend now geocodes internally;
 *     location data is inside PredictFloodResponse.location).
 *   - reverseGeocodeState removed; predictionData reads city/address from
 *     predictFloodState.data.location.
 *   - weatherData mapping updated for new WeatherInfo field names.
 *   - Default coordinates changed from London to Mumbai (19.0760, 72.8777).
 *   - getAirState signature changed: rh: Int → rh: Double.
 */
package org.ubada.xaiflows.ui.screens

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import org.ubada.xaiflows.core.cache.CacheManager
import org.ubada.xaiflows.core.data.models.PredictFloodRequest
import org.ubada.xaiflows.core.data.models.PredictFloodResponse
import org.ubada.xaiflows.core.service.RealTimeMonitoringContract
import org.ubada.xaiflows.core.service.RealTimeMonitoringService
import org.ubada.xaiflows.ui.components.prediction.*
import org.ubada.xaiflows.ui.models.*
import org.ubada.xaiflows.ui.viewmodel.ApiState
import org.ubada.xaiflows.ui.viewmodel.FloodViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import java.util.Calendar
import org.ubada.xaiflows.core.config.AppConfig

// ─── Constants ────────────────────────────────────────────────────────────────

private const val TAG = "PredictionScreen"

// ─── Environment checks ───────────────────────────────────────────────────────

/** Returns true when an active internet connection is present. */
@RequiresApi(Build.VERSION_CODES.M)
private fun isInternetAvailable(context: Context): Boolean {
    val cm       = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network  = cm.activeNetwork ?: return false
    val caps     = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

/** Returns true when POST_NOTIFICATIONS is granted (always true on API < 33). */
private fun hasNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else true
}

// ─── Weather presentation helpers ────────────────────────────────────────────

/**
 * Derives a human-readable air-quality label from relative humidity and
 * dew-point. Note: humidity is now Double (backend returns fractional %).
 */
private fun getAirState(rh: Double, dewpt: Double): String = when {
    rh < 30.0 && dewpt < 10.0              -> "Very Dry"
    rh < 40.0 && dewpt < 15.0              -> "Dry"
    rh >= 40.0 && rh <= 60.0 && dewpt >= 10.0 -> "Moderate Humidity"
    else                                   -> "Humid"
}

/** Maps a wind direction in degrees to a compass label. */
private fun getWindDirection(windDir: Int): String {
    val directions = listOf("North", "Northeast", "East", "Southeast",
                            "South", "Southwest", "West", "Northwest")
    return directions[(Math.round(windDir / 45.0) % 8).toInt()]
}

/** Derives a cloud-coverage label from cloud percentage. */
private fun getCloudCoverage(clouds: Int): String = when {
    clouds == 0   -> "Clear Sky"
    clouds < 30   -> "Partly Cloudy"
    clouds < 70   -> "Mostly Cloudy"
    else          -> "Overcast"
}

/** Derives a time-of-day label from a 24-hour value. */
private fun getTimeOfDay(hour: Int): String = when (hour) {
    in 5..11  -> "Morning"
    in 12..16 -> "Afternoon"
    in 17..19 -> "Evening"
    else      -> "Night"
}

// ─── Main composable ─────────────────────────────────────────────────────────

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PredictionScreen(viewModel: FloodViewModel = viewModel()) {
    val context = LocalContext.current

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isMonitoring by remember { mutableStateOf(false) }
    var isLoading    by remember { mutableStateOf(false) }

    // Runtime permission launcher for POST_NOTIFICATIONS
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        Log.d(TAG, "POST_NOTIFICATIONS granted=$granted")
        if (!granted) Toast.makeText(context, "Enable notifications to monitor", Toast.LENGTH_SHORT).show()
    }

    // Restore persisted state on first composition
    LaunchedEffect(Unit) {
        viewModel.restoreFromCache()
        isMonitoring = CacheManager.isMonitoring()
        Log.d(TAG, "Cache restored — isMonitoring=$isMonitoring")
    }

    // ─── BroadcastReceiver (scoped to composition) ────────────────────────────
    DisposableEffect(Unit) {
        Log.d(TAG, "Registering BroadcastReceiver")
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent == null) return
                when (intent.action) {

                    RealTimeMonitoringContract.ACTION_STATUS -> {
                        val status = intent.getStringExtra(RealTimeMonitoringContract.EXTRA_STATUS)
                        Log.d(TAG, "ACTION_STATUS → $status")
                        isMonitoring = status == "started"
                        isLoading    = status == "started"
                    }

                    RealTimeMonitoringContract.ACTION_LATEST_IMAGE -> {
                        val base64 = intent.getStringExtra(RealTimeMonitoringContract.EXTRA_DATA)
                        if (base64 != null) {
                            Log.d(TAG, "ACTION_LATEST_IMAGE → len=${base64.length}")
                            viewModel.updateLatestImageState(base64)
                        } else {
                            val err = intent.getStringExtra(RealTimeMonitoringContract.EXTRA_ERROR)
                            Log.w(TAG, "ACTION_LATEST_IMAGE error → $err")
                            viewModel.setLatestImageError(err ?: "Unknown image error")
                        }
                    }

                    RealTimeMonitoringContract.ACTION_PREDICT_FLOOD -> {
                        isLoading = false
                        val predictJson = intent.getStringExtra(RealTimeMonitoringContract.EXTRA_DATA)
                        if (predictJson != null) {
                            Log.d(TAG, "ACTION_PREDICT_FLOOD → success")
                            viewModel.updatePredictFloodState(predictJson)
                            errorMessage = null
                        } else {
                            val err = intent.getStringExtra(RealTimeMonitoringContract.EXTRA_ERROR)
                            Log.e(TAG, "ACTION_PREDICT_FLOOD error → $err")
                            viewModel.setPredictFloodError(err ?: "Unknown prediction error")
                        }
                    }

                    RealTimeMonitoringContract.ACTION_ERROR -> {
                        isLoading    = false
                        val err      = intent.getStringExtra(RealTimeMonitoringContract.EXTRA_ERROR)
                        Log.e(TAG, "ACTION_ERROR → $err")
                        errorMessage = err ?: "Unknown monitoring error"
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(RealTimeMonitoringContract.ACTION_STATUS)
            addAction(RealTimeMonitoringContract.ACTION_LATEST_IMAGE)
            addAction(RealTimeMonitoringContract.ACTION_PREDICT_FLOOD)
            addAction(RealTimeMonitoringContract.ACTION_ERROR)
        }
        context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        onDispose {
            Log.d(TAG, "Unregistering BroadcastReceiver")
            context.unregisterReceiver(receiver)
        }
    }

    // ─── State collection ─────────────────────────────────────────────────────
    val latestImageState  by viewModel.latestImageState.collectAsState()
    val predictFloodState by viewModel.predictFloodState.collectAsState()
    val countdown         by viewModel.countdown.collectAsState()
    val isRefreshing      by viewModel.isRefreshing.collectAsState()

    LaunchedEffect(latestImageState) {
        Log.d(TAG, "latestImageState → $latestImageState")
        if (latestImageState is ApiState.Success) viewModel.resetCountdown()
    }
    LaunchedEffect(predictFloodState) { Log.d(TAG, "predictFloodState → $predictFloodState") }

    // Countdown loop (runs while monitoring is active)
    LaunchedEffect(isMonitoring) {
        if (isMonitoring) {
            while (isMonitoring) {
                delay(1_000)
                if (!isRefreshing && countdown > 0) {
                    viewModel.decrementCountdown()
                    if (countdown == 1) viewModel.setRefreshing()
                }
            }
        } else {
            viewModel.resetCountdown()
        }
    }

    // ─── Derived presentation data ────────────────────────────────────────────

    val imageBase64 = (latestImageState as? ApiState.Success)?.data?.imageBase64

    /**
     * Build PredictionData from the flood response.
     * Location (city, address) is now taken from PredictFloodResponse.location
     * — no separate reverseGeocodeState needed.
     */
    val predictionData by remember(predictFloodState) {
        derivedStateOf {
            (predictFloodState as? ApiState.Success)?.data?.let { flood ->
                val blockage      = flood.drain_blockage ?: 2
                val blockageProb  = flood.drain_blockage_prob ?: 0.0
                // If blockage == 1 (No blockage), chance = 1 - prob; else use prob directly
                val blockageChance = if (blockage == 1) (1 - blockageProb) * 100 else blockageProb * 100
                PredictionData(
                    floodRisk        = flood.prediction.flood_risk,
                    reason           = flood.prediction.reason,
                    drainBlockageProb = String.format("%.2f", blockageChance),
                    drainBlockage    = blockage,
                    city             = flood.location.city,
                    address          = flood.location.address
                )
            }
        }
    }

    /**
     * Map WeatherInfo (network model) → WeatherData (UI model).
     * Uses device's current hour for timeOfDay since the new backend weather
     * model no longer includes an "hour" field.
     */
    val weatherData = (predictFloodState as? ApiState.Success)?.data?.let { f ->
        WeatherData(
            temp             = f.weather.temp,
            appTemp          = f.weather.app_temp,
            humidity         = f.weather.humidity,
            windSpeed        = f.weather.wind_speed,
            uv               = f.weather.uv,
            pressure         = f.weather.pressure,
            visibility       = f.weather.visibility,
            weatherCondition = f.weather.condition,
            precipitation    = f.weather.precipitation,
            airState         = getAirState(f.weather.humidity, f.weather.dewpt),
            windDirection    = getWindDirection(f.weather.wind_dir),
            cloudCoverage    = getCloudCoverage(f.weather.clouds),
            timeOfDay        = getTimeOfDay(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
        )
    }

    val isApiLoading = predictFloodState is ApiState.Loading

    // UI-only state
    var isManualMode  by rememberSaveable { mutableStateOf(false) }
    var selectedImage by remember { mutableStateOf<MultipartBody.Part?>(null) }
    var shapData      by remember { mutableStateOf<List<ShapData>>(emptyList()) }

    // Default to Mumbai coordinates
    var longitude by rememberSaveable { mutableStateOf(AppConfig.UI.DEFAULT_LONGITUDE_STR) }
    var latitude  by rememberSaveable { mutableStateOf(AppConfig.UI.DEFAULT_LATITUDE_STR) }

    // Update SHAP list whenever prediction changes
    LaunchedEffect(predictFloodState) {
        shapData = (predictFloodState as? ApiState.Success)?.data
            ?.weather_shap_value
            ?.map { ShapData(it.feature, it.value.toFloat()) }
            ?: emptyList()
        Log.d(TAG, "SHAP updated — ${shapData.size} items")
    }

    // ─── UI ───────────────────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackground()
        FrostedCardContainer {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PredictionHeader()
                Spacer(Modifier.height(20.dp))

                // Mode toggle
                ModeToggle(isManualMode = isManualMode) {
                    isManualMode = !isManualMode
                    viewModel.resetPredictionStates()
                    shapData = emptyList()
                    CacheManager.clearAll()
                    Log.d(TAG, "Mode toggled → manual=$isManualMode (states cleared)")
                }

                Spacer(Modifier.height(24.dp))

                CoordinatesInputs(
                    longitude  = longitude,
                    latitude   = latitude,
                    readOnly   = !isManualMode,
                    onLongitude = { if (isManualMode) longitude = it },
                    onLatitude  = { if (isManualMode) latitude = it }
                )

                Spacer(Modifier.height(20.dp))

                if (isManualMode) {
                    ImageUpload(
                        onImageSelected = {
                            selectedImage = it
                            Log.d(TAG, "Image selected → ${it != null}")
                            Toast.makeText(context,
                                if (it != null) "Image selected" else "Image cleared",
                                Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    LiveFeed(
                        isStreaming  = isMonitoring,
                        countdown    = countdown,
                        isRefreshing = isRefreshing,
                        imageBase64  = imageBase64,
                        modifier     = Modifier.fillMaxWidth().height(220.dp)
                    )
                }

                Spacer(Modifier.height(24.dp))

                if (isManualMode) {
                    ManualPredictionButton(
                        enabled   = !isApiLoading,
                        onPredict = {
                            Log.d(TAG, "Manual predict clicked | hasImage=${selectedImage != null}")
                            if (selectedImage != null) {
                                viewModel.predictFlood(
                                    selectedImage!!,
                                    PredictFloodRequest(
                                        lon = longitude.toDouble(),
                                        lat = latitude.toDouble()
                                    )
                                )
                            } else {
                                errorMessage = "Please select an image before predicting"
                                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                } else {
                    RealTimeMonitoringButton(
                        isMonitoring = isMonitoring,
                        isLoading    = isLoading,
                        onStart = start@{
                            isLoading = true
                            Log.d(TAG, "Start Monitoring clicked")

                            // Internet guard
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                                !isInternetAvailable(context)) {
                                errorMessage = "No internet connection"
                                isLoading = false
                                Log.w(TAG, "Start blocked: no internet")
                                return@start
                            }

                            // Notification permission guard
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                !hasNotificationPermission(context)) {
                                Log.w(TAG, "POST_NOTIFICATIONS missing — requesting")
                                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                isLoading = false
                                return@start
                            }

                            // Start the foreground service with coordinates
                            val intent = Intent(context, RealTimeMonitoringService::class.java).apply {
                                putExtra("latitude",  latitude.toDoubleOrNull()  ?: AppConfig.Monitoring.DEFAULT_LATITUDE)
                                putExtra("longitude", longitude.toDoubleOrNull() ?: AppConfig.Monitoring.DEFAULT_LONGITUDE)
                            }
                            ContextCompat.startForegroundService(context, intent)
                        },
                        onStop = {
                            Log.d(TAG, "Stop Monitoring clicked")
                            context.stopService(Intent(context, RealTimeMonitoringService::class.java))
                            isLoading    = false
                            isMonitoring = false
                            CacheManager.clearAll()
                        }
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Error display
                ErrorSection(
                    predictState = predictFloodState,
                    errorMessage = errorMessage,
                    onRetryPredict = {
                        if (selectedImage != null) {
                            viewModel.predictFlood(
                                selectedImage!!,
                                PredictFloodRequest(longitude.toDouble(), latitude.toDouble())
                            )
                        }
                    },
                    clearLocalError = { errorMessage = null }
                )

                // Result cards
                predictionData?.let { PredictionCard(prediction = it); Spacer(Modifier.height(20.dp)) }
                weatherData?.let    { WeatherCard(weather = it);        Spacer(Modifier.height(20.dp)) }
                if (shapData.isNotEmpty()) { ShapChart(data = shapData); Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}

// ─── Frosted card container ───────────────────────────────────────────────────

@Composable
private fun FrostedCardContainer(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        colors   = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(0.dp),
        shape    = RoundedCornerShape(28.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(listOf(
                        Color.White.copy(alpha = 0.18f),
                        Color.White.copy(alpha = 0.08f)
                    ))
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(listOf(
                        Color.White.copy(alpha = 0.5f),
                        Color.White.copy(alpha = 0.15f)
                    )),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(20.dp)
        ) { content() }
    }
}

// ─── Coordinates input ────────────────────────────────────────────────────────

@Composable
private fun CoordinatesInputs(
    longitude: String, latitude: String,
    readOnly: Boolean,
    onLongitude: (String) -> Unit,
    onLatitude: (String) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        CustomTextField(longitude, onLongitude, "Longitude", readOnly, Modifier.weight(1f))
        CustomTextField(latitude,  onLatitude,  "Latitude",  readOnly, Modifier.weight(1f))
    }
}

// ─── Error section ────────────────────────────────────────────────────────────

@Composable
private fun ErrorSection(
    predictState: ApiState<PredictFloodResponse>,
    errorMessage: String?,
    onRetryPredict: () -> Unit,
    clearLocalError: () -> Unit
) {
    val predictError = (predictState as? ApiState.Error)?.message?.let { "Prediction Error: $it" }
    val toShow       = predictError ?: errorMessage

    if (toShow != null) {
        Log.e(TAG, "Error UI → $toShow")
        ErrorDisplay(
            error   = toShow,
            onRetry = {
                clearLocalError()
                if (predictError != null) onRetryPredict()
            }
        )
    }
}

// ─── Buttons ──────────────────────────────────────────────────────────────────

@Composable
private fun GradientButton(
    text: String,
    gradientColors: List<Color>,
    isLoading: Boolean,
    showResumeIcon: Boolean = false,
    showPauseIcon: Boolean  = false,
    onClick: () -> Unit
) {
    Button(
        onClick  = { if (!isLoading) onClick() },
        modifier = Modifier.fillMaxWidth().height(50.dp),
        colors   = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues()
    ) {
        Box(
            modifier = Modifier
                .background(Brush.horizontalGradient(gradientColors))
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showResumeIcon) Icon(Icons.Default.PlayArrow, "Start", tint = Color.White, modifier = Modifier.size(20.dp).padding(end = 6.dp))
                    if (showPauseIcon)  Icon(Icons.Default.Stop,      "Stop",  tint = Color.White, modifier = Modifier.size(20.dp).padding(end = 6.dp))
                    Text(text, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ManualPredictionButton(enabled: Boolean, onPredict: suspend () -> Unit) {
    var isLoading by remember { mutableStateOf(false) }
    val scope     = rememberCoroutineScope()
    GradientButton(
        text           = if (!enabled) "Predicting…" else "Get Prediction",
        gradientColors = listOf(Color(0xFF34D399), Color(0xFF10B981)),
        isLoading      = !enabled,
        onClick        = {
            if (enabled && !isLoading) {
                isLoading = true
                scope.launch { try { onPredict() } finally { isLoading = false } }
            }
        }
    )
}

@Composable
private fun RealTimeMonitoringButton(
    isMonitoring: Boolean,
    isLoading: Boolean,
    onStart: suspend () -> Unit,
    onStop: () -> Unit
) {
    val scope = rememberCoroutineScope()
    GradientButton(
        text = when {
            isLoading    -> "Starting…"
            isMonitoring -> "Stop Real-time Monitoring"
            else         -> "Start Real-time Monitoring"
        },
        gradientColors = if (isMonitoring)
            listOf(Color(0xFFFF6B6B), Color(0xFFE63946))
        else
            listOf(Color(0xFF3B82F6), Color(0xFF6366F1)),
        isLoading      = isLoading,
        showResumeIcon = !isMonitoring && !isLoading,
        showPauseIcon  = isMonitoring  && !isLoading
    ) {
        if (isMonitoring) onStop() else scope.launch { onStart() }
    }
}

// ─── Text field ───────────────────────────────────────────────────────────────

@Composable
private fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    readOnly: Boolean,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        label         = { Text(label) },
        readOnly      = readOnly,
        modifier      = modifier,
        singleLine    = true,
        colors        = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = Color(0xFF6366F1),
            unfocusedBorderColor = Color(0xFFCBD5E1),
            disabledBorderColor  = Color.Gray,
            errorBorderColor     = Color.Red,
            focusedLabelColor    = Color(0xFF6366F1),
            unfocusedLabelColor  = Color(0xFFCBD5E1)
        )
    )
}
