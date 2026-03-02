package com.stanly.wallpapermovil.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import com.stanly.wallpapermovil.WallpaperViewModel
import kotlinx.coroutines.launch

/**
 * Screen where the user enters a video URL or picks a local file,
 * toggles mute, and proceeds to the Adjust screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWallpaperScreen(
    viewModel: WallpaperViewModel,
    onBack: () -> Unit,
    onPreviewAndAdjust: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var urlText by remember { mutableStateOf("") }
    var isMuted by remember { mutableStateOf(true) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                // This now actually persists because OpenDocument grants the flag
                context.contentResolver.takePersistableUriPermission(
                    it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) { /* catch just in case */ }
            selectedUri = it
            urlText = it.toString()
        }
    }


    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Agregar Fondo") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
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
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── URL directa ───────────────────────────────────────────────────
            Text("URL directa del video", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(
                value = urlText,
                onValueChange = { urlText = it; selectedUri = null },
                label = { Text("URL del video") },
                placeholder = { Text("https://ejemplo.com/video.mp4") },
                leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                supportingText = { Text("Pega una URL directa a un MP4 o MKV") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text(
                "— o elige un archivo local —",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            // ── Selector de archivo ───────────────────────────────────────────
            OutlinedButton(
                onClick = { filePicker.launch(arrayOf("video/*")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Elegir video de la galería")
            }
            selectedUri?.let {
                Text(
                    "📂 ${it.lastPathSegment ?: "Archivo seleccionado"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            HorizontalDivider()

            // ── Mute switch ───────────────────────────────────────────────────
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
                        if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text("Silenciar video", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            if (isMuted) "Sin sonido" else "Con sonido",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(checked = isMuted, onCheckedChange = { isMuted = it })
            }

            Spacer(Modifier.height(8.dp))

            // ── Botón de continuar ────────────────────────────────────────────
            Button(
                onClick = {
                    if (urlText.isBlank()) {
                        scope.launch {
                            snackbarHostState.showSnackbar("⚠️ Ingresa una URL o elige un video")
                        }
                        return@Button
                    }
                    viewModel.pendingUri = urlText
                    viewModel.pendingMuted = isMuted
                    onPreviewAndAdjust()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Vista previa y ajustar recorte →", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
