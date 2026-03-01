package com.stanly.wallpapermovil

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Full configuration for the active live wallpaper.
 *
 * @param videoUri  Absolute file path or direct URL of the video.
 * @param isMuted   Whether audio is silenced during playback.
 * @param zoom      Zoom level: 1.0 = full frame, 4.0 = 4× zoom.
 * @param offsetX   Horizontal pan in NDC [-1, 1]. 0 = center.
 * @param offsetY   Vertical pan in NDC [-1, 1]. 0 = center.
 */
data class WallpaperConfig(
    val videoUri: String = "",
    val isMuted: Boolean = true,
    val zoom: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f
)

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "wallpaper_prefs")

object WallpaperPreferences {

    private val VIDEO_URI_KEY  = stringPreferencesKey("video_uri")
    private val MUTE_KEY       = booleanPreferencesKey("mute")
    private val ZOOM_KEY       = floatPreferencesKey("zoom")
    private val OFFSET_X_KEY   = floatPreferencesKey("offset_x")
    private val OFFSET_Y_KEY   = floatPreferencesKey("offset_y")

    fun getConfig(context: Context): Flow<WallpaperConfig> =
        context.dataStore.data.map { prefs ->
            WallpaperConfig(
                videoUri  = prefs[VIDEO_URI_KEY]  ?: "",
                isMuted   = prefs[MUTE_KEY]       ?: true,
                zoom      = prefs[ZOOM_KEY]       ?: 1f,
                offsetX   = prefs[OFFSET_X_KEY]   ?: 0f,
                offsetY   = prefs[OFFSET_Y_KEY]   ?: 0f
            )
        }

    suspend fun saveConfig(context: Context, config: WallpaperConfig) {
        context.dataStore.edit { prefs ->
            prefs[VIDEO_URI_KEY]  = config.videoUri
            prefs[MUTE_KEY]       = config.isMuted
            prefs[ZOOM_KEY]       = config.zoom
            prefs[OFFSET_X_KEY]   = config.offsetX
            prefs[OFFSET_Y_KEY]   = config.offsetY
        }
    }
}
