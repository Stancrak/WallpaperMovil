package com.stanly.wallpapermovil.ui.screens

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.stanly.wallpapermovil.ThumbnailHelper
import com.stanly.wallpapermovil.VideoWallpaperService
import com.stanly.wallpapermovil.WallpaperConfig
import com.stanly.wallpapermovil.WallpaperPreferences
import com.stanly.wallpapermovil.WallpaperViewModel
import com.stanly.wallpapermovil.data.WallpaperItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Preview + crop/zoom/pan adjustment screen.
 *
 * Gestures:
 *  - Pinch → zoom (1× – 4×)
 *  - Drag  → pan (constrained inside visible frame)
 *
 * Tapping "Aplicar Fondo" saves config → library → launches system wallpaper
 * picker (which natively asks: home / lock / both on Android 12+).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdjustScreen(
    viewModel: WallpaperViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val uri = viewModel.pendingUri
    val isMuted = viewModel.pendingMuted

    var zoom by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Muted preview player
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = Player.REPEAT_MODE_ALL
            volume = 0f
            prepare()
            play()
        }
    }
    DisposableEffect(Unit) { onDispose { player.release() } }

    fun applyWallpaper() {
        scope.launch {
            // 1. Persist user config
            WallpaperPreferences.saveConfig(
                context,
                WallpaperConfig(
                    videoUri = uri,
                    isMuted  = isMuted,
                    zoom     = zoom,
                    offsetX  = offsetX,
                    offsetY  = offsetY
                )
            )
            // 2. Extract thumbnail (background thread)
            val thumbPath = withContext(Dispatchers.IO) {
                ThumbnailHelper.extractAndSave(context, uri)
            } ?: ""

            // 3. Add to library
            val label = uri.substringAfterLast("/").take(28)
                .ifBlank { "Fondo ${System.currentTimeMillis() % 10_000}" }
            viewModel.addToLibrary(
                WallpaperItem(
                    uri = uri, label = label, thumbnailPath = thumbPath,
                    isMuted = isMuted, zoom = zoom, offsetX = offsetX, offsetY = offsetY
                )
            )

            // 4. Launch system picker — they handle home / lock / both natively
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(context, VideoWallpaperService::class.java)
                )
            }
            context.startActivity(intent)
            onSaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustar Recorte") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    // Reset crop
                    IconButton(onClick = { zoom = 1f; offsetX = 0f; offsetY = 0f }) {
                        Icon(Icons.Default.CropFree, contentDescription = "Restablecer")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { scaffoldPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
        ) {

            // ── Video preview with pinch-to-zoom & pan ────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, gestureZoom, _ ->
                            zoom = (zoom * gestureZoom).coerceIn(1f, 4f)
                            val halfSize = 1f / zoom
                            offsetX = (offsetX - pan.x / (size.width / 2f))
                                .coerceIn(-1f + halfSize, 1f - halfSize)
                            offsetY = (offsetY - pan.y / (size.height / 2f))
                                .coerceIn(-1f + halfSize, 1f - halfSize)
                        }
                    }
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = player
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = zoom,
                            scaleY = zoom,
                            translationX = -offsetX * (zoom - 1f) * 300f,
                            translationY = -offsetY * (zoom - 1f) * 300f
                        )
                )

                if (zoom <= 1.01f) {
                    Text(
                        "Pellizca para zoom · Arrastra para encuadrar",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 6.dp)
                    )
                }
            }

            // ── Bottom controls panel ─────────────────────────────────────────
            Surface(tonalElevation = 8.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Zoom slider
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Zoom",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.width(44.dp))
                        Slider(
                            value = zoom,
                            onValueChange = { zoom = it },
                            valueRange = 1f..4f,
                            modifier = Modifier.weight(1f)
                        )
                        Text("%.1f×".format(zoom),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.width(38.dp))
                    }

                    // Action row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onBack,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        ) {
                            Text("Cancelar", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        Button(
                            onClick = ::applyWallpaper,
                            modifier = Modifier
                                .weight(2f)
                                .height(50.dp)
                        ) {
                            Icon(
                                Icons.Default.Wallpaper,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            Text(
                                "Aplicar Fondo",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}
