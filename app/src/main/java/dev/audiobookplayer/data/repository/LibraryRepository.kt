package dev.audiobookplayer.data.repository

import dev.audiobookplayer.domain.model.BookSummary
import kotlinx.coroutines.flow.Flow

interface LibraryRepository {
    fun observeLibrary(): Flow<List<BookSummary>>
    fun observeBook(bookId: String): Flow<BookSummary?>
}

