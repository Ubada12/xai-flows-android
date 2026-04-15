package com.example.xai_flows.ui.screens

// ------------------------------
// Imports
// ------------------------------
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
import com.example.xai_flows.core.data.models.PredictFloodRequest
import com.example.xai_flows.core.data.models.PredictFloodResponse
import com.example.xai_flows.core.data.models.ReverseGeocodeResponse
import com.example.xai_flows.core.service.RealTimeMonitoringService
import com.example.xai_flows.ui.components.prediction.*
import com.example.xai_flows.ui.models.*
import com.example.xai_flows.ui.viewmodel.ApiState
import com.example.xai_flows.ui.viewmodel.FloodViewModel
import com.example.xai_flows.utils.CacheManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MultipartBody

/**
 * PredictionScreen.kt
 * --------------------
 * Refactored for: ultra-clean structure, strong logging, modular helpers, and clear comments.
 *
 * Highlights
 * - Centralized TAG + verbose debug logs around every state transition.
 * - Safe Internet + Notification-permission checks with inline logic (to satisfy lint).
 * - BroadcastReceiver lifecycle is scoped to composition via DisposableEffect.
 * - Small composable for readability: Controls, Inputs, Live/Upload section, Errors, and Cards.
 */

// -------------------------------------------------
// Logging utilities & constants
// -------------------------------------------------
private const val TAG = "PredictionScreen"

// Contract keys kept as object for discoverability & single source of truth
object RealTimeMonitoringContract {
    const val ACTION_LATEST_IMAGE = "com.example.xai_flows.ACTION_LATEST_IMAGE"
    const val ACTION_REVERSE_GEOCODE = "com.example.xai_flows.ACTION_REVERSE_GEOCODE"
    const val ACTION_PREDICT_FLOOD = "com.example.xai_flows.ACTION_PREDICT_FLOOD"
    const val ACTION_ERROR = "com.example.xai_flows.ACTION_ERROR"
    const val ACTION_STATUS = "com.example.xai_flows.ACTION_STATUS"

    const val EXTRA_DATA = "extra_data"
    const val EXTRA_ERROR = "extra_error"
    const val EXTRA_STATUS = "extra_status"
}

// -------------------------------------------------
// Environment checks (inline logic keeps lint happy)
// -------------------------------------------------
@RequiresApi(Build.VERSION_CODES.M)
private fun isInternetAvailable(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return false
    val capabilities = cm.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

private fun hasNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else true
}

// -------------------------------------------------
// Tiny domain helpers for weather strings (unchanged logic)
// -------------------------------------------------
private fun getAirState(rh: Int, dewpt: Double): String = when {
    rh < 30 && dewpt < 10 -> "Very Dry"
    rh < 40 && dewpt < 15 -> "Dry"
    rh in 40..60 && dewpt >= 10 -> "Moderate Humidity"
    else -> "Humid"
}

private fun getWindDirection(windDir: Int): String {
    val directions = listOf("North", "Northeast", "East", "Southeast", "South", "Southwest", "West", "Northwest")
    return directions[(Math.round(windDir / 45.0) % 8).toInt()]
}

private fun getCloudCoverage(clouds: Int): String = when {
    clouds == 0 -> "Clear Sky"
    clouds < 30 -> "Partly Cloudy"
    clouds < 70 -> "Mostly Cloudy"
    else -> "Overcast"
}

private fun getTimeOfDay(hour: Int): String = when (hour) {
    in 5..11 -> "Morning"
    in 12..16 -> "Afternoon"
    in 17..19 -> "Evening"
    else -> "Night"
}

