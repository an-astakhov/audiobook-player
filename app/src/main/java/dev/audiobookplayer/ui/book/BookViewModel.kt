package dev.audiobookplayer.ui.book

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.audiobookplayer.AppContainer
import dev.audiobookplayer.domain.model.BookChapter
import dev.audiobookplayer.domain.model.BookDetail
import dev.audiobookplayer.domain.model.DurationFormatter
import dev.audiobookplayer.playback.controller.PlaybackState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BookUiState(
    val isLoading: Boolean = true,
    val book: BookDetail? = null,
    val playbackState: PlaybackState = PlaybackState(),
) {
    val isActiveBook: Boolean
        get() = book?.id == playbackState.activeBookId

    val isPlaying: Boolean
        get() = isActiveBook && playbackState.isPlaying

    val effectivePositionMs: Long
        get() = if (isActiveBook) playbackState.currentPositionMs else book?.currentPositionMs ?: 0L

    val effectiveDurationMs: Long
        get() = when {
            isActiveBook && playbackState.durationMs > 0L -> playbackState.durationMs
            else -> book?.durationMs ?: 0L
        }

    val currentChapter: BookChapter?
        get() {
            val chapters = book?.chapters.orEmpty()
            if (chapters.isEmpty()) return null
            return chapters.lastOrNull { it.startPositionMs <= effectivePositionMs } ?: chapters.first()
        }

    val currentChapterIndex: Int
        get() = currentChapter?.index ?: 0

    val currentChapterStartMs: Long
        get() = currentChapter?.startPositionMs ?: 0L

    val currentChapterEndMs: Long
        get() {
            val chapters = book?.chapters.orEmpty()
            val nextChapter = chapters.getOrNull(currentChapterIndex + 1)
            return nextChapter?.startPositionMs ?: effectiveDurationMs
        }

    val currentChapterDurationMs: Long
        get() = (currentChapterEndMs - currentChapterStartMs).coerceAtLeast(1L)

    val currentChapterPositionMs: Long
        get() = (effectivePositionMs - currentChapterStartMs)
            .coerceIn(0L, currentChapterDurationMs)

    val currentChapterRemainingMs: Long
        get() = (currentChapterEndMs - effectivePositionMs).coerceAtLeast(0L)

    val remainingBookMs: Long
        get() = (effectiveDurationMs - effectivePositionMs).coerceAtLeast(0L)

    val progressPercent: Int
        get() = DurationFormatter.progressPercent(
            positionMs = effectivePositionMs,
            durationMs = effectiveDurationMs,
        )

    val positionLabel: String
        get() = DurationFormatter.formatPlaybackPosition(effectivePositionMs)

    val progressLabel: String
        get() = "${DurationFormatter.formatElapsed(effectivePositionMs)} / ${DurationFormatter.formatDuration(effectiveDurationMs)}"

    val chapterElapsedLabel: String
        get() = DurationFormatter.formatPlaybackPosition(currentChapterPositionMs)

    val chapterRemainingLabel: String
        get() = "-${DurationFormatter.formatPlaybackPosition(currentChapterRemainingMs)}"

    val bookRemainingLabel: String
        get() = "${DurationFormatter.formatDuration(remainingBookMs)} left"
}

class BookViewModel(
    private val appContainer: AppContainer,
    private val bookId: String,
) : ViewModel() {
    val uiState: StateFlow<BookUiState> = appContainer.libraryRepository
        .observeBook(bookId)
        .combine(appContainer.playbackController.state) { book, playbackState ->
            BookUiState(
                isLoading = false,
                book = book,
                playbackState = playbackState,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BookUiState(),
        )

    fun onPlayPause() {
        val currentState = uiState.value
        if (currentState.book == null) return

        if (currentState.isActiveBook) {
            appContainer.playbackController.togglePlayPause()
        } else {
            viewModelScope.launch {
                appContainer.playbackController.playBook(bookId)
            }
        }
    }

    fun onSeekBack() {
        if (uiState.value.isActiveBook) {
            appContainer.playbackController.seekBack()
        }
    }

    fun onSeekForward() {
        if (uiState.value.isActiveBook) {
            appContainer.playbackController.seekForward()
        }
    }

    fun onSeekTo(positionMs: Long) {
        if (uiState.value.isActiveBook) {
            appContainer.playbackController.seekTo(positionMs)
        }
    }

    fun onSelectChapter(startPositionMs: Long) {
        if (uiState.value.book == null) return

        if (uiState.value.isActiveBook) {
            appContainer.playbackController.seekTo(startPositionMs)
        } else {
            viewModelScope.launch {
                appContainer.playbackController.playBook(
                    bookId = bookId,
                    startPositionMs = startPositionMs,
                )
            }
        }
    }

    companion object {
        fun factory(
            appContainer: AppContainer,
            bookId: String,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                BookViewModel(
                    appContainer = appContainer,
                    bookId = bookId,
                )
            }
        }
    }
}
