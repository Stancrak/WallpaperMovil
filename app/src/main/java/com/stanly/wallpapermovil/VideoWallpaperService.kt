package com.stanly.wallpapermovil

import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.effect.Crop
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Live Wallpaper service powered by AndroidX Media3 ExoPlayer.
 *
 * Battery optimization:
 *  - Pauses when wallpaper is not visible (onVisibilityChanged false).
 *  - Resumes when home screen returns.
 *  - Resources released in onDestroy.
 *
 * Visual quality:
 *  - Playback starts only after onRenderedFirstFrame to avoid black flash.
 *  - Zoom / pan applied via media3-effect Crop GL effect.
 */
class VideoWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = VideoEngine()

    inner class VideoEngine : Engine() {

        private var player: ExoPlayer? = null
        private val scope = CoroutineScope(Dispatchers.Main + Job())
        private var isVisible = true
        private var currentUri = ""

        // ── Lifecycle ─────────────────────────────────────────────────────────

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            initPlayer(surfaceHolder)
            observePreferences()
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            player?.setVideoSurface(holder.surface)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            player?.setVideoSurface(holder.surface)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            player?.setVideoSurface(null)
            super.onSurfaceDestroyed(holder)
        }

        /** Battery optimization: pause/resume based on wallpaper visibility. */
        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            isVisible = visible
            if (visible) player?.play() else player?.pause()
        }

        override fun onDestroy() {
            player?.release()
            player = null
            scope.cancel()
            super.onDestroy()
        }

        // ── Private helpers ───────────────────────────────────────────────────

        private fun initPlayer(surfaceHolder: SurfaceHolder) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build()

            player = ExoPlayer.Builder(this@VideoWallpaperService)
                .build()
                .also { exo ->
                    exo.setAudioAttributes(audioAttributes, false)
                    exo.repeatMode = Player.REPEAT_MODE_ALL
                    exo.volume = 0f
                    // Scale to fill, cropping instead of letterboxing
                    exo.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                    exo.setVideoSurface(surfaceHolder.surface)

                    // Fix black preview: start playback only after first frame is ready
                    exo.addListener(object : Player.Listener {
                        override fun onRenderedFirstFrame() {
                            if (isVisible) exo.play()
                        }
                    })
                }
        }

        private fun observePreferences() {
            scope.launch {
                WallpaperPreferences.getConfig(applicationContext)
                    .collectLatest { config ->
                        val exo = player ?: return@collectLatest

                        // Reload media only when URI changes (avoid re-buffering)
                        if (config.videoUri.isNotBlank() && config.videoUri != currentUri) {
                            currentUri = config.videoUri
                            exo.setMediaItem(MediaItem.fromUri(config.videoUri))
                            applyCropEffect(exo, config.zoom, config.offsetX, config.offsetY)
                            exo.prepare()
                            // play() will be triggered by onRenderedFirstFrame
                        } else if (config.videoUri.isNotBlank()) {
                            // URI unchanged — only update effects/volume live
                            applyCropEffect(exo, config.zoom, config.offsetX, config.offsetY)
                        }

                        exo.volume = if (config.isMuted) 0f else 1f
                    }
            }
        }

        /**
         * Applies a [Crop] GL effect to [exo] based on zoom and pan values.
         * When zoom == 1 and offsets == 0, no effect is applied (full frame).
         */
        private fun applyCropEffect(
            exo: ExoPlayer,
            zoom: Float,
            offsetX: Float,
            offsetY: Float
        ) {
            val zoomClamped = zoom.coerceIn(1f, 4f)
            if (zoomClamped <= 1.01f && offsetX == 0f && offsetY == 0f) {
                exo.setVideoEffects(emptyList())
                return
            }
            val halfSize = 1f / zoomClamped
            val left   = (offsetX - halfSize).coerceIn(-1f, 1f)
            val right  = (offsetX + halfSize).coerceIn(-1f, 1f)
            val bottom = (offsetY - halfSize).coerceIn(-1f, 1f)
            val top    = (offsetY + halfSize).coerceIn(-1f, 1f)
            exo.setVideoEffects(listOf(Crop(left, right, bottom, top)))
        }
    }
}
