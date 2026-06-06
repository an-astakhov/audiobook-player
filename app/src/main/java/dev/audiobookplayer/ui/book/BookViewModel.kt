package dev.audiobookplayer.ui.book

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.audiobookplayer.AppContainer
import dev.audiobookplayer.domain.model.BookSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class BookUiState(
    val isLoading: Boolean = true,
    val book: BookSummary? = null,
)

class BookViewModel(
    appContainer: AppContainer,
    bookId: String,
) : ViewModel() {
    val uiState: StateFlow<BookUiState> = appContainer.libraryRepository
        .observeBook(bookId)
        .map { book ->
            BookUiState(
                isLoading = false,
                book = book,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BookUiState(),
        )

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

