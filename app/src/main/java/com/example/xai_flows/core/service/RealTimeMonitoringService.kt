/**
 * RealTimeMonitoringService.kt
 * ─────────────────────────────────────────────────────────────────────────────
 * Android foreground service that runs a monitoring loop every
 * AppConfig.Monitoring.INTERVAL_MS (default 20 s):
 *
 *   1. Check internet connectivity (NetworkCapabilities API)
 *   2. Check POST_NOTIFICATIONS permission (Android 13+)
 *   3. Fetch latest S3 drain image  →  broadcast ACTION_LATEST_IMAGE
 *   4. Run flood prediction          →  broadcast ACTION_PREDICT_FLOOD
 *      (reverse-geocoding is done server-side; no separate geocode call)
 *   5. If risk level is in AppConfig.Notifications.ALERT_RISK_LEVELS
 *      → fire a high-priority flood alert notification
 *   6. Wait INTERVAL_MS, repeat
 *
 * All tunable values (interval, coords, notification settings) are in
 * AppConfig so you never need to touch this file for configuration changes.
 *
 * Bugs fixed vs. original:
 *  - Default coords were London → now AppConfig.Monitoring.DEFAULT_LATITUDE/LONGITUDE (Mumbai)
 *  - START_NOT_STICKY → START_STICKY (OS restarts service if killed)
 *  - Flood alert was unconditional → now only on risk levels in ALERT_RISK_LEVELS
 *  - Deprecated activeNetworkInfo.isConnected → NetworkCapabilities (API 23+)
 *  - Reverse-geocode API call removed (backend handles it internally)
 *  - Duplicate hasNotificationPermission() helper removed
 */
package com.example.xai_flows.core.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.xai_flows.MainActivity
import com.example.xai_flows.R
import com.example.xai_flows.core.cache.CacheManager
import com.example.xai_flows.core.config.AppConfig
import com.example.xai_flows.core.data.api.ApiClient
import com.example.xai_flows.core.data.models.PredictFloodRequest
import com.example.xai_flows.core.data.repository.FloodRepository
import com.google.gson.Gson
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import kotlin.coroutines.coroutineContext

// ─── Broadcast contract (single source of truth) ──────────────────────────────

/**
 * Intent action strings and extra keys shared between
 * RealTimeMonitoringService and PredictionScreen.
 * Import this object in any file that registers or sends these broadcasts.
 */
object RealTimeMonitoringContract {
    const val ACTION_LATEST_IMAGE  = "com.example.xai_flows.ACTION_LATEST_IMAGE"
    const val ACTION_PREDICT_FLOOD = "com.example.xai_flows.ACTION_PREDICT_FLOOD"
    const val ACTION_ERROR         = "com.example.xai_flows.ACTION_ERROR"
    const val ACTION_STATUS        = "com.example.xai_flows.ACTION_STATUS"

    const val EXTRA_DATA   = "extra_data"
    const val EXTRA_ERROR  = "extra_error"
    const val EXTRA_STATUS = "extra_status"
}

// ─── Service ──────────────────────────────────────────────────────────────────

class RealTimeMonitoringService : Service() {

    private val TAG          = "RealTimeMonitoringSvc"
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val repository   = FloodRepository(ApiClient.apiService)
    private val gson         = Gson()

    /**
     * Geographic coordinates for weather lookup.
     * Defaulted from AppConfig; overridden by Intent extras on start.
     */
    private var lat: Double = AppConfig.Monitoring.DEFAULT_LATITUDE
    private var lon: Double = AppConfig.Monitoring.DEFAULT_LONGITUDE

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        Log.d(TAG, "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand triggered")

        // Accept coordinates from the calling Activity/Fragment
        lat = intent?.getDoubleExtra("latitude",  AppConfig.Monitoring.DEFAULT_LATITUDE)  ?: AppConfig.Monitoring.DEFAULT_LATITUDE
        lon = intent?.getDoubleExtra("longitude", AppConfig.Monitoring.DEFAULT_LONGITUDE) ?: AppConfig.Monitoring.DEFAULT_LONGITUDE

