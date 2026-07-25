/**
 * MainActivity.kt
 * ─────────────────────────────────────────────────────────────────────────────
 * Single-activity entry point for XAI-FLOWS.
 *
 * Responsibilities:
 *   - Install the splash screen
 *   - Initialise CacheManager + TokenStore with application context
 *   - Validate required runtime permissions (location + notifications)
 *   - Silently restore a session on launch (AuthRepository.restoreSession —
 *     mirrors the frontend's bootstrapSession) so a previously logged-in
 *     user doesn't have to log in again every time the app is reopened
 *   - Render the top-level Compose scaffold with NavbarMobile
 *   - Gate PredictionScreen behind AuthGateCard/AuthNavHost when logged out
 *     (the backend requires a Bearer token there — see
 *     backend/app/auth/dependencies.py::get_current_user)
 *   - Simple in-memory page routing over core.navigation.AppRoute (no
 *     NavController overhead) — home/predictions/analytics plus nine
 *     native "site" screens (team/careers/media/events/webinars/faq/
 *     support/privacy/terms) reached from the footer, see AppRoute.kt
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.xai_flows.core.auth.AuthRepository
import com.example.xai_flows.core.auth.AuthSession
import com.example.xai_flows.core.auth.SessionState
import com.example.xai_flows.core.auth.TokenStore
import com.example.xai_flows.core.cache.CacheManager
import com.example.xai_flows.core.navigation.AppRoute
import com.example.xai_flows.core.permissions.PermissionManager
import com.example.xai_flows.ui.auth.AuthNavHost
import com.example.xai_flows.ui.auth.components.AuthGateCard
import com.example.xai_flows.ui.components.common.NavbarMobile
import com.example.xai_flows.ui.screens.AnalyticsScreen
import com.example.xai_flows.ui.screens.HomePage
import com.example.xai_flows.ui.screens.PredictionScreen
import com.example.xai_flows.ui.screens.site.CareersScreen
import com.example.xai_flows.ui.screens.site.EventsScreen
import com.example.xai_flows.ui.screens.site.FaqScreen
import com.example.xai_flows.ui.screens.site.MediaScreen
import com.example.xai_flows.ui.screens.site.PrivacyPolicyScreen
import com.example.xai_flows.ui.screens.site.SupportScreen
import com.example.xai_flows.ui.screens.site.TeamScreen
import com.example.xai_flows.ui.screens.site.TermsOfServiceScreen
import com.example.xai_flows.ui.screens.site.WebinarsScreen
import com.example.xai_flows.ui.theme.XAIFLOWSTheme
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.launch

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

        // Initialise SharedPreferences-backed stores before any composable runs
        CacheManager.init(this)
        TokenStore.init(this)

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

    val coroutineScope = rememberCoroutineScope()

    // ── Session bootstrap (mirrors the frontend's bootstrapSession) ─────────
    // Silently attempts /auth/refresh once on launch using the persisted
    // refresh_token cookie (AuthCookieJar/TokenStore). isRestoringSession
    // gates the gated screens so a previously logged-in user doesn't see a
    // flash of "please log in" before this resolves — same isInitializing
    // rule the frontend's AuthProvider follows.
    var isRestoringSession by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        AuthRepository.restoreSession()
        isRestoringSession = false
    }

    val sessionState by AuthSession.state.collectAsState()

    // Controls whether the auth flow (login/signup/verify-otp) is shown in
    // place of the normal page content, and which screen it opens on.
    var authFlowActive by remember { mutableStateOf(false) }
    var authFlowStartsAtSignup by remember { mutableStateOf(false) }

    fun openAuthFlow(startAtSignup: Boolean) {
        authFlowStartsAtSignup = startAtSignup
        authFlowActive = true
    }

    // Simple in-memory navigation state — no NavController overhead needed.
    // AppRoute (core/navigation/AppRoute.kt) is the single source of truth
    // for every path this can be; FooterData.kt's FooterLink.href values
    // are always AppRoute.path strings, resolved back below.
    val page = remember { mutableStateOf(AppRoute.HOME) }

    if (authFlowActive) {
        AuthNavHost(
            onAuthenticated = { authFlowActive = false },
            startAtSignup = authFlowStartsAtSignup
        )
        return
    }

    Scaffold(
        topBar = {
            Box(modifier = Modifier.statusBarsPadding()) {
                NavbarMobile(
                    onHomeClick        = { page.value = AppRoute.HOME },
                    onPredictionsClick = { page.value = AppRoute.PREDICTIONS },
                    onAnalyticsClick   = { page.value = AppRoute.ANALYTICS },
                    isLoggedIn         = sessionState is SessionState.LoggedIn,
                    userEmail          = (sessionState as? SessionState.LoggedIn)?.user?.email,
                    onLoginClick       = { openAuthFlow(startAtSignup = false) },
                    onLogoutClick      = { coroutineScope.launch { AuthRepository.logout() } }
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
                // Resolves a footer route string (a FooterLink.href, always
                // an AppRoute.path) back to the AppRoute it names. Unknown
                // paths are a no-op — stay on the current screen rather
                // than crash or silently open a browser (see AppRoute.kt's
                // doc comment for why "open a browser" used to be the
                // fallback here, and the bug that caused).
                val handleNavigate: (String) -> Unit = { route ->
                    AppRoute.fromPath(route)?.let { page.value = it }
                }

                // Every site screen's back button returns to Home — same
                // behaviour as every one of the frontend's site pages,
                // which all link "Back to Home" rather than a true
                // browser-style back stack (see SitePageScaffold.kt).
                val backToHome: () -> Unit = { page.value = AppRoute.HOME }

                when (page.value) {
                    AppRoute.HOME        -> HomePage(
                        modifier                = Modifier.fillMaxSize(),
                        onNavigateToPredictions = { page.value = AppRoute.PREDICTIONS },
                        onNavigate              = handleNavigate
                    )
                    AppRoute.ANALYTICS   -> AnalyticsScreen(onNavigate = handleNavigate)
                    AppRoute.PREDICTIONS -> when {
                        isRestoringSession -> Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }

                        sessionState is SessionState.LoggedIn -> PredictionScreen()

                        else -> Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AuthGateCard(
                                onLoginClick = { openAuthFlow(startAtSignup = false) },
                                onSignupClick = { openAuthFlow(startAtSignup = true) }
                            )
                        }
                    }

                    // ── Site pages (formerly opened the website — see
                    //    AppRoute.kt's doc comment) ──────────────────────
                    AppRoute.TEAM             -> TeamScreen(onBack = backToHome)
                    AppRoute.CAREERS          -> CareersScreen(onBack = backToHome)
                    AppRoute.MEDIA            -> MediaScreen(onBack = backToHome)
                    AppRoute.EVENTS           -> EventsScreen(onBack = backToHome)
                    AppRoute.WEBINARS         -> WebinarsScreen(onBack = backToHome)
                    AppRoute.FAQ              -> FaqScreen(onBack = backToHome)
                    AppRoute.SUPPORT          -> SupportScreen(onBack = backToHome)
                    AppRoute.PRIVACY_POLICY   -> PrivacyPolicyScreen(onBack = backToHome)
                    AppRoute.TERMS_OF_SERVICE -> TermsOfServiceScreen(onBack = backToHome)
                }
            }
        }
    }
}
