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
 * Live Wallpaper service that renders a looping video on the home screen
 * using AndroidX Media3 ExoPlayer.
 *
 * Battery optimization:
 * - Playback is PAUSED when the wallpaper is not visible (user opens another app).
 * - Playback RESUMES when the home screen is visible again.
 * - All resources are released in [VideoEngine.onDestroy].
 */
class VideoWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = VideoEngine()

    // ─────────────────────────────────────────────────────────────────────────
    // Inner Engine
    // ─────────────────────────────────────────────────────────────────────────

    inner class VideoEngine : Engine() {

        private var player: ExoPlayer? = null
        private val scope = CoroutineScope(Dispatchers.Main + Job())

        /** Tracks wallpaper visibility to avoid playing when off-screen. */
        private var isVisible = true

        // ── Lifecycle ────────────────────────────────────────────────────────

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
            // Re-attach surface when size changes (e.g. orientation flip).
            player?.setVideoSurface(holder.surface)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            player?.setVideoSurface(null)
            super.onSurfaceDestroyed(holder)
        }

        /**
         * Critical battery optimization:
         * Pause when the wallpaper is hidden (another app in foreground),
         * resume when the home screen comes back.
         */
        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            isVisible = visible
            if (visible) {
                player?.play()
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

        // ── Private helpers ──────────────────────────────────────────────────

        /** Builds the ExoPlayer instance and attaches it to the provided surface. */
        private fun initPlayer(surfaceHolder: SurfaceHolder) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build()

            player = ExoPlayer.Builder(this@VideoWallpaperService)
                .build()
                .also { exo ->
                    // Audio attributes (will be controlled via volume below)
                    exo.setAudioAttributes(audioAttributes, /* handleAudioFocus= */ false)
                    // Loop video indefinitely
                    exo.repeatMode = Player.REPEAT_MODE_ALL
                    // Default muted until prefs are loaded
                    exo.volume = 0f
                    // Attach rendering surface
                    exo.setVideoSurface(surfaceHolder.surface)
                }
        }

        /**
         * Observes DataStore preferences and reconfigures the player whenever
         * the user changes the video URI or mute state.
         */
        private fun observePreferences() {
            scope.launch {
                WallpaperPreferences.getConfig(applicationContext)
                    .collectLatest { config ->
                        val exo = player ?: return@collectLatest

                        if (config.videoUri.isNotBlank()) {
                            val mediaItem = MediaItem.fromUri(config.videoUri)
                            // Only reload if the URI actually changed to avoid re-buffering
                            val currentUri = exo.currentMediaItem?.localConfiguration?.uri?.toString()
                            if (currentUri != config.videoUri) {
                                exo.setMediaItem(mediaItem)
                                exo.prepare()
                            }
                        }

                        // Apply mute preference
                        exo.volume = if (config.isMuted) 0f else 1f

                        // Start playback only if the wallpaper is currently on screen
                        if (isVisible) exo.play()
                    }
            }
        }
    }
}
