package com.example.xai_flows.ui.components.analytics

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xai_flows.ui.models.ConclusionInsight

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConclusionSection(conclusionInsights: List<ConclusionInsight>) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Section Title
        Text(
            text = "Key Findings & Recommendations",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2563EB),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        conclusionInsights.forEachIndexed { index, insight ->
            AnimatedConclusionCard(insight = insight, index = index)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnimatedConclusionCard(insight: ConclusionInsight, index: Int) {
    // Animation states
    val animatedAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 500, delayMillis = index * 150, easing = FastOutSlowInEasing)
    )
    val animatedOffsetY by animateDpAsState(
        targetValue = 0.dp,
        animationSpec = tween(durationMillis = 500, delayMillis = index * 150, easing = FastOutSlowInEasing)
    )

    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f)

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = animatedAlpha
                translationY = animatedOffsetY.value
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    }
                )
            }
    ) {
        Column(Modifier.padding(16.dp)) {

            // Icon + Title
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Animated Icon
                val iconScale by animateFloatAsState(
                    targetValue = 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                )
                Icon(
                    imageVector = insight.icon,
                    contentDescription = null,
                    tint = insight.iconTint,
                    modifier = Modifier
                        .size(22.dp)
                        .graphicsLayer(scaleX = iconScale, scaleY = iconScale)
                )
                Text(insight.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(Modifier.height(4.dp))

            // Description
            Text(insight.description, fontSize = 14.sp, color = Color.Gray, lineHeight = 18.sp)

            Spacer(Modifier.height(8.dp))

            // Recommendation with Gradient Accent
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFFDBEAFE), Color(0xFFEFF6FF))
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(10.dp)
            ) {
                Text(
                    text = "Recommendation: ${insight.recommendation}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1E3A8A)
                )
            }
        }
    }
}

