package dev.audiobookplayer.ui.library

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.audiobookplayer.AppContainer
import dev.audiobookplayer.domain.model.BookSummary
import dev.audiobookplayer.ui.components.BookCoverArtwork
import dev.audiobookplayer.ui.theme.AudiobookPlayerTheme
import dev.audiobookplayer.ui.theme.Apricot
import dev.audiobookplayer.ui.theme.Paper
import dev.audiobookplayer.ui.theme.Sand
import dev.audiobookplayer.ui.theme.WarmWhite

@Composable
fun LibraryRoute(
    appContainer: AppContainer,
    onOpenBook: (String) -> Unit,
    onImportBook: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: LibraryViewModel = viewModel(
        factory = LibraryViewModel.factory(appContainer),
    )
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            viewModel.importBook(uri)
        }
    }

    LaunchedEffect(uiState.value.message) {
        val message = uiState.value.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearMessage()
    }

    LibraryScreen(
        uiState = uiState.value,
        onOpenBook = onOpenBook,
        onImportBook = {
            onImportBook()
            importLauncher.launch(arrayOf("audio/*", "audio/mp4", "application/mp4"))
        },
        snackbarHostState = snackbarHostState,
    )
}

@Composable
fun LibraryScreen(
    uiState: LibraryUiState,
    onOpenBook: (String) -> Unit,
    onImportBook: () -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                WarmWhite,
                                Paper,
                                Sand.copy(alpha = 0.28f),
                                Apricot.copy(alpha = 0.22f),
                            ),
                        ),
                    )
                    .padding(innerPadding)
                    .statusBarsPadding()
                    .navigationBarsPadding(),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 22.dp, top = 12.dp, end = 22.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    item {
                        LibraryHeader(
                            isImporting = uiState.isImporting,
                            onImportBook = onImportBook,
                        )
                    }

                    if (uiState.books.isEmpty()) {
                        item {
                            EmptyLibraryState(
                                isImporting = uiState.isImporting,
                                onImportBook = onImportBook,
                            )
                        }
                    } else {
                        item {
                            Text(
                                text = "Recently imported",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        items(uiState.books, key = { it.id }) { book ->
                            BookCard(
                                book = book,
                                onClick = { onOpenBook(book.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryHeader(
    isImporting: Boolean,
    onImportBook: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Audiobooks",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Library",
                style = MaterialTheme.typography.headlineMedium,
            )
        }

        Button(
            onClick = onImportBook,
            enabled = !isImporting,
            shape = RoundedCornerShape(999.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        ) {
            if (isImporting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Importing")
            } else {
                Icon(
                    imageVector = Icons.Outlined.UploadFile,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Import")
            }
        }
    }
}

@Composable
private fun EmptyLibraryState(
    isImporting: Boolean,
    onImportBook: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(580.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(
                containerColor = WarmWhite,
            ),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Surface(
                    modifier = Modifier.size(62.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Headphones,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "A quiet place for your audiobooks",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = "Import a local M4B file to start building the library. Metadata and cover art are extracted during import and stored on device.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Button(
                    onClick = onImportBook,
                    enabled = !isImporting,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.UploadFile,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(if (isImporting) "Importing..." else "Import your first M4B")
                }
            }
        }
    }
}

@Composable
private fun BookCard(
    book: BookSummary,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = WarmWhite,
        ),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(width = 74.dp, height = 106.dp)
                    .clip(RoundedCornerShape(18.dp)),
            ) {
                BookCoverArtwork(
                    title = book.title,
                    coverImagePath = book.coverImagePath,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (!book.author.isNullOrBlank()) {
                            Text(
                                text = book.author,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Text(
                        text = book.durationLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(book.progressPercent / 100f)
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }

                Text(
                    text = "${book.progressPercent}% listened - ${book.progressLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyLibraryPreview() {
    AudiobookPlayerTheme {
        LibraryScreen(
            uiState = LibraryUiState(
                isLoading = false,
                books = emptyList(),
            ),
            onOpenBook = {},
            onImportBook = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PopulatedLibraryPreview() {
    AudiobookPlayerTheme {
        LibraryScreen(
            uiState = LibraryUiState(
                isLoading = false,
                books = listOf(
                    BookSummary(
                        id = "1",
                        title = "World War Z",
                        author = "Max Brooks",
                        progressPercent = 0,
                        durationLabel = "6h 44m",
                        progressLabel = "0m / 6h 44m",
                        coverImagePath = null,
                        hasChapters = false,
                    ),
                ),
            ),
            onOpenBook = {},
            onImportBook = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}