// -------------------------------------------------
// Composable: PredictionScreen
// -------------------------------------------------
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PredictionScreen(viewModel: FloodViewModel = viewModel()) {
    val context = LocalContext.current

    // Global UI flags
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isMonitoring by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Ask for POST_NOTIFICATIONS at runtime when needed
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        Log.d(TAG, "POST_NOTIFICATIONS permission result -> granted=$granted")
        if (!granted) Toast.makeText(context, "Enable notifications to monitor", Toast.LENGTH_SHORT).show()
    }

    // Restore persisted state once
    LaunchedEffect(Unit) {
        Log.d(TAG, "Restore from cache: start")
        viewModel.restoreFromCache()
        isMonitoring = CacheManager.isMonitoring()
        Log.d(TAG, "Restore from cache: isMonitoring=$isMonitoring")
    }

    // ------------------------------
    // BroadcastReceiver: single instance, scoped lifecycle
    // ------------------------------
    DisposableEffect(Unit) {
        Log.d(TAG, "Registering BroadcastReceiver")
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent == null) return
                when (intent.action) {
                    RealTimeMonitoringContract.ACTION_STATUS -> {
                        val status = intent.getStringExtra(RealTimeMonitoringContract.EXTRA_STATUS)
                        Log.d(TAG, "ACTION_STATUS -> $status")
                        isMonitoring = status == "started"
                        isLoading = status == "started"
                    }
                    RealTimeMonitoringContract.ACTION_LATEST_IMAGE -> {
                        val base64 = intent.getStringExtra(RealTimeMonitoringContract.EXTRA_DATA)
                        if (base64 != null) {
                            Log.d(TAG, "ACTION_LATEST_IMAGE -> success (len=${base64.length})")
                            viewModel.updateLatestImageState(base64)
                        } else {
                            val err = intent.getStringExtra(RealTimeMonitoringContract.EXTRA_ERROR)
                            Log.w(TAG, "ACTION_LATEST_IMAGE -> error=$err")
                            viewModel.setLatestImageError(err ?: "Unknown image error")
                        }
                    }
                    RealTimeMonitoringContract.ACTION_REVERSE_GEOCODE -> {
                        val reverseJson = intent.getStringExtra(RealTimeMonitoringContract.EXTRA_DATA)
                        if (reverseJson != null) {
                            Log.d(TAG, "ACTION_REVERSE_GEOCODE -> success")
                            viewModel.updateReverseGeocodeState(reverseJson)
                        } else {
                            val err = intent.getStringExtra(RealTimeMonitoringContract.EXTRA_ERROR)
                            Log.w(TAG, "ACTION_REVERSE_GEOCODE -> error=$err")
                            viewModel.setReverseGeocodeError(err ?: "Unknown reverse geocode error")
                        }
                    }
                    RealTimeMonitoringContract.ACTION_PREDICT_FLOOD -> {
                        isLoading = false
                        val predictJson = intent.getStringExtra(RealTimeMonitoringContract.EXTRA_DATA)
                        if (predictJson != null) {
                            Log.d(TAG, "ACTION_PREDICT_FLOOD -> success")
                            viewModel.updatePredictFloodState(predictJson)
                            errorMessage = null
                        } else {
                            val err = intent.getStringExtra(RealTimeMonitoringContract.EXTRA_ERROR)
                            Log.e(TAG, "ACTION_PREDICT_FLOOD -> error=$err")
                            viewModel.setPredictFloodError(err ?: "Unknown prediction error")
                        }
                    }
                    RealTimeMonitoringContract.ACTION_ERROR -> {
                        isLoading = false
                        val err = intent.getStringExtra(RealTimeMonitoringContract.EXTRA_ERROR)
                        Log.e(TAG, "ACTION_ERROR -> $err")
                        errorMessage = err ?: "Unknown monitoring error"
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(RealTimeMonitoringContract.ACTION_STATUS)
            addAction(RealTimeMonitoringContract.ACTION_LATEST_IMAGE)
            addAction(RealTimeMonitoringContract.ACTION_REVERSE_GEOCODE)
            addAction(RealTimeMonitoringContract.ACTION_PREDICT_FLOOD)
            addAction(RealTimeMonitoringContract.ACTION_ERROR)
        }
        // RECEIVER_EXPORTED is fine as Service is your process; adjust if you scope it differently
        context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        onDispose {
            Log.d(TAG, "Unregistering BroadcastReceiver")
            context.unregisterReceiver(receiver)
        }
    }

    // ------------------------------
    // Observe ViewModel state flows
    // ------------------------------
    val latestImageState by viewModel.latestImageState.collectAsState()
    val reverseGeocodeState by viewModel.reverseGeocodeState.collectAsState()
    val predictFloodState by viewModel.predictFloodState.collectAsState()
    val countdown by viewModel.countdown.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    // Log state transitions for deep debugging
    LaunchedEffect(latestImageState) { Log.d(TAG, "latestImageState -> $latestImageState"); if (latestImageState is ApiState.Success) viewModel.resetCountdown() }
    LaunchedEffect(reverseGeocodeState) { Log.d(TAG, "reverseGeocodeState -> $reverseGeocodeState") }
    LaunchedEffect(predictFloodState) { Log.d(TAG, "predictFloodState -> $predictFloodState") }

    // Countdown: runs only while monitoring
    LaunchedEffect(isMonitoring) {
        Log.d(TAG, "isMonitoring changed -> $isMonitoring (start countdown loop if true)")
        if (isMonitoring) {
            while (isMonitoring) {
                delay(1000)
                if (!isRefreshing && countdown > 0) {
                    viewModel.decrementCountdown()
                    if (countdown == 1) viewModel.setRefreshing()
                }
            }
        } else {
            viewModel.resetCountdown()
        }
    }

    // Extract success data
    val imageBase64 = (latestImageState as? ApiState.Success)?.data?.imageBase64
    val predictFloodData = (predictFloodState as? ApiState.Success)?.data
    predictFloodData?.let { Log.d(TAG, "PredictFloodData -> ${it.prediction.flood_risk}") }

    // Derived presentation model
    val predictionData by remember(predictFloodState, reverseGeocodeState) {
        derivedStateOf {
            if (predictFloodState is ApiState.Success && reverseGeocodeState is ApiState.Success) {
                val flood = (predictFloodState as ApiState.Success).data
                val reverse = (reverseGeocodeState as ApiState.Success).data
                val blockageChance = if (flood.drain_blockage == 1) {
                    (1 - flood.drain_blockage_prob) * 100
                } else {
                    flood.drain_blockage_prob * 100
                }
                val formattedChance = String.format("%.2f", blockageChance)
                PredictionData(
                    floodRisk = flood.prediction.flood_risk,
                    reason = flood.prediction.reason,
                    drainBlockageProb = formattedChance,
                    drainBlockage = flood.drain_blockage,
                    city = reverse.city,
                    address = reverse.address
                )
            } else null
        }
    }

    val weatherData = (predictFloodState as? ApiState.Success)?.data?.let { f ->
        WeatherData(
            temp = f.weather_data.temp,
            appTemp = f.weather_data.app_temp,
            humidity = f.weather_data.rh,
            windSpeed = f.weather_data.wind_spd,
            uv = f.weather_data.uv,
            pressure = f.weather_data.pres,
            visibility = f.weather_data.vis.toDouble(),
            weatherCondition = f.weather_prediction.weather,
            precipitation = f.weather_prediction.precip,
            airState = getAirState(f.weather_data.rh, f.weather_data.dewpt),
            windDirection = getWindDirection(f.weather_data.wind_dir),
            cloudCoverage = getCloudCoverage(f.weather_data.clouds),
            timeOfDay = getTimeOfDay(f.weather_data.hour)
        )
    }

    val isApiLoading = predictFloodState is ApiState.Loading || reverseGeocodeState is ApiState.Loading

    // UI state
    var isManualMode by rememberSaveable { mutableStateOf(false) }
    var selectedImage by remember { mutableStateOf<MultipartBody.Part?>(null) }
    var shapData by remember { mutableStateOf<List<ShapData>>(emptyList()) }
    var longitude by rememberSaveable { mutableStateOf("0.1276") }
    var latitude by rememberSaveable { mutableStateOf("51.5072") }

    // Map SHAP values whenever prediction updates
    LaunchedEffect(predictFloodState) {
        val successData = (predictFloodState as? ApiState.Success)?.data
        shapData = successData?.weather_shap_value?.map { ShapData(it.feature, it.value.toFloat()) } ?: emptyList()
        if (shapData.isNotEmpty()) Log.d(TAG, "SHAP updated -> ${shapData.size} items")
    }

    // ------------------------------
    // Composed UI
    // ------------------------------
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

                // Mode toggle + clear cache
                ModeToggle(isManualMode = isManualMode) {
                    isManualMode = !isManualMode
                    viewModel.resetPredictionStates()
                    shapData = emptyList()
                    CacheManager.clearAll()
                    Log.d(TAG, "Mode toggled -> manual=$isManualMode (states cleared)")
                }

                Spacer(Modifier.height(24.dp))

                CoordinatesInputs(
                    longitude = longitude,
                    latitude = latitude,
                    readOnly = !isManualMode,
                    onLongitude = { if (isManualMode) longitude = it },
                    onLatitude = { if (isManualMode) latitude = it }
                )

                Spacer(Modifier.height(20.dp))

                // Live vs Manual image section
                if (isManualMode) {
                    ImageUpload(
                        onImageSelected = {
                            selectedImage = it
                            Log.d(TAG, "Image selected -> ${it != null}")
                            Toast.makeText(context, if (it != null) "Image selected" else "Image cleared", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    LiveFeed(
                        isStreaming = isMonitoring,
                        countdown = countdown,
                        isRefreshing = isRefreshing,
                        imageBase64 = imageBase64,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Action buttons
                if (isManualMode) {
                    ManualPredictionButton(
                        enabled = !isApiLoading,
                        onPredict = {
                            Log.d(TAG, "Manual Predict clicked | hasImage=${selectedImage != null}")
                            if (selectedImage != null) {
                                val req = PredictFloodRequest(lon = longitude.toDouble(), lat = latitude.toDouble())
                                viewModel.predictFlood(selectedImage!!, req)
                                viewModel.fetchReverseGeocode(latitude.toDouble(), longitude.toDouble())
                            } else {
                                errorMessage = "Please select an image before predicting"
                                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                } else {
                    RealTimeMonitoringButton(
                        isMonitoring = isMonitoring,
                        isLoading = isLoading,
                        onStart = start@{
                            isLoading = true
                            Log.d(TAG, "Start Monitoring clicked")

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isInternetAvailable(context)) {
                                errorMessage = "No internet connection"
                                isLoading = false
                                Log.w(TAG, "Start blocked: no internet")
                                return@start
                            }

                            // Inline permission check keeps lint happy
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission(context)) {
                                Log.w(TAG, "POST_NOTIFICATIONS missing -> requesting")
                                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                isLoading = false
                                return@start
                            }

                            val intent = Intent(context, RealTimeMonitoringService::class.java)
                            ContextCompat.startForegroundService(context, intent)
                        },
                        onStop = {
                            Log.d(TAG, "Stop Monitoring clicked")
                            context.stopService(Intent(context, RealTimeMonitoringService::class.java))
                            isLoading = false
                            isMonitoring = false
                            CacheManager.clearAll()
                        }
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Error area (prediction / reverse geocode / local)
                ErrorSection(
                    predictState = predictFloodState,
                    reverseState = reverseGeocodeState,
                    errorMessage = errorMessage,
                    onRetryPredict = {
                        (predictFloodState as? ApiState.Error)?.let {
                            if (selectedImage != null) {
                                val req = PredictFloodRequest(lon = longitude.toDouble(), lat = latitude.toDouble())
                                viewModel.predictFlood(selectedImage!!, req)
                            }
                        }
                    },
                    onRetryReverse = {
                        if (reverseGeocodeState is ApiState.Error) {
                            viewModel.fetchReverseGeocode(latitude.toDouble(), longitude.toDouble())
                        }
                    },
                    clearLocalError = { errorMessage = null }
                )

                // Data cards
                predictionData?.let { PredictionCard(prediction = it); Spacer(Modifier.height(20.dp)) }
                weatherData?.let { WeatherCard(weather = it); Spacer(Modifier.height(20.dp)) }
                if (shapData.isNotEmpty()) { ShapChart(data = shapData); Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}

// -------------------------------------------------
// UI building blocks (modular composables)
// -------------------------------------------------
@Composable
private fun FrostedCardContainer(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(28.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.18f),
                            Color.White.copy(alpha = 0.08f)
                        )
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.5f),
                            Color.White.copy(alpha = 0.15f)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(20.dp)
        ) { content() }
    }
}

@Composable
private fun CoordinatesInputs(
    longitude: String,
    latitude: String,
    readOnly: Boolean,
    onLongitude: (String) -> Unit,
    onLatitude: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CustomTextField(
            value = longitude,
            onValueChange = onLongitude,
            label = "Longitude",
            readOnly = readOnly,
            modifier = Modifier.weight(1f)
        )
        CustomTextField(
            value = latitude,
            onValueChange = onLatitude,
            label = "Latitude",
            readOnly = readOnly,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ErrorSection(
    predictState: ApiState<PredictFloodResponse>,
    reverseState: ApiState<ReverseGeocodeResponse>,
    errorMessage: String?,
    onRetryPredict: () -> Unit,
    onRetryReverse: () -> Unit,
    clearLocalError: () -> Unit
) {
    val predictError = (predictState as? ApiState.Error)?.message?.let { "Prediction Error: $it" }
    val reverseError = (reverseState as? ApiState.Error)?.message?.let { "Reverse Geocode Error: $it" }
    val toShow = predictError ?: reverseError ?: errorMessage

    if (toShow != null) {
        Log.e(TAG, "Error UI -> $toShow")
        ErrorDisplay(
            error = toShow,
            onRetry = {
                clearLocalError()
                if (predictError != null) onRetryPredict() else if (reverseError != null) onRetryReverse()
            }
        )
    }
}

// -------------------------------------------------
// Reusable Buttons (unchanged UI, cleaned internals)
// -------------------------------------------------
@Composable
private fun GradientButton(
    text: String,
    gradientColors: List<Color>,
    isLoading: Boolean,
    showResumeIcon: Boolean = false,
    showPauseIcon: Boolean = false,
    onClick: () -> Unit
) {
    Button(
        onClick = { if (!isLoading) onClick() },
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
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
                    if (showResumeIcon) Icon(Icons.Default.PlayArrow, contentDescription = "Start Monitoring", tint = Color.White, modifier = Modifier.size(20.dp).padding(end = 6.dp))
                    if (showPauseIcon) Icon(Icons.Default.Stop, contentDescription = "Stop Monitoring", tint = Color.White, modifier = Modifier.size(20.dp).padding(end = 6.dp))
                    Text(text = text, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ManualPredictionButton(
    enabled: Boolean,
    onPredict: suspend () -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    GradientButton(
        text = if (!enabled) "Predicting..." else "Get Prediction",
        gradientColors = listOf(Color(0xFF34D399), Color(0xFF10B981)),
        isLoading = !enabled,
        onClick = {
            if (enabled && !isLoading) {
                isLoading = true
                scope.launch {
                    try { onPredict() } finally { isLoading = false }
                }
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
            isLoading -> "Starting..."
            isMonitoring -> "Stop Real-time Monitoring"
            else -> "Start Real-time Monitoring"
        },
        gradientColors = if (isMonitoring) listOf(Color(0xFFFF6B6B), Color(0xFFE63946)) else listOf(Color(0xFF3B82F6), Color(0xFF6366F1)),
        isLoading = isLoading,
        showResumeIcon = !isMonitoring && !isLoading,
        showPauseIcon = isMonitoring && !isLoading
    ) {
        if (isMonitoring) onStop() else scope.launch { onStart() }
    }
}

// -------------------------------------------------
// Styled text field wrapper (unchanged behavior, just grouped here)
// -------------------------------------------------
@Composable
private fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    readOnly: Boolean,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        readOnly = readOnly,
        modifier = modifier,
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF6366F1),
            unfocusedBorderColor = Color(0xFFCBD5E1),
            disabledBorderColor = Color.Gray,
            errorBorderColor = Color.Red,
            focusedLabelColor = Color(0xFF6366F1),
            unfocusedLabelColor = Color(0xFFCBD5E1)
        )
    )
}
