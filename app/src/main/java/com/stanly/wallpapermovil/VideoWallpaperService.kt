package com.stanly.wallpapermovil

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

/**
 * Live Wallpaper service powered by AndroidX Media3 ExoPlayer.
 *
 * Battery optimization:
 *  - Pauses on onVisibilityChanged(false) — user opens another app.
 *  - Resumes on onVisibilityChanged(true).
 *  - All resources released in onDestroy.
 *
 * Black-preview fix:
 *  - play() is called from the onRenderedFirstFrame listener so the surface
 *    never shows a black frame — the video only starts presenting once the
 *    first decoded frame is ready.
 *
 * NOTE: zoom/offsetX/offsetY stored in DataStore are used by the AdjustScreen
 * preview; the live wallpaper uses SCALE_TO_FIT_WITH_CROPPING which fills the
 * screen without letterboxing. Full GL-based crop will be a future enhancement.
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
                    // Fill screen without letterboxing
                    exo.setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                    exo.setVideoSurface(surfaceHolder.surface)

                    // Black-preview fix: start playback only after first frame is decoded
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

                        if (config.videoUri.isNotBlank() && config.videoUri != currentUri) {
                            currentUri = config.videoUri
                            exo.setMediaItem(MediaItem.fromUri(config.videoUri))
                            exo.prepare()
                            // play() is triggered by onRenderedFirstFrame listener
                        }

                        exo.volume = if (config.isMuted) 0f else 1f
                    }
            }
        }
    }
}
