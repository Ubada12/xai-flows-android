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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.xai_flows.ui.theme.XAIFLOWSTheme
import com.example.xai_flows.core.permissions.PermissionManager
import com.example.xai_flows.ui.components.common.NavbarMobile
import com.example.xai_flows.ui.screens.HomePage
import androidx.compose.foundation.layout.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.SideEffect
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import androidx.compose.ui.graphics.Color
import com.example.xai_flows.ui.screens.AnalyticsScreen
import com.example.xai_flows.ui.screens.PredictionScreen
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.example.xai_flows.utils.CacheManager
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : ComponentActivity() {

    // Launcher for app settings — will trigger ONLY when user returns
    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Handle result after returning from settings
        PermissionManager.handleReturnFromSettings(this)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        CacheManager.init(this)
        enableEdgeToEdge()

        setContent {
            XAIFLOWSTheme {
                MainScreen()
            }
        }

        // Validate permissions and redirect if needed
        PermissionManager.validateAllPermissions(
            activity = this,
            onRedirect = { redirectToSettings() }
        )
    }

    /**
     * Opens App Settings via launcher (clean flow)
     */
    private fun redirectToSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        settingsLauncher.launch(intent)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview
@Composable
fun MainScreen() {
    // Controller to manage status bar color
    val systemUiController = rememberSystemUiController()
    val statusBarColor = Color(0xFF0F172A) // Example: Dark blue/black gradient start color
    val page = remember { mutableStateOf("home") }

    SideEffect {
        // Set status bar color and icons contrast
        systemUiController.setStatusBarColor(
            color = statusBarColor,
            darkIcons = false // false = white icons, true = dark icons
        )
    }

    Scaffold(
        topBar = {
            Box(modifier = Modifier.statusBarsPadding()) {
                NavbarMobile(
                    onHomeClick = { page.value = "home" },
                    onPredictionsClick = { page.value = "predictions" },
                    onAnalyticsClick = { page.value = "analytics" }
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
            // Scrollable HomePage - occupies all remaining space
            Box(
                modifier = Modifier
                    .weight(1f)      // Fill remaining height
                    .fillMaxWidth()
            ) {
                if (page.value == "home") {
                    HomePage(
                        modifier = Modifier.fillMaxSize(), // Pass fillMaxSize to LazyColumn
                        onNavigateToPredictions = {
                            page.value = "predictions"
                        }
                    )
                }
                else if (page.value == "analytics") {
                    AnalyticsScreen()
                }
                else if (page.value == "predictions") {
                    PredictionScreen()
                }
            }
        }
    }
}