        startForeground(
            AppConfig.Notifications.ID_FOREGROUND_SERVICE,
            buildForegroundNotification("Starting real-time monitoring…")
        )

        serviceScope.launch { monitorLoop() }

        // START_STICKY: OS will restart the service with a null intent if killed
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "Service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ─── Monitoring loop ──────────────────────────────────────────────────────

    private suspend fun monitorLoop() {
        while (coroutineContext.isActive) {
            try {
                // Guard: internet required
                if (!isInternetAvailable(this@RealTimeMonitoringService)) {
                    broadcastError("No internet connection. Stopping monitoring.")
                    postErrorNotification("Monitoring stopped: No internet")
                    sendStatus("stopped")
                    delay(AppConfig.Monitoring.ERROR_STOP_DELAY_MS)
                    stopSelf()
                    break
                }

                // Guard: notification permission (Android 13+)
                if (!hasNotificationPermission(this@RealTimeMonitoringService)) {
                    broadcastError("Notification permission missing. Stopping monitoring.")
                    postErrorNotification("Monitoring stopped: Enable notifications")
                    sendStatus("stopped")
                    delay(AppConfig.Monitoring.ERROR_STOP_DELAY_MS)
                    stopSelf()
                    break
                }

                sendStatus("started")

                // Step 1: Fetch latest S3 drain image
                val latestImageResponse = repository.getLatestImage()
                broadcastUpdate(RealTimeMonitoringContract.ACTION_LATEST_IMAGE, latestImageResponse)

                // Step 2: Run full flood prediction pipeline
                // (backend handles reverse-geocoding and returns location in response)
                val imagePart = base64ToMultipart(latestImageResponse.imageBase64)
                val predictResponse = repository.predictFlood(
                    imagePart,
                    PredictFloodRequest(lon = lon, lat = lat)
                )
                CacheManager.savePredictFlood(predictResponse)
                broadcastUpdate(RealTimeMonitoringContract.ACTION_PREDICT_FLOOD, predictResponse)

                updateForegroundNotification("Monitoring active — last update OK")

                // Step 3: Fire alert only for configured risk levels (High / Moderate)
                val riskLevel = predictResponse.prediction.flood_risk
                if (riskLevel in AppConfig.Notifications.ALERT_RISK_LEVELS) {
                    val city   = predictResponse.location.city.ifBlank { "Unknown location" }
                    val reason = predictResponse.prediction.reason
                    postFloodAlert(
                        title      = "Flood Alert — $riskLevel Risk",
                        message    = "$reason — $city",
                        incidentId = "xaiflows_${System.currentTimeMillis()}",
                        fullScreen = riskLevel == AppConfig.Notifications.FULL_SCREEN_RISK_LEVEL
                    )
                    Log.w(TAG, "Flood alert posted: risk=$riskLevel city=$city")
                }

            } catch (e: Exception) {
                Log.e(TAG, "monitorLoop error: ${e.message}", e)
                postErrorNotification("Monitoring stopped: ${e.message}")
                broadcastError("Error: ${e.message}")
                sendStatus("stopped")
                delay(AppConfig.Monitoring.ERROR_STOP_DELAY_MS)
                stopSelf()
                break
            }

            // Wait before next cycle
            delay(AppConfig.Monitoring.INTERVAL_MS)
        }
    }

