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
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BookUiState(
    val isLoading: Boolean = true,
    val book: BookDetail? = null,
    val playbackState: PlaybackState = PlaybackState(),
    val isDeleting: Boolean = false,
    val message: String? = null,
    val wasDeleted: Boolean = false,
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

    val adjustedRemainingBookMs: Long
        get() = (remainingBookMs / effectivePlaybackSpeed.coerceAtLeast(0.1f)).toLong()

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
        get() = "${DurationFormatter.formatDuration(adjustedRemainingBookMs)} left"

    val effectivePlaybackSpeed: Float
        get() = if (isActiveBook) playbackState.playbackSpeed else (book?.playbackSpeed ?: 1f)

    val playbackSpeedLabel: String
        get() = String.format(Locale.US, "%.1fx", effectivePlaybackSpeed)
}

class BookViewModel(
    private val appContainer: AppContainer,
    private val bookId: String,
) : ViewModel() {
    private val isDeleting = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)
    private val wasDeleted = MutableStateFlow(false)

    val uiState: StateFlow<BookUiState> = appContainer.libraryRepository
        .observeBook(bookId)
        .combine(appContainer.playbackController.state) { book, playbackState ->
            book to playbackState
        }
        .combine(isDeleting) { (book, playbackState), deleting ->
            Triple(book, playbackState, deleting)
        }
        .combine(message) { (book, playbackState, deleting), currentMessage ->
            Quadruple(book, playbackState, deleting, currentMessage)
        }
        .combine(wasDeleted) { (book, playbackState, deleting, currentMessage), deleted ->
            BookUiState(
                isLoading = false,
                book = book,
                playbackState = playbackState,
                isDeleting = deleting,
                message = currentMessage,
                wasDeleted = deleted,
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

    fun onSetPlaybackSpeed(speed: Float) {
        if (uiState.value.isActiveBook) {
            appContainer.playbackController.setPlaybackSpeed(speed)
        }
    }

    fun removeBook() {
        if (isDeleting.value) return

        viewModelScope.launch {
            isDeleting.value = true
            message.value = null

            runCatching {
                appContainer.playbackController.stopForRemovedBook(bookId)
                appContainer.libraryRepository.deleteBook(bookId)
            }.onSuccess {
                wasDeleted.value = true
            }.onFailure { throwable ->
                message.value = throwable.message ?: "Unable to remove this audiobook right now."
            }

            isDeleting.value = false
        }
    }

    fun clearMessage() {
        message.update { null }
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

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
)
