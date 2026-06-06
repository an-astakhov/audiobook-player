package dev.audiobookplayer.playback.controller

import kotlinx.coroutines.flow.StateFlow

interface PlaybackController {
    val state: StateFlow<PlaybackState>

    suspend fun playBook(
        bookId: String,
        startPositionMs: Long? = null,
    )
    fun togglePlayPause()
    fun seekBack()
    fun seekForward()
    fun seekTo(positionMs: Long)
    fun setPlaybackSpeed(speed: Float)
}

data class PlaybackState(
    val isPlaying: Boolean = false,
    val activeBookId: String? = null,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1f,
)
