package org.ubada.xaiflows.ui.components.prediction

import android.util.Base64
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LiveFeed(
    isStreaming: Boolean,
    countdown: Int,
    isRefreshing: Boolean,
    imageBase64: String? = null,
    modifier: Modifier = Modifier
) {
    var showFullImage by remember { mutableStateOf(false) }

    // Decode Base64 to ImageBitmap
    val imageBitmap: ImageBitmap? = remember(imageBase64) {
        if (!imageBase64.isNullOrEmpty()) {
            try {
                // Directly decode raw base64 (no prefix expected from backend)
                val decodedBytes = Base64.decode(imageBase64, Base64.DEFAULT)
                Log.d("LiveFeed", "Image decoded successfully")
                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        } else null
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(enabled = isStreaming && imageBitmap != null) {
                    showFullImage = true
                }
        ) {
            if (isStreaming && imageBitmap != null) {
                // Show the live image
                Image(
                    bitmap = imageBitmap,
                    contentDescription = "Live Monitoring Feed",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Live badge
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Red)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .align(Alignment.TopStart)
                ) {
                    Text(
                        text = "LIVE",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }

                // Countdown or pulse indicator
                if (isStreaming) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x80000000))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        if (!isRefreshing) {
                            Text("Refresh in ${countdown}s", color = Color.White, fontSize = 12.sp)
                        } else {
                            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                            val alpha by infiniteTransition.animateFloat(
                                initialValue = 0.5f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(800, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "pulseAlpha"
                            )
                            Text(
                                text = "Refreshing…",
                                color = Color.White.copy(alpha = alpha),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            } else {
                // Show fallback text
                Text(
                    text = "No Live Feed",
                    color = Color.Gray,
                    fontSize = 16.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }

    // Fullscreen Overlay (reuse same decoded bitmap)
    if (showFullImage && imageBitmap != null) {
        if (imageBase64 != null) {
            ImageOverlay(
                imageUrl = imageBase64, // Updated ImageOverlay to accept bitmap directly
                title = "Live Monitoring Feed",
                onDismiss = { showFullImage = false }
            )
        }
    }
}
