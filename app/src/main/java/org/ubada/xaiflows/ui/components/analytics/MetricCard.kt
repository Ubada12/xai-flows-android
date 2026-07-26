package org.ubada.xaiflows.ui.components.analytics

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ubada.xaiflows.ui.models.Metric
import androidx.compose.foundation.layout.FlowRow

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MetricCard(metric: Metric, onImageClick: (Int, String) -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(Modifier.padding(20.dp)) {

            // Category Badge (Top Left)
            CategoryBadge(metric.category)

            Spacer(Modifier.height(12.dp))

            // Title
            Text(
                text = metric.title,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = Color(0xFF1E293B) // Dark blue-gray
            )

            Spacer(Modifier.height(8.dp))

            // Description
            Text(
                text = metric.description,
                fontSize = 15.sp,
                color = Color(0xFF64748B) // Softer gray
            )

            Spacer(Modifier.height(16.dp))

            // Image
            Image(
                painter = painterResource(id = metric.image),
                contentDescription = metric.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clickable { onImageClick(metric.image, metric.title) }
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.height(20.dp))

            // Key Insights Section
            Text(
                text = "Key Insights",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = Color(0xFF1E293B)
            )

            Spacer(Modifier.height(6.dp))

            // Bullet List Styled
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                metric.keyInsights.forEach { insight ->
                    Row {
                        Text(
                            text = "•",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3B82F6), // Blue bullet
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = insight,
                            fontSize = 14.sp,
                            color = Color(0xFF475569),
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // Technical Details (unchanged)
            metric.technicalDetails?.let {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Performance Metrics",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = Color(0xFF1E293B)
                )

                Spacer(Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp) // space between rows)
                ) {
                    it.accuracy?.let { value -> MetricBadge("Accuracy", value) }
                    it.precision?.let { value -> MetricBadge("Precision", value) }
                    it.recall?.let { value -> MetricBadge("Recall", value) }
                }
            }
        }
    }
}

@Composable
fun CategoryBadge(category: String) {
    val (bgColor, textColor) = when (category.lowercase()) {
        "visualization" -> Color(0xFFE0F2FE) to Color(0xFF0369A1) // Light Blue
        "interpretation" -> Color(0xFFF3E8FF) to Color(0xFF7E22CE) // Purple
        "performance" -> Color(0xFFE0F7EC) to Color(0xFF065F46) // Green
        else -> Color(0xFFE5E7EB) to Color(0xFF374151) // Default gray
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = category.replaceFirstChar { it.uppercase() },
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}

