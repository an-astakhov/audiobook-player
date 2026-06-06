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
    val message: String? = null,
    val books: List<BookSummary> = emptyList(),
)

class LibraryViewModel(
    private val appContainer: AppContainer,
) : ViewModel() {
    private val isImporting = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<LibraryUiState> = appContainer.libraryRepository
        .observeLibrary()
        .combine(isImporting) { books, importing -> books to importing }
        .combine(message) { (books, importing), currentMessage ->
            LibraryUiState(
                isLoading = false,
                isImporting = importing,
                message = currentMessage,
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
            message.value = null

            runCatching {
                appContainer.libraryRepository.importBook(uri)
            }.onSuccess { result ->
                message.value = if (result.wasUpdated) {
                    "Updated the existing library entry for this M4B."
                } else {
                    "Imported audiobook into your library."
                }
            }.onFailure { throwable ->
                message.value = when (throwable) {
                    is UnsupportedOperationException -> throwable.message
                        ?: "Only .m4b files are supported in this build."

                    else -> throwable.message ?: "Unable to import this audiobook."
                }
            }

            isImporting.value = false
        }
    }

    fun clearMessage() {
        message.update { null }
    }

    companion object {
        fun factory(appContainer: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                LibraryViewModel(appContainer)
            }
        }
    }
}
