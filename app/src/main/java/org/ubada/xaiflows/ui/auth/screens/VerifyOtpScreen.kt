/**
 * VerifyOtpScreen.kt
 * ─────────────────────────────────────────────────────────────────────────────
 * Step 2 of signup: the 6-digit code POST /auth/register emailed, submitted
 * to POST /auth/verify-otp to activate the account. [email] arrives as a
 * navigation argument from SignupScreen (see AuthNavHost) rather than being
 * re-typed or re-read from ViewModel state, so it survives process death /
 * config change the same way any nav-graph argument does.
 *
 * No Turnstile challenge here — the backend only requires it on
 * /auth/register and /auth/login (see backend/app/api/v1/endpoints/auth.py);
 * verify-otp is guarded by the OTP itself plus its own rate limit instead.
 */
package org.ubada.xaiflows.ui.auth.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.ubada.xaiflows.core.config.AppConfig
import org.ubada.xaiflows.ui.auth.AuthUiState
import org.ubada.xaiflows.ui.auth.AuthValidation
import org.ubada.xaiflows.ui.auth.AuthViewModel
import org.ubada.xaiflows.ui.auth.components.AuthErrorBanner
import org.ubada.xaiflows.ui.auth.components.AuthPrimaryButton
import org.ubada.xaiflows.ui.auth.components.AuthTextField

@Composable
fun VerifyOtpScreen(
    email: String,
    onVerified: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var otp by rememberSaveable { mutableStateOf("") }
    var touched by rememberSaveable { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()
    val isLoading = uiState is AuthUiState.Loading
    val serverError = (uiState as? AuthUiState.Error)?.message
    val otpError = if (touched) AuthValidation.otpError(otp) else null

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.VerifyOtpSuccess) onVerified()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B))))
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))
        Text("Verify your email", color = Color.White, style = MaterialTheme.typography.headlineSmall)
        Text(
            "Enter the ${AppConfig.Auth.OTP_LENGTH}-digit code sent to $email",
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
            if (serverError != null) {
                AuthErrorBanner(serverError)
                Spacer(Modifier.height(12.dp))
            }

            AuthTextField(
                value = otp,
                onValueChange = { input ->
                    if (input.length <= AppConfig.Auth.OTP_LENGTH && input.all(Char::isDigit)) {
                        otp = input
                    }
                },
                label = "Verification code",
                keyboardType = KeyboardType.NumberPassword,
                errorText = otpError,
                enabled = !isLoading
            )

            Spacer(Modifier.height(16.dp))
            AuthPrimaryButton(
                text = "Verify",
                onClick = {
                    touched = true
                    if (AuthValidation.otpError(otp) == null) {
                        viewModel.verifyOtp(email, otp)
                    }
                },
                enabled = !isLoading,
                isLoading = isLoading
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}
