/**
 * MainActivity.kt
 * ─────────────────────────────────────────────────────────────────────────────
 * Single-activity entry point for XAI-FLOWS.
 *
 * Responsibilities:
 *   - Install the splash screen
 *   - Initialise CacheManager with application context
 *   - Validate required runtime permissions (location + notifications)
 *   - Render the top-level Compose scaffold with NavbarMobile
 *   - Simple in-memory page routing: "home" | "predictions" | "analytics"
 */
package com.example.xai_flows

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.xai_flows.core.cache.CacheManager
import com.example.xai_flows.core.permissions.PermissionManager
import com.example.xai_flows.ui.components.common.NavbarMobile
import com.example.xai_flows.ui.screens.AnalyticsScreen
import com.example.xai_flows.ui.screens.HomePage
import com.example.xai_flows.ui.screens.PredictionScreen
import com.example.xai_flows.ui.theme.XAIFLOWSTheme
import com.google.accompanist.systemuicontroller.rememberSystemUiController

class MainActivity : ComponentActivity() {

    /**
     * Launched when the user returns from the app-settings screen.
     * If permissions are still missing after returning, PermissionManager
     * will exit the app gracefully.
     */
    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        PermissionManager.handleReturnFromSettings(this)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Initialise SharedPreferences cache before any composable runs
        CacheManager.init(this)

        enableEdgeToEdge()
        setContent { XAIFLOWSTheme { MainScreen() } }

        // Check required permissions; redirect to settings if any are missing
        PermissionManager.validateAllPermissions(
            activity   = this,
            onRedirect = { redirectToSettings() }
        )
    }

    /** Opens the app details screen in system Settings. */
    private fun redirectToSettings() {
        settingsLauncher.launch(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
        )
    }
}

// ─── Top-level screen ─────────────────────────────────────────────────────────

@RequiresApi(Build.VERSION_CODES.O)
@Preview
@Composable
fun MainScreen() {
    // Tint the status bar to match the navbar dark background
    val systemUiController = rememberSystemUiController()
    SideEffect {
        systemUiController.setStatusBarColor(
            color     = Color(0xFF0F172A),
            darkIcons = false  // white icons on dark background
        )
    }

    // Simple in-memory navigation state — no NavController overhead needed
    val page = remember { mutableStateOf("home") }

    Scaffold(
        topBar = {
            Box(modifier = Modifier.statusBarsPadding()) {
                NavbarMobile(
                    onHomeClick        = { page.value = "home" },
                    onPredictionsClick = { page.value = "predictions" },
                    onAnalyticsClick   = { page.value = "analytics" }
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                // Converts footer route strings to page keys used by this when-expression.
                val handleNavigate: (String) -> Unit = { route ->
                    page.value = when (route) {
                        "/"            -> "home"
                        "/predictions" -> "predictions"
                        "/analytics"   -> "analytics"
                        else           -> page.value   // unknown route — stay put
                    }
                }

                when (page.value) {
                    "home"        -> HomePage(
                        modifier                = Modifier.fillMaxSize(),
                        onNavigateToPredictions = { page.value = "predictions" },
                        onNavigate              = handleNavigate
                    )
                    "analytics"   -> AnalyticsScreen(onNavigate = handleNavigate)
                    "predictions" -> PredictionScreen()
                }
            }
        }
    }
}
