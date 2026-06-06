package dev.audiobookplayer.data.repository

import dev.audiobookplayer.domain.model.BookSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class InMemoryLibraryRepository : LibraryRepository {
    private val books = MutableStateFlow(emptyList<BookSummary>())

    override fun observeLibrary(): Flow<List<BookSummary>> = books

    override fun observeBook(bookId: String): Flow<BookSummary?> {
        return books.map { library -> library.firstOrNull { it.id == bookId } }
    }
}

