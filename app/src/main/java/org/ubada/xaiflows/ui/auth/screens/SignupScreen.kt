/**
 * SignupScreen.kt
 * ─────────────────────────────────────────────────────────────────────────────
 * Step 1 of the signup flow: name + email + password + confirm-password
 * (confirm is client-side only, never sent to the backend) + Turnstile
 * CAPTCHA -> POST /auth/register. On success the backend emails a 6-digit
 * OTP and this screen hands off to VerifyOtpScreen with the email carried
 * as a nav argument (see AuthNavHost) — the user isn't asked to retype it.
 */
package org.ubada.xaiflows.ui.auth.screens

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
import org.ubada.xaiflows.ui.auth.AuthUiState
import org.ubada.xaiflows.ui.auth.AuthValidation
import org.ubada.xaiflows.ui.auth.AuthViewModel
import org.ubada.xaiflows.ui.auth.TurnstileWebView
import org.ubada.xaiflows.ui.auth.components.AuthErrorBanner
import org.ubada.xaiflows.ui.auth.components.AuthPrimaryButton
import org.ubada.xaiflows.ui.auth.components.AuthTextField

@Composable
fun SignupScreen(
    onRegisterSuccess: (email: String) -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var touched by rememberSaveable { mutableStateOf(false) }
    var turnstileToken by remember { mutableStateOf<String?>(null) }
    var turnstileError by remember { mutableStateOf<String?>(null) }
    var turnstileResetKey by remember { mutableStateOf(0) }

    val uiState by viewModel.uiState.collectAsState()
    val isLoading = uiState is AuthUiState.Loading
    val serverError = (uiState as? AuthUiState.Error)?.message
    val displayedError = serverError ?: turnstileError

    val nameError = if (touched) AuthValidation.nameError(name) else null
    val emailError = if (touched && !AuthValidation.isValidEmail(email)) "Enter a valid email." else null
    val passwordError = if (touched) AuthValidation.registrationPasswordError(password) else null
    val confirmError = if (touched && confirmPassword != password) "Passwords don't match." else null

    LaunchedEffect(uiState) {
        val state = uiState
        if (state is AuthUiState.RegisterSuccess) onRegisterSuccess(state.email)
    }

    val isFormValid = AuthValidation.nameError(name) == null &&
        AuthValidation.isValidEmail(email) &&
        AuthValidation.registrationPasswordError(password) == null &&
        confirmPassword == password

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B))))
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))
        Text("Create your account", color = Color.White, style = MaterialTheme.typography.headlineSmall)
        Text(
            "Get real-time, explainable flood-risk alerts",
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
                value = name,
                onValueChange = { name = it },
                label = "Full name",
                errorText = nameError,
                enabled = !isLoading
            )
            Spacer(Modifier.height(12.dp))
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
            Spacer(Modifier.height(12.dp))
            AuthTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = "Confirm password",
                isPassword = true,
                errorText = confirmError,
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
                text = "Create account",
                onClick = {
                    touched = true
                    if (isFormValid && turnstileToken != null) {
                        viewModel.register(name.trim(), email.trim(), password, turnstileToken.orEmpty())
                    }
                },
                enabled = !isLoading,
                isLoading = isLoading
            )
        }

        Spacer(Modifier.height(20.dp))
        Row {
            Text("Already have an account? ", color = Color(0xFF9CA3AF))
            Text(
                "Log in",
                color = Color(0xFF60A5FA),
                modifier = Modifier.clickable(enabled = !isLoading) {
                    viewModel.resetState()
                    onNavigateToLogin()
                }
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}
