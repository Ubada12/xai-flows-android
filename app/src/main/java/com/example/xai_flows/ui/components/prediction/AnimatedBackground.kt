package com.example.xai_flows.ui.components.prediction

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.*
import kotlin.random.Random

@Composable
fun AnimatedBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "bgAnimation")

    // Gradient shift animation
    val gradientShift = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradientShift"
    )

    // Particle data
    val particles = remember {
        List(15) {
            ParticleData(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                radius = Random.nextInt(10, 30),
                dx = Random.nextFloat() * 0.5f,
                dy = Random.nextFloat() * 0.5f
            )
        }
    }

    Canvas(modifier = Modifier
        .fillMaxSize()
        .background(
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF1E3A8A),
                    Color(0xFF3B82F6),
                    Color(0xFF9333EA)
                ),
                start = Offset(gradientShift.value, gradientShift.value),
                end = Offset(gradientShift.value + 500f, gradientShift.value + 500f)
            )
        )
    ) {
        // Draw particles
        particles.forEachIndexed { index, particle ->
            val animatedX = (particle.x * size.width + gradientShift.value * particle.dx) % size.width
            val animatedY = (particle.y * size.height + gradientShift.value * particle.dy) % size.height

            drawCircle(
                color = Color.White.copy(alpha = 0.15f),
                radius = particle.radius.dp.toPx(),
                center = Offset(animatedX, animatedY)
            )
        }
    }
}

data class ParticleData(
    val x: Float,
    val y: Float,
    val radius: Int,
    val dx: Float,
    val dy: Float
)
