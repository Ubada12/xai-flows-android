package org.ubada.xaiflows.ui.components.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MetricBadge(label: String, value: Double) {
    Box(
        modifier = Modifier
            .background(Color(0xFFE0F2FE))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "$label: ${"%.2f".format(value)}",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1E3A8A)
        )
    }
}
