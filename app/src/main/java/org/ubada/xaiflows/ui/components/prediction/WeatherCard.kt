/**
 * WeatherCard.kt
 * ─────────────────────────────────────────────────────────────────────────────
 * Composable card displaying a weather snapshot with animated metric tiles.
 *
 * Receives a WeatherData (UI model) and renders six weather metrics in a
 * vertical list with staggered fade-in animations.
 *
 * Fixed in this version:
 *   - Precipitation comparison was `== 0` (Int) → now `< 0.01` (Double)
 *     to handle the float type returned by the backend.
 *   - Precipitation tile now shows the actual rainfall amount (e.g., "2.3 mm")
 *     rather than the raw condition string when rain is detected.
 */
package org.ubada.xaiflows.ui.components.prediction

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ubada.xaiflows.ui.models.WeatherData
import kotlinx.coroutines.delay

/** Internal data holder for a single weather metric tile. */
data class WeatherMetric(
    val icon: ImageVector,
    val title: String,
    val primaryValue: String,
    val secondaryValue: String
)

@Composable
fun WeatherCard(
    weather: WeatherData,
    modifier: Modifier = Modifier
) {
    val gradientColors = listOf(Color(0xFF93C5FD), Color(0xFFC7D2FE), Color(0xFFEDE9FE))

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .background(Brush.linearGradient(gradientColors))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with animated weather icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnimatedWeatherIcon(condition = weather.weatherCondition)
                Text(
                    text = "Weather Conditions",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E3A8A)
                )
            }

            // Precipitation display: show amount (mm) when > 0, "None" when dry
            val precipDisplay = if (weather.precipitation < 0.01) "None"
                                else "${"%.1f".format(weather.precipitation)} mm"

            // Six animated metric tiles
            val metrics = listOf(
                WeatherMetric(
                    Icons.Filled.Thermostat, "Temperature",
                    "${weather.temp}°C", "(Feels ${weather.appTemp}°C)"
                ),
                WeatherMetric(
                    Icons.Filled.Air, "Wind",
                    "${weather.windSpeed} m/s", weather.windDirection
                ),
                WeatherMetric(
                    Icons.Filled.WaterDrop, "Humidity",
                    "${"%.0f".format(weather.humidity)}%", weather.cloudCoverage
                ),
                WeatherMetric(
                    Icons.Filled.Compress, "Pressure",
                    "${weather.pressure} mb", "UV ${weather.uv}"
                ),
                WeatherMetric(
                    Icons.Filled.Umbrella, "Precipitation",
                    precipDisplay, weather.weatherCondition
                ),
                WeatherMetric(
                    Icons.Filled.Visibility, "Visibility",
                    "${weather.visibility} km", weather.airState
                )
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                metrics.forEachIndexed { index, metric ->
                    AnimatedMetricTile(
                        icon       = metric.icon,
                        label      = metric.title,
                        value      = metric.primaryValue,
                        metadata   = metric.secondaryValue,
                        delayIndex = index
                    )
                }
            }
        }
    }
}

/**
 * Animated weather icon (emoji + subtle pulse) that adapts to the condition.
 */
@Composable
fun AnimatedWeatherIcon(condition: String) {
    val (icon, color) = when {
        condition.contains("rain",  ignoreCase = true) -> "🌧" to Color(0xFF3B82F6)
        condition.contains("cloud", ignoreCase = true) -> "☁️" to Color(0xFF6B7280)
        condition.contains("sun",   ignoreCase = true) ||
        condition.contains("clear", ignoreCase = true) -> "☀️" to Color(0xFFFBBF24)
        else                                           -> "🌡" to Color(0xFF9333EA)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "weatherIcon")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 1.15f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "weatherIconScale"
    )

    Box(
        modifier = Modifier
            .size(36.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale },
        contentAlignment = Alignment.Center
    ) {
        Text(text = icon, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

/**
 * Single metric tile with staggered fade-in + slide-up entry animation.
 *
 * @param delayIndex  Index in the list; each tile is delayed by 100 ms × index.
 */
@Composable
fun AnimatedMetricTile(
    icon: ImageVector,
    label: String,
    value: String,
    metadata: String,
    delayIndex: Int
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(delayIndex * 100L)
        visible = true
    }

    val alpha   by animateFloatAsState(if (visible) 1f else 0f,   tween(500, easing = LinearOutSlowInEasing),  label = "tileAlpha")
    val offsetY by animateDpAsState(if (visible) 0.dp else 20.dp, tween(500, easing = FastOutSlowInEasing),    label = "tileOffset")

    // Icon accent colour per label
    val iconColor = when (label) {
        "Temperature"   -> Color(0xFFFF7043)
        "Wind"          -> Color(0xFF0288D1)
        "Humidity"      -> Color(0xFF29B6F6)
        "Pressure"      -> Color(0xFF7E57C2)
        "Precipitation" -> Color(0xFF3F51B5)
        "Visibility"    -> Color(0xFF26A69A)
        else            -> Color(0xFF374151)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = offsetY)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.6f))
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .graphicsLayer { this.alpha = alpha }
    ) {
        // Icon + label row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(iconColor.copy(alpha = 0.4f), Color.Transparent),
                            radius = 40f
                        ),
                        shape = RoundedCornerShape(50)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = label, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF374151))
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Primary value (large)
        Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))

        Spacer(modifier = Modifier.height(2.dp))

        // Secondary metadata (small, muted)
        Text(text = metadata, fontSize = 12.sp, fontWeight = FontWeight.Normal, color = Color(0xFF6B7280))
    }
}
