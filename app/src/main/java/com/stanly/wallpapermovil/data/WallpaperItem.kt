package com.stanly.wallpapermovil.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a saved wallpaper entry in the local library.
 */
@Entity(tableName = "wallpaper_items")
data class WallpaperItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    /** Content URI or direct URL of the video */
    val uri: String,
    /** Display label shown in the library grid */
    val label: String,
    /** Absolute path to the cached thumbnail JPEG */
    val thumbnailPath: String,
    val isMuted: Boolean = true,
    /** Zoom level 1.0 (full frame) – 4.0 (4× zoom) */
    val zoom: Float = 1f,
    /** Horizontal pan offset in NDC [-1, 1] */
    val offsetX: Float = 0f,
    /** Vertical pan offset in NDC [-1, 1] */
    val offsetY: Float = 0f,
    val addedAt: Long = System.currentTimeMillis()
)
