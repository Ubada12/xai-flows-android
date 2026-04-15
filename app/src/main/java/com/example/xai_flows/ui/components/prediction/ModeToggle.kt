package com.example.xai_flows.ui.components.prediction

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ModeToggle(
    isManualMode: Boolean,
    onToggle: () -> Unit
) {
    val knobOffset by animateDpAsState(
        targetValue = if (isManualMode) 32.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "knobAnim"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Real-time",
            color = if (!isManualMode) Color.Blue else Color.Gray,
            fontWeight = if (!isManualMode) FontWeight.Bold else FontWeight.Normal,
            fontSize = 16.sp
        )

        Box(
            modifier = Modifier
                .width(64.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = if (isManualMode)
                            listOf(Color(0xFF22C55E), Color(0xFF16A34A))
                        else
                            listOf(Color(0xFF3B82F6), Color(0xFF2563EB))
                    )
                )
                .clickable { onToggle() },
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .offset(x = knobOffset)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }

        Text(
            "Manual",
            color = if (isManualMode) Color(0xFF16A34A) else Color.Gray,
            fontWeight = if (isManualMode) FontWeight.Bold else FontWeight.Normal,
            fontSize = 16.sp
        )
    }
}
