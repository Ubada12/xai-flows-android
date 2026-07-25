/**
 * LoginScreen.kt
 * ─────────────────────────────────────────────────────────────────────────────
 * Email + password + Turnstile CAPTCHA -> POST /auth/login. On success,
 * AuthRepository has already published the access token into AuthSession
 * (see AuthViewModel.login) before AuthUiState.LoginSuccess is emitted, so
 * onLoginSuccess() here is purely a navigation signal, not a data hop.
 *
 * The Turnstile widget mounts alongside the form and resolves in the
 * background while the user is still typing — the submit button simply
 * stays disabled until a token has arrived (or a fresh one after the
 * previous one expired), same UX the web app's login page gets from its
 * visible widget.
 */
package com.example.xai_flows.ui.auth.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.xai_flows.ui.auth.AuthUiState
import com.example.xai_flows.ui.auth.AuthValidation
import com.example.xai_flows.ui.auth.AuthViewModel
import com.example.xai_flows.ui.auth.TurnstileWebView
import com.example.xai_flows.ui.auth.components.AuthErrorBanner
import com.example.xai_flows.ui.auth.components.AuthPrimaryButton
import com.example.xai_flows.ui.auth.components.AuthTextField

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToSignup: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var touched by rememberSaveable { mutableStateOf(false) }
    var turnstileToken by remember { mutableStateOf<String?>(null) }
    var turnstileError by remember { mutableStateOf<String?>(null) }
    var turnstileResetKey by remember { mutableStateOf(0) }

    val uiState by viewModel.uiState.collectAsState()
    val isLoading = uiState is AuthUiState.Loading
    val serverError = (uiState as? AuthUiState.Error)?.message
    val displayedError = serverError ?: turnstileError

    val emailError = if (touched && !AuthValidation.isValidEmail(email)) "Enter a valid email." else null
    val passwordError = if (touched) AuthValidation.loginPasswordError(password) else null

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.LoginSuccess) onLoginSuccess()
    }

    val canSubmit = AuthValidation.isValidEmail(email) &&
        AuthValidation.loginPasswordError(password) == null &&
        turnstileToken != null &&
        !isLoading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B))))
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))
        Text("Welcome back", color = Color.White, style = MaterialTheme.typography.headlineSmall)
        Text(
            "Log in to run live flood predictions",
            color = Color(0xFF9CA3AF),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            if (displayedError != null) {
                AuthErrorBanner(displayedError)
                Spacer(Modifier.height(12.dp))
            }

            AuthTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email",
                keyboardType = KeyboardType.Email,
                errorText = emailError,
                enabled = !isLoading
            )
            Spacer(Modifier.height(12.dp))
            AuthTextField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                isPassword = true,
                errorText = passwordError,
                enabled = !isLoading
            )

            Spacer(Modifier.height(16.dp))
            key(turnstileResetKey) {
                TurnstileWebView(
                    modifier = Modifier.fillMaxWidth(),
                    onToken = { turnstileToken = it; turnstileError = null },
                    onError = { turnstileToken = null; turnstileError = "Verification failed. Please retry." },
                    onExpired = { turnstileToken = null; turnstileError = "Verification expired. Please retry." }
                )
            }
            if (turnstileToken == null && turnstileError != null) {
                Text(
                    "Retry verification",
                    color = Color(0xFF60A5FA),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clickable {
                            turnstileError = null
                            turnstileResetKey += 1
                        }
                )
            }

            Spacer(Modifier.height(8.dp))
            AuthPrimaryButton(
                text = "Log in",
                onClick = {
                    touched = true
                    if (canSubmit) viewModel.login(email.trim(), password, turnstileToken.orEmpty())
                },
                enabled = !isLoading,
                isLoading = isLoading
            )
        }

        Spacer(Modifier.height(20.dp))
        Row {
            Text("Don't have an account? ", color = Color(0xFF9CA3AF))
            Text(
                "Create one",
                color = Color(0xFF60A5FA),
                modifier = Modifier.clickable(enabled = !isLoading) {
                    viewModel.resetState()
                    onNavigateToSignup()
                }
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}
