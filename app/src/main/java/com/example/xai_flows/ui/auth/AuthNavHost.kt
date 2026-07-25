/**
 * AuthNavHost.kt
 * ─────────────────────────────────────────────────────────────────────────────
 * Self-contained navigation graph for the three unauthenticated auth
 * screens (login/signup/verify-otp). Mounted by MainActivity whenever the
 * user taps into a gated feature while logged out (see AuthGateCard); once
 * login or OTP-verify succeeds, [onAuthenticated] fires and the caller
 * swaps back to the real app content — this graph never needs to know
 * what's on the other side of that call.
 *
 * Each screen resolves its own `viewModel()` by default, scoped to its own
 * back-stack entry — deliberately NOT one shared AuthViewModel across all
 * three, since nothing here needs cross-screen state: the one piece of
 * data that crosses a screen boundary (the email between Signup and
 * VerifyOtp) is passed as a plain nav argument instead, which is simpler
 * and survives process death the same way any nav-graph argument does.
 */
package com.example.xai_flows.ui.auth

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.xai_flows.ui.auth.screens.LoginScreen
import com.example.xai_flows.ui.auth.screens.SignupScreen
import com.example.xai_flows.ui.auth.screens.VerifyOtpScreen
import java.net.URLDecoder
import java.net.URLEncoder

private object AuthRoutes {
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val VERIFY_OTP = "verify_otp/{email}"
    fun verifyOtp(email: String) = "verify_otp/${URLEncoder.encode(email, "UTF-8")}"
}

/**
 * @param startAtSignup  true to open directly on the signup screen (e.g.
 *                        AuthGateCard's "Create account" button) instead of
 *                        login (its "Log in" button).
 * @param onAuthenticated called once a session exists — after login, or
 *                        after verify-otp completes and the user logs in
 *                        from the login screen it hands back to.
 */
@Composable
fun AuthNavHost(
    onAuthenticated: () -> Unit,
    startAtSignup: Boolean = false
) {
    val navController = rememberNavController()
    val startDestination = if (startAtSignup) AuthRoutes.SIGNUP else AuthRoutes.LOGIN

    NavHost(navController = navController, startDestination = startDestination) {
        composable(AuthRoutes.LOGIN) {
            LoginScreen(
                onLoginSuccess = onAuthenticated,
                onNavigateToSignup = { navController.navigate(AuthRoutes.SIGNUP) }
            )
        }
        composable(AuthRoutes.SIGNUP) {
            SignupScreen(
                onRegisterSuccess = { email ->
                    navController.navigate(AuthRoutes.verifyOtp(email))
                },
                onNavigateToLogin = {
                    navController.navigate(AuthRoutes.LOGIN) {
                        popUpTo(AuthRoutes.LOGIN) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(
            route = AuthRoutes.VERIFY_OTP,
            arguments = listOf(navArgument("email") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedEmail = backStackEntry.arguments?.getString("email").orEmpty()
            val email = URLDecoder.decode(encodedEmail, "UTF-8")
            VerifyOtpScreen(
                email = email,
                onVerified = {
                    // Account is active now, but the user still needs to
                    // log in to actually mint a token — hand back to a
                    // clean login screen rather than auto-logging them in
                    // (verify-otp intentionally returns no token, see
                    // backend/app/models/auth.py::VerifyOTPResponse).
                    navController.navigate(AuthRoutes.LOGIN) {
                        popUpTo(AuthRoutes.LOGIN) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}
