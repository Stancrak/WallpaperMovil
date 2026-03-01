package com.stanly.wallpapermovil

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Data class representing the user's wallpaper configuration.
 *
 * @param videoUri  Absolute file path or direct URL of the video to play.
 * @param isMuted   Whether audio should be silenced during wallpaper playback.
 */
data class WallpaperConfig(
    val videoUri: String = "",
    val isMuted: Boolean = true
)

/** DataStore instance scoped to the application context. */
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "wallpaper_prefs")

object WallpaperPreferences {

    private val VIDEO_URI_KEY = stringPreferencesKey("video_uri")
    private val MUTE_KEY = booleanPreferencesKey("mute")

    /**
     * Returns a [Flow] of the current [WallpaperConfig].
     * Collects from DataStore; emits the default config if nothing is saved yet.
     */
    fun getConfig(context: Context): Flow<WallpaperConfig> =
        context.dataStore.data.map { prefs ->
            WallpaperConfig(
                videoUri = prefs[VIDEO_URI_KEY] ?: "",
                isMuted = prefs[MUTE_KEY] ?: true
            )
        }

    /**
     * Persists the given [WallpaperConfig] to DataStore.
     * Must be called from a coroutine (suspend function).
     */
    suspend fun saveConfig(context: Context, config: WallpaperConfig) {
        context.dataStore.edit { prefs ->
            prefs[VIDEO_URI_KEY] = config.videoUri
            prefs[MUTE_KEY] = config.isMuted
        }
    }
}
