package com.stanly.wallpapermovil

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stanly.wallpapermovil.ui.theme.WallpaperMovilTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WallpaperMovilTheme {
                WallpaperScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Observe persisted preferences
    val savedConfig by WallpaperPreferences
        .getConfig(context)
        .collectAsStateWithLifecycle(initialValue = WallpaperConfig())

    // Local UI state – seeded from DataStore but editable before saving
    var urlText by remember(savedConfig.videoUri) { mutableStateOf(savedConfig.videoUri) }
    var isMuted by remember(savedConfig.isMuted) { mutableStateOf(savedConfig.isMuted) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }

    // ── File picker ──────────────────────────────────────────────────────────
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // Take persistent read permission so the Service can access the URI
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Some providers don't offer persistable permissions; fall back gracefully
            }
            selectedUri = it
            urlText = it.toString()
        }
    }

    // ── Scaffold ─────────────────────────────────────────────────────────────
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("WallpaperMovil", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Header ───────────────────────────────────────────────────────
            Icon(
                imageVector = Icons.Default.Wallpaper,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Configura tu fondo de pantalla animado",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            // ── URL input ────────────────────────────────────────────────────
            OutlinedTextField(
                value = urlText,
                onValueChange = { urlText = it; selectedUri = null },
                label = { Text("URL del video") },
                placeholder = { Text("https://ejemplo.com/video.mp4") },
                leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                supportingText = { Text("Introduce una URL directa a un archivo MP4 o MKV") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // ── Local file picker ────────────────────────────────────────────
            OutlinedButton(
                onClick = { filePicker.launch("video/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Elegir video local")
            }

            if (selectedUri != null) {
                Text(
                    text = "📂 ${selectedUri?.lastPathSegment ?: "Archivo seleccionado"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            HorizontalDivider()

            // ── Mute switch ──────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text("Silenciar video", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = if (isMuted) "Sin sonido" else "Con sonido",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(checked = isMuted, onCheckedChange = { isMuted = it })
            }

            Spacer(Modifier.height(8.dp))

            // ── Set Wallpaper button ─────────────────────────────────────────
            Button(
                onClick = {
                    if (urlText.isBlank()) {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                "⚠️ Ingresa una URL o elige un video local primero"
                            )
                        }
                        return@Button
                    }
                    scope.launch {
                        // 1. Persist configuration
                        WallpaperPreferences.saveConfig(
                            context,
                            WallpaperConfig(videoUri = urlText, isMuted = isMuted)
                        )
                        // 2. Launch system live wallpaper picker pre-selected on this service
                        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                            putExtra(
                                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                                ComponentName(context, VideoWallpaperService::class.java)
                            )
                        }
                        context.startActivity(intent)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Wallpaper, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Establecer como Fondo",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
