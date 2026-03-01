package com.stanly.wallpapermovil.ui.screens

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Build
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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
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

    // Show "Where to set" dialog
    var showWhereDialog by remember { mutableStateOf(false) }

    // Player for live preview (muted – visual adjustment only)
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

    // ── Helper: save & launch wallpaper with chosen flag ─────────────────────
    fun saveAndSetWallpaper(whereFlag: Int) {
        scope.launch {
            // 1. Save config to DataStore
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
            // 2. Extract thumbnail in background
            val thumbPath = withContext(Dispatchers.IO) {
                ThumbnailHelper.extractAndSave(context, uri)
            } ?: ""

            // 3. Save to library
            val label = uri.substringAfterLast("/").take(28).ifBlank {
                "Fondo ${System.currentTimeMillis() % 10_000}"
            }
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

            // 4. Launch system live-wallpaper picker
            //    whereFlag: WallpaperManager.FLAG_SYSTEM | FLAG_LOCK (API 24+)
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(context, VideoWallpaperService::class.java)
                )
                // Pass the destination flag so the system picker pre-selects it (API 24+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    putExtra("which", whereFlag)
                }
            }
            context.startActivity(intent)
            onSaved()
        }
    }

    // ── "Where to set" dialog ─────────────────────────────────────────────────
    if (showWhereDialog) {
        AlertDialog(
            onDismissRequest = { showWhereDialog = false },
            title = { Text("¿Dónde establecer el fondo?") },
            text  = { Text("Elige en qué pantalla quieres aplicar el fondo animado.") },
            confirmButton = {},
            dismissButton = {},
            // Custom button layout so we get three choices
            icon = { Icon(Icons.Default.Wallpaper, contentDescription = null) }
        )
        // Overlay with three buttons (AlertDialog doesn't natively support 3 buttons cleanly)
        AlertDialog(
            onDismissRequest = { showWhereDialog = false },
            title = { Text("¿Dónde establecer?") },
            text  = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            showWhereDialog = false
                            saveAndSetWallpaper(WallpaperManager.FLAG_SYSTEM)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Home, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Solo pantalla de inicio")
                    }
                    Button(
                        onClick = {
                            showWhereDialog = false
                            saveAndSetWallpaper(WallpaperManager.FLAG_LOCK)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Lock, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Solo pantalla de bloqueo")
                    }
                    Button(
                        onClick = {
                            showWhereDialog = false
                            saveAndSetWallpaper(
                                WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Wallpaper, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Ambas pantallas")
                    }
                    TextButton(
                        onClick = { showWhereDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) { Text("Cancelar") }
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }

    // ── Main scaffold ─────────────────────────────────────────────────────────
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

        // Main layout: video preview + bottom control panel
        Column(modifier = Modifier.fillMaxSize().padding(scaffoldPadding)) {

            // ── Video preview (takes remaining space) ─────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, gestureZoom, _ ->
                            zoom = (zoom * gestureZoom).coerceIn(1f, 4f)
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
                if (zoom <= 1.01f) {
                    Text(
                        "Pellizca para zoom · Arrastra para encuadrar",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                    )
                }
            }

            // ── Bottom control panel (Surface, no fixed height limit) ──────────
            Surface(tonalElevation = 8.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Zoom slider
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Zoom",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.width(44.dp)
                        )
                        Slider(
                            value = zoom,
                            onValueChange = { zoom = it },
                            valueRange = 1f..4f,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "%.1f×".format(zoom),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.width(38.dp)
                        )
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onBack,
                            modifier = Modifier.weight(1f).height(52.dp)
                        ) { Text("Cancelar") }

                        Button(
                            onClick = { showWhereDialog = true },
                            modifier = Modifier.weight(2f).height(52.dp)
                        ) {
                            Icon(Icons.Default.Wallpaper, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Guardar y Establecer")
                        }
                    }
                }
            }
        }
    }
}
