package org.ubada.xaiflows.ui.components.home

import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

@Composable
fun LoopingVideoPlayer(videoResId: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // Create ExoPlayer instance
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val uri = Uri.parse("android.resource://${context.packageName}/$videoResId")
            val mediaItem = MediaItem.fromUri(uri)
            setMediaItem(mediaItem)
            prepare()
            repeatMode = ExoPlayer.REPEAT_MODE_ALL  // Loop
            playWhenReady = true                     // Auto play
            volume = 0f                               // Muted
        }
    }

    // Clean up player when Composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    // Display PlayerView without controls
    AndroidView(
        modifier = modifier,
        factory = {
            PlayerView(it).apply {
                player = exoPlayer
                useController = false  // Hide controls
            }
        }
    )
}
