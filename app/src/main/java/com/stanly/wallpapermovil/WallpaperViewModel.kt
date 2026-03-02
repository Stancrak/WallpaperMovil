package com.stanly.wallpapermovil

import android.app.Application
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stanly.wallpapermovil.data.WallpaperItem
import com.stanly.wallpapermovil.data.WallpaperRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Shared ViewModel for the wallpaper library.
 *
 * Holds a [pendingUri] and [pendingMuted] that the Add screen sets before
 * navigating to the Adjust screen, avoiding complex NavGraph argument encoding.
 */
class WallpaperViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = WallpaperRepository.getInstance(app)

    val wallpapers = repo.allWallpapers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    // ── Transient state passed between Add → Adjust screens ──────────────────
    var pendingUri: String = ""
    var pendingMuted: Boolean = true

    // ── Library operations ────────────────────────────────────────────────────

    fun addToLibrary(item: WallpaperItem) {
        viewModelScope.launch(Dispatchers.IO) { repo.add(item) }
    }

    fun deleteFromLibrary(item: WallpaperItem) {
        viewModelScope.launch(Dispatchers.IO) {
            ThumbnailHelper.delete(item.thumbnailPath)
            repo.delete(item)
        }
    }

    /** Saves [item]'s settings to DataStore and launches the system wallpaper picker. */
    fun applyWallpaper(item: WallpaperItem) {
        viewModelScope.launch {
            WallpaperPreferences.saveConfig(
                getApplication(),
                WallpaperConfig(
                    videoUri      = item.uri,
                    isMuted       = item.isMuted,
                    zoom          = item.zoom,
                    offsetX       = item.offsetX,
                    offsetY       = item.offsetY,
                    thumbnailPath = item.thumbnailPath
                )
            )
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(getApplication(), VideoWallpaperService::class.java)
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            getApplication<Application>().startActivity(intent)
        }
    }
}
