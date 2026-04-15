package com.example.xai_flows.core.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat

object PermissionManager {

    private const val TAG = "PermissionManager"

    // Required permissions list
    private val REQUIRED_PERMISSIONS = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    fun validateAllPermissions(
        activity: Activity,
        onRedirect: () -> Unit
    ) {
        if (!arePermissionsGranted(activity)) {
            // Explain why permissions are needed (Toast for now)
            Toast.makeText(
                activity,
                "Permissions required: Location (Always) & Notifications to proceed.",
                Toast.LENGTH_LONG
            ).show()

            // Call redirect ONCE
            onRedirect()
        } else {
            Log.i(TAG, "All permissions granted ✔ Proceeding with app")
        }
    }

    fun handleReturnFromSettings(activity: Activity) {
        if (!arePermissionsGranted(activity)) {
            // Still not granted → show toast and exit (no redirect again)
            Toast.makeText(activity, "Permissions still missing. Exiting app.", Toast.LENGTH_LONG).show()
            activity.finishAffinity() // Exit completely
        } else {
            Log.i(TAG, "Permissions granted after returning from settings ✔")
        }
    }


    /**
     * Check if all required permissions are granted
     */
    private fun arePermissionsGranted(context: Context): Boolean {
        for (perm in REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Permission missing: $perm")
                return false
            }
        }
        return true
    }
}
