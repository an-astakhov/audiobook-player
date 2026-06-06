package dev.audiobookplayer.ui.book

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.audiobookplayer.AppContainer
import dev.audiobookplayer.domain.model.BookSummary
import dev.audiobookplayer.ui.theme.AudiobookPlayerTheme

@Composable
fun BookRoute(
    bookId: String,
    appContainer: AppContainer,
    onNavigateBack: () -> Unit,
) {
    val viewModel: BookViewModel = viewModel(
        factory = BookViewModel.factory(
            appContainer = appContainer,
            bookId = bookId,
        ),
    )
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()

    BookScreen(
        uiState = uiState.value,
        onNavigateBack = onNavigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookScreen(
    uiState: BookUiState,
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(uiState.book?.title ?: "Book")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = uiState.book?.title ?: "Book detail placeholder",
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = uiState.book?.author ?: "This screen is the next home for cover art, playback controls, chapter list, and speed settings.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BookPreview() {
    AudiobookPlayerTheme {
        BookScreen(
            uiState = BookUiState(
                isLoading = false,
                book = BookSummary(
                    id = "preview",
                    title = "The Tombs of Atuan",
                    author = "Ursula K. Le Guin",
                    progressPercent = 12,
                    durationLabel = "7h 05m",
                    hasChapters = true,
                ),
            ),
            onNavigateBack = {},
        )
    }
}