    // ─── Notification helpers ─────────────────────────────────────────────────

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannels() {
        val monitoringChannel = NotificationChannel(
            AppConfig.Notifications.CHANNEL_MONITORING,
            "Monitoring",
            AppConfig.Notifications.IMPORTANCE_MONITORING
        )

        val errorChannel = NotificationChannel(
            AppConfig.Notifications.CHANNEL_ERROR,
            "Monitoring Errors",
            AppConfig.Notifications.IMPORTANCE_ERROR
        )

        val floodAlertChannel = NotificationChannel(
            AppConfig.Notifications.CHANNEL_FLOOD_ALERT,
            "Flood Alert (Urgent)",
            AppConfig.Notifications.IMPORTANCE_FLOOD
        ).apply {
            description          = "Immediate flood risk notification"
            enableVibration(true)
            vibrationPattern     = AppConfig.Notifications.FLOOD_VIBRATION_PATTERN
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            setSound(
                alarmUri,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }

        getSystemService(NotificationManager::class.java)
            .createNotificationChannels(listOf(monitoringChannel, errorChannel, floodAlertChannel))
    }

    private fun buildForegroundNotification(content: String): Notification =
        NotificationCompat.Builder(this, AppConfig.Notifications.CHANNEL_MONITORING)
            .setContentTitle("XAI-Flows Monitoring")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()

    private fun updateForegroundNotification(content: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(AppConfig.Notifications.ID_FOREGROUND_SERVICE, buildForegroundNotification(content))
    }

    private fun postErrorNotification(message: String) {
        val notification = NotificationCompat.Builder(this, AppConfig.Notifications.CHANNEL_ERROR)
            .setContentTitle("XAI-Flows Monitoring Error")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(AppConfig.Notifications.ID_ERROR, notification)
    }

    @SuppressLint("MissingPermission") // permission checked before calling
    private fun postFloodAlert(
        title: String,
        message: String,
        incidentId: String,
        fullScreen: Boolean = false
    ) {
        val contentPi = PendingIntent.getActivity(
            this, 100,
            Intent(this, MainActivity::class.java).putExtra("incidentId", incidentId),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, AppConfig.Notifications.CHANNEL_FLOOD_ALERT)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setColor(AppConfig.Notifications.FLOOD_ACCENT_COLOR)
            .setContentTitle("🚨 $title")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(AppConfig.Notifications.FLOOD_ALERT_PRIORITY)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setContentIntent(contentPi)

        if (fullScreen) {
            val fsPi = PendingIntent.getActivity(
                this, 101,
                Intent(this, MainActivity::class.java).putExtra("incidentId", incidentId),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.setFullScreenIntent(fsPi, true)
        }

        NotificationManagerCompat.from(this).notify(incidentId.hashCode(), builder.build())
    }

    // ─── Broadcast helpers ────────────────────────────────────────────────────

    private fun broadcastUpdate(action: String, data: Any) {
        sendBroadcast(Intent(action).apply {
            putExtra(RealTimeMonitoringContract.EXTRA_DATA, gson.toJson(data))
        })
    }

    private fun broadcastError(message: String) {
        sendBroadcast(Intent(RealTimeMonitoringContract.ACTION_ERROR).apply {
            putExtra(RealTimeMonitoringContract.EXTRA_ERROR, message)
        })
    }

    private fun sendStatus(status: String) {
        CacheManager.saveMonitoringState(status == "started")
        sendBroadcast(Intent(RealTimeMonitoringContract.ACTION_STATUS).apply {
            putExtra(RealTimeMonitoringContract.EXTRA_STATUS, status)
        })
    }

    // ─── Utility ─────────────────────────────────────────────────────────────

    /**
     * Converts a base64 string (from S3) to a Retrofit MultipartBody.Part.
     * Strips the "data:image/jpeg;base64," prefix if present.
     */
    private fun base64ToMultipart(base64: String, fileName: String = "image.jpg"): MultipartBody.Part {
        val clean        = base64.substringAfter("base64,", base64)
        val decodedBytes = android.util.Base64.decode(clean, android.util.Base64.DEFAULT)
        val requestBody  = decodedBytes.toRequestBody("image/jpeg".toMediaType())
        return MultipartBody.Part.createFormData("image", fileName, requestBody)
    }

    /**
     * Returns true when the device has an active internet connection.
     * Uses NetworkCapabilities on API 23+ to avoid the deprecated
     * ConnectivityManager.activeNetworkInfo API.
     */
    private fun isInternetAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork ?: return false
            val caps    = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            cm.activeNetworkInfo?.isConnected == true
        }
    }

    /**
     * Returns true when POST_NOTIFICATIONS is granted.
     * Always returns true on API < 33 (permission did not exist).
     */
    private fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }
}
