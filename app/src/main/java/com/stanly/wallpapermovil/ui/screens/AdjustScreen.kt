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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
 * Preview screen where the user adjusts crop/zoom/pan before saving to the library.
 *
 * Gestures:
 *  - Pinch → zoom (1× to 4×)
 *  - Drag  → pan (constrained inside frame)
 *
 * The graphicsLayer transform gives a live visual preview of what will be
 * applied as a GL Crop effect in [VideoWallpaperService].
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

    // Crop/zoom state
    var zoom by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // ExoPlayer for the live preview (muted – this is just for visual adjustment)
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
                    // Reset button
                    IconButton(onClick = { zoom = 1f; offsetX = 0f; offsetY = 0f }) {
                        Icon(Icons.Default.CropFree, contentDescription = "Restablecer")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            BottomAppBar(tonalElevation = 8.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Zoom slider
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Zoom", style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.width(48.dp))
                        Slider(
                            value = zoom,
                            onValueChange = { zoom = it },
                            valueRange = 1f..4f,
                            steps = 29,
                            modifier = Modifier.weight(1f)
                        )
                        Text("%.1f×".format(zoom), style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.width(36.dp))
                    }

                    Spacer(Modifier.height(4.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onBack,
                            modifier = Modifier.weight(1f)
                        ) { Text("Cancelar") }

                        Button(
                            onClick = {
                                scope.launch {
                                    // 1. Guardar prefs
                                    val config = WallpaperConfig(
                                        videoUri = uri,
                                        isMuted  = isMuted,
                                        zoom     = zoom,
                                        offsetX  = offsetX,
                                        offsetY  = offsetY
                                    )
                                    WallpaperPreferences.saveConfig(context, config)

                                    // 2. Extraer thumbnail en background
                                    val thumbPath = withContext(Dispatchers.IO) {
                                        ThumbnailHelper.extractAndSave(context, uri)
                                    } ?: ""

                                    // 3. Guardar en biblioteca
                                    val label = uri.substringAfterLast("/").take(24)
                                        .ifBlank { "Fondo ${System.currentTimeMillis() % 1000}" }
                                    viewModel.addToLibrary(
                                        WallpaperItem(
                                            uri           = uri,
                                            label         = label,
                                            thumbnailPath = thumbPath,
                                            isMuted       = isMuted,
                                            zoom          = zoom,
                                            offsetX       = offsetX,
                                            offsetY       = offsetY
                                        )
                                    )

                                    // 4. Lanzar selector de fondo del sistema
                                    val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                                        putExtra(
                                            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                                            ComponentName(context, VideoWallpaperService::class.java)
                                        )
                                    }
                                    context.startActivity(intent)
                                    onSaved()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Wallpaper, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Guardar y Establecer")
                        }
                    }
                }
            }
        }
    ) { padding ->

        // ── Video preview with live zoom/pan visual feedback via graphicsLayer ─
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, gestureZoom, _ ->
                        // Update zoom
                        zoom = (zoom * gestureZoom).coerceIn(1f, 4f)

                        // Convert pixel pan to normalized offset
                        val halfSize = 1f / zoom
                        val dxNorm = pan.x / (size.width / 2f)
                        val dyNorm = pan.y / (size.height / 2f)
                        offsetX = (offsetX - dxNorm).coerceIn(-1f + halfSize, 1f - halfSize)
                        offsetY = (offsetY - dyNorm).coerceIn(-1f + halfSize, 1f - halfSize)
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

            // Hint overlay
            if (zoom == 1f) {
                Text(
                    "Pellizca para hacer zoom · Arrastra para encuadrar",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                )
            }
        }
    }
}
