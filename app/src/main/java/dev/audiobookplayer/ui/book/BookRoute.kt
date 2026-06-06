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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Forward30
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Replay30
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
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
import dev.audiobookplayer.domain.model.BookDetail
import dev.audiobookplayer.domain.model.DurationFormatter
import dev.audiobookplayer.playback.controller.PlaybackState
import dev.audiobookplayer.ui.components.BookCoverArtwork
import dev.audiobookplayer.ui.theme.AudiobookPlayerTheme
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
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
                        )
                    }

                    item {
                        NextStageCard(
                            displayName = book.displayName,
                            hasChapters = book.hasChapters,
                        )
                    }
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
) {
    val durationMs = uiState.effectiveDurationMs.coerceAtLeast(1L)
    var sliderValue by remember(uiState.book?.id, uiState.isActiveBook) {
        mutableFloatStateOf(uiState.effectivePositionMs.toFloat())
    }

    val effectivePosition = uiState.effectivePositionMs.toFloat()
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = uiState.positionLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = DurationFormatter.formatPlaybackPosition(durationMs),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Slider(
                    value = sliderValue.coerceIn(0f, durationMs.toFloat()),
                    onValueChange = { sliderValue = it },
                    valueRange = 0f..durationMs.toFloat(),
                    onValueChangeFinished = {
                        onSeekTo(sliderValue.toLong())
                    },
                    enabled = uiState.isActiveBook,
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

            Text(
                text = if (uiState.isActiveBook) {
                    uiState.progressLabel
                } else {
                    "Tap play to start this imported audiobook. Once playback begins, background controls and seek actions become available."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
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
private fun NextStageCard(
    displayName: String,
    hasChapters: Boolean,
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
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                )
                Text(
                    text = "Current scope",
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            Text(
                text = "Imported file: $displayName",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = if (hasChapters) {
                    "The file appears chapter-capable, but chapter extraction and next or previous chapter controls are still the next implementation step."
                } else {
                    "This build now covers import, persistent library state, and playback foundation. Chapter parsing and navigation still come next."
                },
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
                    hasChapters = false,
                ),
                playbackState = PlaybackState(
                    durationMs = 24_240_000L,
                ),
            ),
            onNavigateBack = {},
            onPlayPause = {},
            onSeekBack = {},
            onSeekForward = {},
            onSeekTo = {},
        )
    }
}
