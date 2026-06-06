package dev.audiobookplayer.data.repository

import android.net.Uri
import dev.audiobookplayer.domain.model.BookDetail
import dev.audiobookplayer.domain.model.BookSummary
import kotlinx.coroutines.flow.Flow

interface LibraryRepository {
    fun observeLibrary(): Flow<List<BookSummary>>
    fun observeBook(bookId: String): Flow<BookDetail?>
    suspend fun importBook(uri: Uri): ImportResult
    suspend fun getPlaybackSource(bookId: String): PlaybackSource?
    suspend fun updatePlaybackState(
        bookId: String,
        positionMs: Long,
        lastPlayedAtEpochMs: Long,
        playbackSpeed: Float,
    )
}

data class ImportResult(
    val bookId: String,
    val wasUpdated: Boolean,
)

data class PlaybackSource(
    val bookId: String,
    val contentUri: String,
    val title: String,
    val author: String?,
    val resumePositionMs: Long,
    val durationMs: Long,
)
