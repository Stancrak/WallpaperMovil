package com.stanly.wallpapermovil.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stanly.wallpapermovil.WallpaperViewModel
import com.stanly.wallpapermovil.data.WallpaperItem
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    viewModel: WallpaperViewModel,
    onAddNew: () -> Unit
) {
    val wallpapers by viewModel.wallpapers.collectAsStateWithLifecycle()
    var itemToDelete by remember { mutableStateOf<WallpaperItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Fondos", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddNew) {
                Icon(Icons.Default.Add, contentDescription = "Agregar fondo")
            }
        }
    ) { padding ->

        if (wallpapers.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Wallpaper,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.outlineVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Tu biblioteca está vacía",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Toca + para agregar tu primer fondo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(padding)
            ) {
                items(wallpapers, key = { it.id }) { item ->
                    WallpaperCard(
                        item = item,
                        onApply = { viewModel.applyWallpaper(item) },
                        onDeleteRequest = { itemToDelete = item }
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Eliminar fondo") },
            text = { Text("¿Eliminar \"${item.label}\" de la biblioteca?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteFromLibrary(item)
                    itemToDelete = null
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) { Text("Cancelar") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WallpaperCard(
    item: WallpaperItem,
    onApply: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .combinedClickable(
                onClick = onApply,
                onLongClick = onDeleteRequest
            ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box {
            // Thumbnail
            val thumbFile = File(item.thumbnailPath)
            if (thumbFile.exists()) {
                val bmp = remember(item.thumbnailPath) {
                    BitmapFactory.decodeFile(item.thumbnailPath)
                }
                bmp?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = item.label,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.outline)
                }
            }

            // Mute badge
            if (item.isMuted) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            RoundedCornerShape(50)
                        )
                        .padding(4.dp)
                ) {
                    Icon(
                        Icons.Default.VolumeOff,
                        contentDescription = "Silenciado",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Label row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDeleteRequest,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
