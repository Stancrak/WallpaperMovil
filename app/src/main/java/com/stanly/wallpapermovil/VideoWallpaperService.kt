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
 * Playback strategy (no race conditions):
 *  - playWhenReady = true at creation → auto-starts when READY.
 *  - onVisibilityChanged calls play()/pause() explicitly.
 *  - observePreferences also calls play() if already visible when URI loads
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

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            player?.setVideoSurface(holder.surface)
            if (isVisible) player?.play()
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


    }
}
