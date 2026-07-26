package org.ubada.xaiflows.ui.theme

import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.animateFadeSlide(
    initialOffsetY: Float = 20f,
    delayMillis: Int = 0,
    durationMillis: Int = 500
): Modifier {
    val animatedAlpha = animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis, delayMillis, FastOutSlowInEasing),
        label = ""
    ).value

    val animatedOffset = animateFloatAsState(
        targetValue = 0f,
        animationSpec = tween(durationMillis, delayMillis, FastOutSlowInEasing),
        label = ""
    ).value

    return this
        .graphicsLayer {
            alpha = animatedAlpha
            translationY = initialOffsetY * (1 - animatedAlpha)
        }
}
