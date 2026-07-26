package org.ubada.xaiflows.ui.components.prediction

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ubada.xaiflows.ui.models.PredictionData
import kotlinx.coroutines.delay
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Home

// Flood status enum for clarity
enum class FloodStatus(val label: String, val color: Color) {
    NO_FLOOD("No Flood Detected", Color(0xFF22C55E)),     // Green
    FLOOD_DETECTED("Flood Detected", Color(0xFFEF4444)),  // Red
    SEMI_FLOOD("Semi Flood Detected", Color(0xFFFACC15))  // Yellow
}

@Composable
fun PredictionCard(
    prediction: PredictionData,
    modifier: Modifier = Modifier
) {
    val newStatus = when (prediction.drainBlockage) {
        1 -> FloodStatus.NO_FLOOD
        0 -> FloodStatus.FLOOD_DETECTED
        else -> FloodStatus.SEMI_FLOOD
    }

    var displayedStatus by remember { mutableStateOf(newStatus) }
    var rotation by remember { mutableStateOf(0f) }

    // Animate rotation
    val animRotation by animateFloatAsState(
        targetValue = rotation,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "rotation"
    )

    // Trigger flip only when status changes
    LaunchedEffect(newStatus) {
        if (newStatus != displayedStatus) {
            // First half: rotate to 90 (hide)
            rotation = 90f
            delay(450)
            displayedStatus = newStatus // swap content at midpoint
            // Second half: rotate back to 0 (reveal)
            rotation = 0f
        }
    }

    // Glow pulse animation (unchanged)
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .graphicsLayer {
                rotationY = animRotation
                cameraDistance = 12 * density // more realistic 3D perspective
            }
    ) {
        // Glow background
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(displayedStatus.color.copy(alpha = glowAlpha), Color.Transparent),
                        radius = 600f
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
        )

        // Card content
        PredictionCardContent(displayedStatus, prediction)
    }
}

@Composable
private fun PredictionCardContent(floodStatus: FloodStatus, prediction: PredictionData) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier.shadow(12.dp, RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status icon
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(50))
                    .background(floodStatus.color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = floodStatus.color.copy(alpha = 0.3f),
                        radius = size.minDimension / 2.2f
                    )
                    drawCircle(
                        color = floodStatus.color,
                        radius = size.minDimension / 3f
                    )
                }
            }

            // Status label
            Text(
                text = floodStatus.label,
                color = floodStatus.color,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            // Risk badge
            Surface(
                color = floodStatus.color.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = "Risk Level: ${prediction.floodRisk}",
                    color = floodStatus.color,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            // Reason
            Text(
                text = prediction.reason,
                color = Color.Gray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            // Probability
            Text(
                text = "Blockage Probability: ${prediction.drainBlockageProb}%",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF374151)
            )

            // Divider
            Divider(
                color = Color(0xFFE5E7EB),
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // City and Address Section
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                // City
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "City",
                        tint = Color(0xFF3B82F6), // blue accent
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "City: ${prediction.city}",
                        fontSize = 14.sp,
                        color = Color(0xFF374151),
                        fontWeight = FontWeight.Medium
                    )
                }

                // Address
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Address",
                        tint = Color(0xFF6B7280), // grayish icon
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = prediction.address,
                        fontSize = 13.sp,
                        color = Color.Gray,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
