package com.stanly.wallpapermovil

import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
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
 * Design principles (simplified to avoid race conditions):
 *  1. playWhenReady = TRUE from the moment the player is created.
 *     ExoPlayer auto-plays as soon as it reaches READY state.
 *  2. onVisibilityChanged simply flips playWhenReady on/off for battery.
 *  3. observePreferences always calls setMediaItem + prepare when URI
 *     changes. Since playWhenReady is already true, playback starts
 *     automatically when the player finishes buffering.
 *  4. Surface is set both in initPlayer (onCreate) and in onSurfaceCreated
 *     to handle both fast and slow surface creation paths.
 */
class VideoWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = VideoEngine()

    inner class VideoEngine : Engine() {

        private var player: ExoPlayer? = null
        private val scope = CoroutineScope(Dispatchers.Main + Job())
        private var currentUri = ""

        // ── Lifecycle ─────────────────────────────────────────────────────────

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            initPlayer(surfaceHolder)
            observePreferences()
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            // Re-attach in case the surface wasn't valid during onCreate
            player?.setVideoSurface(holder.surface)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            player?.setVideoSurface(holder.surface)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            player?.clearVideoSurface()
            super.onSurfaceDestroyed(holder)
        }

        /** Battery optimization: pause when home screen is hidden. */
        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            player?.playWhenReady = visible
        }

        override fun onDestroy() {
            scope.cancel()
            player?.release()
            player = null
            super.onDestroy()
        }

        // ── Private helpers ───────────────────────────────────────────────────

        private fun initPlayer(surfaceHolder: SurfaceHolder) {
            player = ExoPlayer.Builder(this@VideoWallpaperService)
                .build()
                .apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(C.USAGE_MEDIA)
                            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                            .build(),
                        /* handleAudioFocus= */ false
                    )
                    repeatMode = ExoPlayer.REPEAT_MODE_ALL
                    volume = 0f
                    setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)

                    // Attach the surface now; onSurfaceCreated will re-attach once
                    // the surface is fully ready, which is harmless.
                    setVideoSurface(surfaceHolder.surface)

                    // Auto-play as soon as buffering finishes.
                    // onVisibilityChanged will pause/resume via playWhenReady.
                    playWhenReady = true
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
                            // playWhenReady is already true → auto-plays when READY
                            exo.prepare()
                        }

                        exo.volume = if (config.isMuted) 0f else 1f
                    }
            }
        }
    }
}
