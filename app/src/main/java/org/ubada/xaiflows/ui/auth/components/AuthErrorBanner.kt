/**
 * AuthErrorBanner.kt
 * ─────────────────────────────────────────────────────────────────────────────
 * Inline error banner shown above the form fields. Fed by
 * AuthErrorParser.parse(throwable).message, or a Turnstile-specific message
 * when the CAPTCHA widget itself fails — either way, the screen just passes
 * a nullable String through and this renders nothing when it's null/blank.
 */
package org.ubada.xaiflows.ui.auth.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AuthErrorBanner(message: String?, modifier: Modifier = Modifier) {
    if (message.isNullOrBlank()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFFEE2E2), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.ErrorOutline,
            contentDescription = null,
            tint = Color(0xFFDC2626)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = message,
            color = Color(0xFF991B1B),
            style = MaterialTheme.typography.bodySmall
        )
    }
}
