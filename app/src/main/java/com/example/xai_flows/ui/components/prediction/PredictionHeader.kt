package com.example.xai_flows.ui.components.prediction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PredictionHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Icon with subtle animation (MUI WaterDrop)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(80.dp)
                .background(Color.White.copy(alpha = 0.08f), CircleShape)
                .shadow(4.dp, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Rounded.Waves,
                contentDescription = "Flood Icon",
                tint = Color(0xFF3B82F6),
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Gradient Title (Premium Style)
        Text(
            text = "Flood Prediction System",
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer(alpha = 0.99f),
            color = Color.Unspecified, // Force gradient instead of solid color
            style = androidx.compose.ui.text.TextStyle(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF3B82F6), Color(0xFF9333EA))
                )
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "AI-powered real-time flood monitoring & insights",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center
        )
    }
}