package dev.audiobookplayer.playback.controller

import kotlinx.coroutines.flow.StateFlow

interface PlaybackController {
    val state: StateFlow<PlaybackState>
}

data class PlaybackState(
    val isPlaying: Boolean = false,
    val activeBookId: String? = null,
    val currentPositionLabel: String = "00:00",
)

