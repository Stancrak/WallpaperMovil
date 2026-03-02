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
 * Key design decisions:
 *  - Uses `playWhenReady` instead of `onRenderedFirstFrame` listener.
 *    This avoids a bug where the listener fires while the wallpaper is not
 *    yet visible (system picker preview), causing the flag-check to skip
 *    play() permanently → black wallpaper on home screen.
 *  - Pauses on onVisibilityChanged(false) → battery optimization.
 *  - Resumes on onVisibilityChanged(true).
 */
class VideoWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = VideoEngine()

    inner class VideoEngine : Engine() {

        private var player: ExoPlayer? = null
        private val scope = CoroutineScope(Dispatchers.Main + Job())
        private var isVisible = true
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

        /** Battery optimization: pause when off-screen, resume when visible. */
        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            isVisible = visible
            if (visible) {
                // Resume — also re-arms playWhenReady in case the player
                // was prepared while invisible
                player?.playWhenReady = true
            } else {
                player?.pause()
            }
        }

        override fun onDestroy() {
            player?.release()
            player = null
            scope.cancel()
            super.onDestroy()
        }

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
                    // playWhenReady is set in observePreferences() and onVisibilityChanged()
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
                            // KEY FIX: set playWhenReady to current visibility at
                            // prepare-time, not at initPlayer-time. This handles the
                            // race where onVisibilityChanged fires before DataStore
                            // emits the URI (common in the system wallpaper picker).
                            exo.playWhenReady = isVisible
                            exo.prepare()
                        }

                        exo.volume = if (config.isMuted) 0f else 1f
                    }
            }
        }
    }
}
