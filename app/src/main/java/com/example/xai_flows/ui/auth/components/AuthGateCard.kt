/**
 * AuthGateCard.kt
 * ─────────────────────────────────────────────────────────────────────────────
 * Shown in place of a screen that requires a logged-in session — today
 * that's PredictionScreen, since the backend requires a Bearer token on
 * both /predict-flood/ and /get-latest-s3-image (see
 * backend/app/auth/dependencies.py::get_current_user). Mirrors the
 * frontend's features/predictions/components/auth-gate-card.tsx: same
 * message, same two actions, rendered inline instead of redirecting away,
 * so the user stays on the tab they tapped and sees exactly why the
 * content isn't there yet.
 */
package com.example.xai_flows.ui.auth.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun AuthGateCard(
    onLoginClick: () -> Unit,
    onSignupClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(20.dp))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(Color(0xFFEFF6FF), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = Color(0xFF2563EB))
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = "Sign in to access live predictions",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF111827),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Real-time flood risk analysis is available to registered users. " +
                "Log in or create a free account to run predictions on live drainage camera images.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF6B7280),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onLoginClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A))
            ) {
                Text("Log in", color = Color.White)
            }
            OutlinedButton(onClick = onSignupClick) {
                Text("Create account")
            }
        }
    }
}
