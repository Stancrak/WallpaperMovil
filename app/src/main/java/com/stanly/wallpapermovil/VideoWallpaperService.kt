package com.stanly.wallpapermovil

import android.graphics.BitmapFactory
import android.graphics.RectF
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Live Wallpaper service — ExoPlayer-based.
 *
 * Thumbnail-first strategy (eliminates black preview):
 *  1. When the surface is created, immediately draw the cached thumbnail
 *     from disk to the Canvas so the picker preview is never black.
 *  2. THEN attach the surface to ExoPlayer. Once ExoPlayer renders its
 *     first frame, it overwrites the thumbnail and the video plays live.
 *  3. playWhenReady = true + onVisibilityChanged pause/resume for battery.
 */
class VideoWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = VideoEngine()

    inner class VideoEngine : Engine() {

        private var player: ExoPlayer? = null
        private val scope = CoroutineScope(Dispatchers.Main + Job())
        private var isVisible = true
        private var currentUri = ""
        private var cachedThumbPath = ""

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            initPlayer()
            observePreferences()
        }

        /**
         * Draw thumbnail first, then give the surface to ExoPlayer.
         * This gives instant visual feedback and avoids the black preview.
         */
        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            scope.launch {
                // Load and draw thumbnail on IO → Main, before setVideoSurface
                drawThumbnailToHolder(holder)
                // Now hand the surface to ExoPlayer
                player?.setVideoSurface(holder.surface)
                if (isVisible) player?.play()
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            player?.setVideoSurface(holder.surface)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            player?.setVideoSurface(null)
            super.onSurfaceDestroyed(holder)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            isVisible = visible
            if (visible) player?.play() else player?.pause()
        }

        override fun onDestroy() {
            scope.cancel()
            player?.release()
            player = null
            super.onDestroy()
        }

        // ── Private helpers ───────────────────────────────────────────────────

        private fun initPlayer() {
            val audioAttrs = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build()

            player = ExoPlayer.Builder(this@VideoWallpaperService)
                .build()
                .also { exo ->
                    exo.setAudioAttributes(audioAttrs, false)
                    exo.repeatMode = Player.REPEAT_MODE_ALL
                    exo.volume = 0f
                    exo.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                    // Surface set in onSurfaceCreated (after thumbnail is drawn)
                    exo.playWhenReady = true
                }
        }

        private fun observePreferences() {
            scope.launch {
                WallpaperPreferences.getConfig(applicationContext)
                    .collectLatest { config ->
                        val exo = player ?: return@collectLatest

                        cachedThumbPath = config.thumbnailPath

                        if (config.videoUri.isNotBlank() && config.videoUri != currentUri) {
                            currentUri = config.videoUri
                            exo.setMediaItem(MediaItem.fromUri(config.videoUri))
                            exo.prepare()
                            if (isVisible) exo.play()
                        }

                        exo.volume = if (config.isMuted) 0f else 1f
                    }
            }
        }

        /**
         * Loads the cached thumbnail bitmap from disk on an IO thread and
         * draws it to the SurfaceHolder canvas. This fills the preview with
         * the last known frame instead of black while ExoPlayer buffers.
         */
        private suspend fun drawThumbnailToHolder(holder: SurfaceHolder) {
            val path = cachedThumbPath
            if (path.isBlank()) return

            val bmp = withContext(Dispatchers.IO) {
                runCatching { BitmapFactory.decodeFile(path) }.getOrNull()
            } ?: return

            try {
                val canvas = holder.lockCanvas() ?: return
                val dst = RectF(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat())
                canvas.drawBitmap(bmp, null, dst, null)
                holder.unlockCanvasAndPost(canvas)
            } catch (_: Exception) {
                // Surface might not be ready — silently ignore
            } finally {
                bmp.recycle()
            }
        }
    }
}
