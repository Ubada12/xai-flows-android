// RealTimeMonitoringService.kt
package com.example.xai_flows.core.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.xai_flows.R
import com.example.xai_flows.core.data.repository.FloodRepository
import com.example.xai_flows.core.data.api.ApiClient
import com.example.xai_flows.core.data.models.PredictFloodRequest
import com.example.xai_flows.utils.CacheManager
import com.google.gson.Gson
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import kotlin.coroutines.coroutineContext
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationManagerCompat
import com.example.xai_flows.MainActivity
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

fun hasNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true // permission not needed on Android 12L and below
    }
}

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

class RealTimeMonitoringService : Service() {

    private val TAG = "RealTimeMonitoringSvc"
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val repository = FloodRepository(ApiClient.apiService)

    private var lat: Double = 51.5072
    private var lon: Double = 0.1276

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.d(TAG, "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service onStartCommand triggered")

        lat = intent?.getDoubleExtra("latitude", 51.5072) ?: 51.5072
        lon = intent?.getDoubleExtra("longitude", 0.1276) ?: 0.1276

        startForeground(1, buildNotification("Starting real-time monitoring..."))

        serviceScope.launch {
            monitorLoop()
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "Service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // --------------------------------------------
    // Monitoring Loop
    // --------------------------------------------
    private suspend fun monitorLoop() {
        while (coroutineContext.isActive) {
            try {
                // 1. Check conditions
                if (!isInternetAvailable(this@RealTimeMonitoringService)) {
                    broadcastError("No internet connection. Stopping monitoring.")
                    postErrorNotification("Monitoring stopped: No internet")
                    sendStatus("stopped")
                    delay(10000)
                    stopSelf()
                    break
                }
                if (!hasNotificationPermission(this@RealTimeMonitoringService)) {
                    broadcastError("Notification permission missing. Stopping monitoring.")
                    postErrorNotification("Monitoring stopped: Enable notifications")
                    sendStatus("stopped")
                    delay(10000)
                    stopSelf()
                    break
                }

                sendStatus("started")

                // 2. API Calls Sequentially
                // Fetch latest image
                val latestImageResponse = repository.getLatestImage()
                broadcastUpdate(RealTimeMonitoringContract.ACTION_LATEST_IMAGE, latestImageResponse)

                // Reverse geocode
                val reverseGeoResponse = repository.reverseGeocode(lat, lon)
                CacheManager.saveReverseGeo(reverseGeoResponse)
                broadcastUpdate(RealTimeMonitoringContract.ACTION_REVERSE_GEOCODE, reverseGeoResponse)

                // Convert base64 to multipart
                val imagePart = base64ToMultipart(latestImageResponse.imageBase64)
                val predictResponse = repository.predictFlood(imagePart, PredictFloodRequest(lon, lat))
                CacheManager.savePredictFlood(predictResponse)
                broadcastUpdate(RealTimeMonitoringContract.ACTION_PREDICT_FLOOD, predictResponse)

                // Update notification to indicate cycle success
                updateNotification("Monitoring active - Last update OK")

                postFloodAlert(
                    title = "Flood Emergency Alert",
                    message = "${predictResponse.prediction.reason} at ${reverseGeoResponse.city}",
                    incidentId = "powai_2025-08-09T15:42",
                    fullScreen = true
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error in monitoring cycle: ${e.message}", e)
                postErrorNotification("Monitoring stopped: ${e.message}")
                broadcastError("Error: ${e.message}")
                // Depending on severity: break or continue
                sendStatus("stopped")
                delay(10000)
                stopSelf()
                break
            }

            // 3. Wait for 20 seconds before next cycle
            delay(20_000)
        }
    }

    // --------------------------------------------
    // Helper functions
    // --------------------------------------------
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val monitoringChannel = NotificationChannel(
            "monitoring_channel",
            "Monitoring",
            NotificationManager.IMPORTANCE_LOW
        )
        val errorChannel = NotificationChannel(
            "error_channel",
            "Monitoring Errors",
            NotificationManager.IMPORTANCE_HIGH
        )

        // ✅ Flood Alert (max attention)
        val floodAlert = NotificationChannel(
            "flood_alert", "Flood Alert (Urgent)", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Immediate attention for flood risk/danger"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 700, 250, 700)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            setSound(
                uri,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(listOf(monitoringChannel, errorChannel, floodAlert))
    }

    private fun buildNotification(content: String): Notification {
        return NotificationCompat.Builder(this, "monitoring_channel")
            .setContentTitle("XAI-Flows Monitoring")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }

    private fun postErrorNotification(message: String) {
        val notification = NotificationCompat.Builder(this, "error_channel")
            .setContentTitle("XAI-Flows Monitoring Error")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)  // Can be dismissed
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(2, notification)  // Different ID from foreground
    }

    @SuppressLint("MissingPermission") // we check before calling
    private fun postFloodAlert(
        title: String,
        message: String,
        incidentId: String,
        fullScreen: Boolean = false
    ) {
        // Tap goes to app (adjust target activity if needed)
        val contentPi = PendingIntent.getActivity(
            this, 100,
            Intent(this, MainActivity::class.java).putExtra("incidentId", incidentId),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, "flood_alert")
            .setSmallIcon(R.drawable.ic_launcher_foreground)          // use a proper 24dp mono icon
            .setColor(0xFFDC2626.toInt())                      // red accent
            .setContentTitle("🚨 $title")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)      // for pre-26 devices
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setContentIntent(contentPi)

        if (fullScreen) {
            val fsPi = PendingIntent.getActivity(
                this, 101,
                Intent(this, MainActivity::class.java).putExtra("incidentId", incidentId),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.setFullScreenIntent(fsPi, true)            // use only for truly critical cases
        }

        NotificationManagerCompat.from(this)
            .notify(incidentId.hashCode(), builder.build())
    }

    private fun updateNotification(content: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = buildNotification(content)
        manager.notify(1, notification)
    }

    private val gson = Gson()

    private fun broadcastUpdate(action: String, data: Any) {
        val intent = Intent(action).apply {
            putExtra(RealTimeMonitoringContract.EXTRA_DATA, gson.toJson(data))
        }
        sendBroadcast(intent)
    }

    private fun broadcastError(message: String) {
        val intent = Intent(RealTimeMonitoringContract.ACTION_ERROR).apply {
            putExtra(RealTimeMonitoringContract.EXTRA_ERROR, message)
        }
        sendBroadcast(intent)
    }

    private fun sendStatus(status: String) {
        val intent = Intent(RealTimeMonitoringContract.ACTION_STATUS).apply {
            putExtra(RealTimeMonitoringContract.EXTRA_STATUS, status)
        }
        if (status.equals("stopped")) {
            CacheManager.saveMonitoringState(false)
        }
        else if (status.equals("started")) {
            CacheManager.saveMonitoringState(true)
        }
        sendBroadcast(intent)
    }

    private fun base64ToMultipart(base64: String, fileName: String = "image.jpg"): MultipartBody.Part {
        val cleanBase64 = base64.substringAfter("base64,", base64)
        val decodedBytes = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT)
        val requestBody = decodedBytes.toRequestBody("image/jpeg".toMediaType())
        return MultipartBody.Part.createFormData("image", fileName, requestBody)
    }

    private fun isInternetAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        return cm.activeNetworkInfo?.isConnected == true
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else true
    }
}

