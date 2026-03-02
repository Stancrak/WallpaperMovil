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
 * Live Wallpaper service — ExoPlayer-based.
 *
 * Playback strategy (no race conditions):
 *  - playWhenReady = true at creation → auto-starts when READY.
 *  - onVisibilityChanged calls play()/pause() explicitly.
 *  - observePreferences also calls play() if already visible when URI loads
 *    (handles the case where visibility=true before DataStore emits).
 */
class VideoWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = VideoEngine()

    inner class VideoEngine : Engine() {

        private var player: ExoPlayer? = null
        private val scope = CoroutineScope(Dispatchers.Main + Job())
        private var isVisible = true   // true until first onVisibilityChanged
        private var currentUri = ""

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

        /** Battery optimization — pause when home hidden, resume when visible. */
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

        private fun initPlayer(surfaceHolder: SurfaceHolder) {
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
                    exo.setVideoSurface(surfaceHolder.surface)
                    // Start playing as soon as buffering finishes
                    exo.playWhenReady = true
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
                            // If visibility already arrived before DataStore, arm playback now
                            if (isVisible) exo.play()
                        }

                        exo.volume = if (config.isMuted) 0f else 1f
                    }
            }
        }
    }
}
