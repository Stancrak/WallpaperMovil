package com.stanly.wallpapermovil

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

/**
 * Utility object for extracting and caching video thumbnail images.
 */
object ThumbnailHelper {

    private const val THUMB_DIR = "thumbnails"
    private const val THUMB_WIDTH = 480
    private const val THUMB_HEIGHT = 270  // 16:9

    /**
     * Extracts a frame at [timeUs] microseconds (default = 1 second) from the given
     * video [uri], scales it, saves it as a JPEG in the app's files directory, and
     * returns the absolute path. Returns null if extraction fails.
     */
    fun extractAndSave(
        context: Context,
        uri: String,
        timeUs: Long = 1_000_000L
    ): String? = runCatching {
        val retriever = MediaMetadataRetriever()
        if (uri.startsWith("content://") || uri.startsWith("file://")) {
            retriever.setDataSource(context, Uri.parse(uri))
        } else {
            // HTTP/HTTPS URL
            retriever.setDataSource(uri, emptyMap())
        }

        val raw = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        retriever.release()
        raw ?: return null

        // Scale to target size
        val scaled = Bitmap.createScaledBitmap(raw, THUMB_WIDTH, THUMB_HEIGHT, true)
        raw.recycle()

        // Save to files/thumbnails/
        val thumbDir = File(context.filesDir, THUMB_DIR).also { it.mkdirs() }
        val file = File(thumbDir, "thumb_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        scaled.recycle()
        file.absolutePath
    }.getOrNull()

    /** Deletes a cached thumbnail from disk. */
    fun delete(path: String) {
        File(path).takeIf { it.exists() }?.delete()
    }
}
