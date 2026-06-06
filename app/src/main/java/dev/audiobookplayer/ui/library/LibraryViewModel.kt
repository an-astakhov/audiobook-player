package dev.audiobookplayer.ui.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.audiobookplayer.AppContainer
import dev.audiobookplayer.domain.model.BookSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LibraryUiState(
    val isLoading: Boolean = true,
    val isImporting: Boolean = false,
    val importErrorMessage: String? = null,
    val books: List<BookSummary> = emptyList(),
)

class LibraryViewModel(
    private val appContainer: AppContainer,
) : ViewModel() {
    private val isImporting = MutableStateFlow(false)
    private val importErrorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<LibraryUiState> = appContainer.libraryRepository
        .observeLibrary()
        .combine(isImporting) { books, importing -> books to importing }
        .combine(importErrorMessage) { (books, importing), errorMessage ->
            LibraryUiState(
                isLoading = false,
                isImporting = importing,
                importErrorMessage = errorMessage,
                books = books,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LibraryUiState(),
        )

    fun importBook(uri: Uri) {
        if (isImporting.value) return

        viewModelScope.launch {
            isImporting.value = true
            importErrorMessage.value = null

            runCatching {
                appContainer.libraryRepository.importBook(uri)
            }.onFailure { throwable ->
                importErrorMessage.value = throwable.message ?: "Unable to import this audiobook."
            }

            isImporting.value = false
        }
    }

    fun clearImportError() {
        importErrorMessage.update { null }
    }

    companion object {
        fun factory(appContainer: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                LibraryViewModel(appContainer)
            }
        }
    }
}
