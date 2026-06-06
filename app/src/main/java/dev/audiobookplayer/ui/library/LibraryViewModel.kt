package dev.audiobookplayer.ui.library

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

data class LibraryUiState(
    val isLoading: Boolean = true,
    val books: List<BookSummary> = emptyList(),
)

class LibraryViewModel(
    appContainer: AppContainer,
) : ViewModel() {
    val uiState: StateFlow<LibraryUiState> = appContainer.libraryRepository
        .observeLibrary()
        .map { books ->
            LibraryUiState(
                isLoading = false,
                books = books,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LibraryUiState(),
        )

    companion object {
        fun factory(appContainer: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                LibraryViewModel(appContainer)
            }
        }
    }
}

