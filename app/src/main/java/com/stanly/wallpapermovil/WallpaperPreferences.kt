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

data class WallpaperConfig(
    val videoUri: String = "",
    val isMuted: Boolean = true,
    val zoom: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    /** Absolute path to the cached thumbnail JPEG — used for instant preview. */
    val thumbnailPath: String = ""
)

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "wallpaper_prefs")

object WallpaperPreferences {

    private val VIDEO_URI_KEY      = stringPreferencesKey("video_uri")
    private val MUTE_KEY           = booleanPreferencesKey("mute")
    private val ZOOM_KEY           = floatPreferencesKey("zoom")
    private val OFFSET_X_KEY       = floatPreferencesKey("offset_x")
    private val OFFSET_Y_KEY       = floatPreferencesKey("offset_y")
    private val THUMBNAIL_PATH_KEY = stringPreferencesKey("thumbnail_path")

    fun getConfig(context: Context): Flow<WallpaperConfig> =
        context.dataStore.data.map { prefs ->
            WallpaperConfig(
                videoUri      = prefs[VIDEO_URI_KEY]      ?: "",
                isMuted       = prefs[MUTE_KEY]           ?: true,
                zoom          = prefs[ZOOM_KEY]           ?: 1f,
                offsetX       = prefs[OFFSET_X_KEY]       ?: 0f,
                offsetY       = prefs[OFFSET_Y_KEY]       ?: 0f,
                thumbnailPath = prefs[THUMBNAIL_PATH_KEY] ?: ""
            )
        }

    suspend fun saveConfig(context: Context, config: WallpaperConfig) {
        context.dataStore.edit { prefs ->
            prefs[VIDEO_URI_KEY]      = config.videoUri
            prefs[MUTE_KEY]           = config.isMuted
            prefs[ZOOM_KEY]           = config.zoom
            prefs[OFFSET_X_KEY]       = config.offsetX
            prefs[OFFSET_Y_KEY]       = config.offsetY
            prefs[THUMBNAIL_PATH_KEY] = config.thumbnailPath
        }
    }
}
