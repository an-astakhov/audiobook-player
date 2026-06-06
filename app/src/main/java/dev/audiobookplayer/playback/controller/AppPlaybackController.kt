package dev.audiobookplayer.playback.controller

import dev.audiobookplayer.data.repository.LibraryRepository
import dev.audiobookplayer.playback.runtime.PlaybackRuntime
import kotlinx.coroutines.flow.StateFlow

class AppPlaybackController(
    private val libraryRepository: LibraryRepository,
    private val playbackRuntime: PlaybackRuntime,
) : PlaybackController {
    override val state: StateFlow<PlaybackState> = playbackRuntime.state

    override suspend fun playBook(
        bookId: String,
        startPositionMs: Long?,
    ) {
        val playbackSource = libraryRepository.getPlaybackSource(bookId) ?: return
        playbackRuntime.playBook(
            source = playbackSource,
            startPositionMs = startPositionMs,
        )
    }

    override fun togglePlayPause() {
        playbackRuntime.togglePlayPause()
    }

    override fun seekBack() {
        playbackRuntime.seekBack()
    }

    override fun seekForward() {
        playbackRuntime.seekForward()
    }

    override fun seekTo(positionMs: Long) {
        playbackRuntime.seekTo(positionMs)
    }

    override fun setPlaybackSpeed(speed: Float) {
        playbackRuntime.setPlaybackSpeed(speed)
    }

    override suspend fun stopForRemovedBook(bookId: String) {
        playbackRuntime.stopForRemovedBook(bookId)
    }
}
