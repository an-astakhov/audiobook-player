package dev.audiobookplayer.ui.book

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Forward30
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Replay30
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.audiobookplayer.AppContainer
import dev.audiobookplayer.domain.model.BookChapter
import dev.audiobookplayer.domain.model.BookDetail
import dev.audiobookplayer.playback.controller.PlaybackState
import dev.audiobookplayer.ui.components.BookCoverArtwork
import dev.audiobookplayer.ui.theme.AudiobookPlayerTheme
import java.util.Locale
import kotlin.math.abs

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
        onPlayPause = viewModel::onPlayPause,
        onSeekBack = viewModel::onSeekBack,
        onSeekForward = viewModel::onSeekForward,
        onSeekTo = viewModel::onSeekTo,
        onSelectChapter = viewModel::onSelectChapter,
        onSetPlaybackSpeed = viewModel::onSetPlaybackSpeed,
        onRemoveBook = viewModel::removeBook,
        onClearMessage = viewModel::clearMessage,
    )
}

@Composable
fun BookScreen(
    uiState: BookUiState,
    onNavigateBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSelectChapter: (Long) -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit,
    onRemoveBook: () -> Unit,
    onClearMessage: () -> Unit,
) {
    var isChapterDialogOpen by rememberSaveable(uiState.book?.id) {
        mutableStateOf(false)
    }
    var isSpeedDialogOpen by rememberSaveable(uiState.book?.id) {
        mutableStateOf(false)
    }
    var isDeleteDialogOpen by rememberSaveable(uiState.book?.id) {
        mutableStateOf(false)
    }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        val message = uiState.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onClearMessage()
    }

    LaunchedEffect(uiState.wasDeleted) {
        if (uiState.wasDeleted) {
            onNavigateBack()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.book == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "This audiobook is no longer available.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            else -> {
                val book = uiState.book
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(innerPadding)
                        .statusBarsPadding()
                        .navigationBarsPadding(),
                    contentPadding = PaddingValues(22.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    item {
                        Button(
                            onClick = onNavigateBack,
                            shape = RoundedCornerShape(999.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = null,
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Library")
                        }
                    }

                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 212.dp, height = 300.dp)
                                    .clip(RoundedCornerShape(26.dp)),
                            ) {
                                BookCoverArtwork(
                                    title = book.title,
                                    coverImagePath = book.coverImagePath,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = book.title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    textAlign = TextAlign.Center,
                                )
                                if (!book.author.isNullOrBlank()) {
                                    Text(
                                        text = book.author,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }

                    item {
                        PlayerPanel(
                            uiState = uiState,
                            onPlayPause = onPlayPause,
                            onSeekBack = onSeekBack,
                            onSeekForward = onSeekForward,
                            onSeekTo = onSeekTo,
                            onOpenSpeedDialog = { isSpeedDialogOpen = true },
                        )
                    }

                    item {
                        ChapterLauncherCard(
                            displayName = book.displayName,
                            currentChapterTitle = uiState.currentChapter?.title,
                            chapterCount = book.chapters.size,
                            hasChapters = book.hasChapters,
                            onOpenChapters = { isChapterDialogOpen = true },
                            isRemoving = uiState.isDeleting,
                            onRemoveBook = { isDeleteDialogOpen = true },
                        )
                    }
                }

                if (isChapterDialogOpen && book.chapters.isNotEmpty()) {
                    ChapterDialog(
                        chapters = book.chapters,
                        currentChapterIndex = uiState.currentChapter?.index,
                        onDismiss = { isChapterDialogOpen = false },
                        onSelectChapter = { startPositionMs ->
                            isChapterDialogOpen = false
                            onSelectChapter(startPositionMs)
                        },
                    )
                }

                if (isSpeedDialogOpen) {
                    SpeedDialog(
                        currentSpeed = uiState.effectivePlaybackSpeed,
                        isEnabled = uiState.isActiveBook,
                        onDismiss = { isSpeedDialogOpen = false },
                        onSelectSpeed = { speed ->
                            isSpeedDialogOpen = false
                            onSetPlaybackSpeed(speed)
                        },
                    )
                }

                if (isDeleteDialogOpen) {
                    DeleteBookDialog(
                        title = book.title,
                        isDeleting = uiState.isDeleting,
                        onDismiss = { isDeleteDialogOpen = false },
                        onConfirm = {
                            isDeleteDialogOpen = false
                            onRemoveBook()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerPanel(
    uiState: BookUiState,
    onPlayPause: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onOpenSpeedDialog: () -> Unit,
) {
    val sliderRangeMs = uiState.currentChapterDurationMs.coerceAtLeast(1L)
    var sliderValue by remember(
        uiState.book?.id,
        uiState.isActiveBook,
        uiState.currentChapter?.index,
    ) {
        mutableFloatStateOf(uiState.currentChapterPositionMs.toFloat())
    }

    val effectivePosition = uiState.currentChapterPositionMs.toFloat()
    if (abs(sliderValue - effectivePosition) > 3_000f) {
        sliderValue = effectivePosition
    }

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Playback",
                style = MaterialTheme.typography.titleMedium,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                SnapshotMetric(
                    label = "Progress",
                    value = "${uiState.progressPercent}%",
                )
                SnapshotMetric(
                    label = "Duration",
                    value = uiState.book?.durationLabel.orEmpty(),
                )
                SnapshotMetric(
                    label = "Position",
                    value = uiState.positionLabel,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = uiState.currentChapter?.title ?: "Current chapter",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = uiState.chapterElapsedLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = uiState.chapterRemainingLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Slider(
                    value = sliderValue.coerceIn(0f, sliderRangeMs.toFloat()),
                    onValueChange = { sliderValue = it },
                    valueRange = 0f..sliderRangeMs.toFloat(),
                    onValueChangeFinished = {
                        onSeekTo(uiState.currentChapterStartMs + sliderValue.toLong())
                    },
                    enabled = uiState.isActiveBook,
                )

                Text(
                    text = uiState.bookRemainingLabel,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalIconButton(
                    onClick = onSeekBack,
                    enabled = uiState.isActiveBook,
                    modifier = Modifier.size(58.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Replay30,
                        contentDescription = "Back 30 seconds",
                    )
                }

                FilledIconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(74.dp),
                ) {
                    Icon(
                        imageVector = if (uiState.isPlaying) {
                            Icons.Outlined.Pause
                        } else {
                            Icons.Outlined.PlayArrow
                        },
                        contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                    )
                }

                FilledTonalIconButton(
                    onClick = onSeekForward,
                    enabled = uiState.isActiveBook,
                    modifier = Modifier.size(58.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Forward30,
                        contentDescription = "Forward 30 seconds",
                    )
                }
            }

            Button(
                onClick = onOpenSpeedDialog,
                enabled = uiState.isActiveBook,
                shape = RoundedCornerShape(999.dp),
            ) {
                Text("Speed ${uiState.playbackSpeedLabel}")
            }

            Text(
                text = if (uiState.isActiveBook) {
                    uiState.progressLabel
                } else {
                    "Tap play to start this imported audiobook. Once playback begins, chapter scrubbing and seek actions become available."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SpeedDialog(
    currentSpeed: Float,
    isEnabled: Boolean,
    onDismiss: () -> Unit,
    onSelectSpeed: (Float) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        title = {
            Text("Playback speed")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isEnabled) {
                    Text(
                        text = "Start playback to change speed.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    SPEED_OPTIONS.forEach { speed ->
                        val isCurrent = speed == currentSpeed
                        Button(
                            onClick = { onSelectSpeed(speed) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                        ) {
                            Text(
                                text = if (isCurrent) {
                                    "${formatSpeedLabel(speed)} (Current)"
                                } else {
                                    formatSpeedLabel(speed)
                                },
                            )
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun SnapshotMetric(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ChapterLauncherCard(
    displayName: String,
    currentChapterTitle: String?,
    chapterCount: Int,
    hasChapters: Boolean,
    onOpenChapters: () -> Unit,
    isRemoving: Boolean,
    onRemoveBook: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Bookmarks,
                    contentDescription = null,
                )
                Text(
                    text = "Chapters",
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            if (!hasChapters) {
                Text(
                    text = "Imported file: $displayName",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = "No embedded chapter markers were found in this M4B. Playback still works normally, but there is no chapter list to jump through for this file.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = currentChapterTitle ?: displayName,
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "$chapterCount chapters available in this book.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = onOpenChapters,
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Bookmarks,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Chapters")
                }
            }

            TextButton(
                onClick = onRemoveBook,
                enabled = !isRemoving,
            ) {
                Text(if (isRemoving) "Removing..." else "Remove from library")
            }
        }
    }
}

@Composable
private fun DeleteBookDialog(
    title: String,
    isDeleting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isDeleting,
            ) {
                Text(if (isDeleting) "Removing..." else "Remove")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDeleting,
            ) {
                Text("Cancel")
            }
        },
        title = {
            Text("Remove audiobook")
        },
        text = {
            Text("Remove \"$title\" from the library? Your saved progress for this import will also be removed.")
        },
    )
}

@Composable
private fun ChapterDialog(
    chapters: List<BookChapter>,
    currentChapterIndex: Int?,
    onDismiss: () -> Unit,
    onSelectChapter: (Long) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        title = {
            Text("Chapters")
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(chapters) { chapter ->
                    ChapterRow(
                        chapter = chapter,
                        isCurrentChapter = chapter.index == currentChapterIndex,
                        onClick = { onSelectChapter(chapter.startPositionMs) },
                    )
                }
            }
        },
    )
}

@Composable
private fun ChapterRow(
    chapter: BookChapter,
    isCurrentChapter: Boolean,
    onClick: () -> Unit,
) {
    val containerColor = if (isCurrentChapter) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    }
    val contentColor = if (isCurrentChapter) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(containerColor)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = chapter.title,
                style = MaterialTheme.typography.titleSmall,
                color = contentColor,
            )
            Text(
                text = "Starts at ${chapter.startPositionLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.78f),
            )
        }

        TextButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Outlined.PlayCircle,
                contentDescription = null,
            )
            Spacer(modifier = Modifier.size(6.dp))
            Text(if (isCurrentChapter) "Replay" else "Jump")
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
                book = BookDetail(
                    id = "preview",
                    title = "World War Z",
                    author = "Max Brooks",
                    displayName = "World War Z.m4b",
                    durationMs = 24_240_000L,
                    currentPositionMs = 0L,
                    durationLabel = "6h 44m",
                    currentPositionLabel = "00:00",
                    progressLabel = "0m / 6h 44m",
                    progressPercent = 0,
                    coverImagePath = null,
                    playbackSpeed = 1.2f,
                    hasChapters = true,
                    chapters = listOf(
                        BookChapter(
                            index = 0,
                            title = "00: Intro",
                            startPositionMs = 0L,
                            startPositionLabel = "00:00",
                        ),
                        BookChapter(
                            index = 1,
                            title = "01: Introduction",
                            startPositionMs = 87_000L,
                            startPositionLabel = "01:27",
                        ),
                    ),
                ),
                playbackState = PlaybackState(
                    activeBookId = "preview",
                    currentPositionMs = 98_000L,
                    isPlaying = true,
                    durationMs = 24_240_000L,
                    playbackSpeed = 1.2f,
                ),
            ),
            onNavigateBack = {},
            onPlayPause = {},
            onSeekBack = {},
            onSeekForward = {},
            onSeekTo = {},
            onSelectChapter = {},
            onSetPlaybackSpeed = {},
            onRemoveBook = {},
            onClearMessage = {},
        )
    }
}

private val SPEED_OPTIONS = listOf(0.8f, 1.0f, 1.2f, 1.5f, 1.8f, 2.0f)

private fun formatSpeedLabel(speed: Float): String = String.format(Locale.US, "%.1fx", speed)
