package dev.audiobookplayer.ui.book

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Forward30
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
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import dev.audiobookplayer.ui.theme.Apricot
import dev.audiobookplayer.ui.theme.Coral
import dev.audiobookplayer.ui.theme.Sunflower
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

                MinimalBookContent(
                    book = book,
                    uiState = uiState,
                    onNavigateBack = onNavigateBack,
                    onPlayPause = onPlayPause,
                    onSeekBack = onSeekBack,
                    onSeekForward = onSeekForward,
                    onSeekTo = onSeekTo,
                    onOpenSpeedDialog = { isSpeedDialogOpen = true },
                    onOpenChapters = { isChapterDialogOpen = true },
                    onRemoveBook = { isDeleteDialogOpen = true },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .statusBarsPadding()
                        .navigationBarsPadding(),
                )

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
private fun MinimalBookContent(
    book: BookDetail,
    uiState: BookUiState,
    onNavigateBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onOpenSpeedDialog: () -> Unit,
    onOpenChapters: () -> Unit,
    onRemoveBook: () -> Unit,
    modifier: Modifier = Modifier,
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

    Box(
        modifier = modifier.background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Sunflower,
                    Apricot,
                    MaterialTheme.colorScheme.background,
                ),
            ),
        ),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            shape = RoundedCornerShape(34.dp),
            color = Color(0xFFFFFCF8),
            shadowElevation = 8.dp,
            tonalElevation = 2.dp,
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
            ) {
                val compactLayout = maxHeight < 720.dp
                val coverWidthFraction = if (compactLayout) 0.74f else 0.80f
                val waveformTopSpacing = if (compactLayout) 8.dp else 14.dp
                val sectionSpacing = if (compactLayout) 10.dp else 16.dp

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 22.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(sectionSpacing),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                    ) {
                        FilledTonalIconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.size(42.dp),
                            shape = CircleShape,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back to library",
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(coverWidthFraction)
                            .widthIn(max = 304.dp)
                            .aspectRatio(0.94f)
                            .clip(RoundedCornerShape(30.dp)),
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
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (!book.author.isNullOrBlank()) {
                            Text(
                                text = book.author,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        uiState.currentChapter?.title?.takeIf(String::isNotBlank)?.let { chapterTitle ->
                            Text(
                                text = chapterTitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = waveformTopSpacing),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        WaveformSlider(
                            value = sliderValue.coerceIn(0f, sliderRangeMs.toFloat()),
                            onValueChange = { sliderValue = it },
                            onValueChangeFinished = {
                                onSeekTo(uiState.currentChapterStartMs + sliderValue.toLong())
                            },
                            valueRange = 0f..sliderRangeMs.toFloat(),
                            enabled = uiState.isActiveBook,
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = uiState.chapterElapsedLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = uiState.chapterRemainingLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        Text(
                            text = uiState.bookRemainingLabel,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    MinimalControlRow(
                        playbackSpeedLabel = uiState.playbackSpeedLabel,
                        isPlaying = uiState.isPlaying,
                        isActiveBook = uiState.isActiveBook,
                        hasChapters = book.hasChapters,
                        onOpenChapters = onOpenChapters,
                        onSeekBack = onSeekBack,
                        onPlayPause = onPlayPause,
                        onSeekForward = onSeekForward,
                        onOpenSpeedDialog = onOpenSpeedDialog,
                    )

                    TextButton(
                        onClick = onRemoveBook,
                        enabled = !uiState.isDeleting,
                    ) {
                        Text(if (uiState.isDeleting) "Removing..." else "Remove from library")
                    }
                }
            }
        }
    }
}

@Composable
private fun MinimalControlRow(
    playbackSpeedLabel: String,
    isPlaying: Boolean,
    isActiveBook: Boolean,
    hasChapters: Boolean,
    onOpenChapters: () -> Unit,
    onSeekBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeekForward: () -> Unit,
    onOpenSpeedDialog: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MinimalCircleButton(
            onClick = onOpenChapters,
            enabled = hasChapters,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                contentDescription = "Open chapters",
            )
        }

        MinimalCircleButton(
            onClick = onSeekBack,
            enabled = isActiveBook,
        ) {
            Icon(
                imageVector = Icons.Outlined.Replay30,
                contentDescription = "Back 30 seconds",
            )
        }

        FilledIconButton(
            onClick = onPlayPause,
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier.size(34.dp),
            )
        }

        MinimalCircleButton(
            onClick = onSeekForward,
            enabled = isActiveBook,
        ) {
            Icon(
                imageVector = Icons.Outlined.Forward30,
                contentDescription = "Forward 30 seconds",
            )
        }

        Surface(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .clickable(enabled = isActiveBook, onClick = onOpenSpeedDialog),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = playbackSpeedLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isActiveBook) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

@Composable
private fun MinimalCircleButton(
    onClick: () -> Unit,
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    FilledTonalIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(52.dp),
        shape = CircleShape,
    ) {
        content()
    }
}

@Composable
private fun WaveformSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val range = (valueRange.endInclusive - valueRange.start).coerceAtLeast(1f)
    val normalizedValue = ((value - valueRange.start) / range).coerceIn(0f, 1f)
    val inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    val startColor = Coral
    val endColor = Sunflower

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WAVEFORM_PATTERN.forEachIndexed { index, amplitude ->
                val progressAtBar = index.toFloat() / (WAVEFORM_PATTERN.lastIndex.coerceAtLeast(1))
                val barColor = if (progressAtBar <= normalizedValue) {
                    lerp(startColor, endColor, progressAtBar)
                } else {
                    inactiveColor
                }
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .heightIn(min = 14.dp)
                        .size(
                            width = 4.dp,
                            height = (18 + (amplitude * 26)).dp,
                        )
                        .clip(CircleShape)
                        .background(barColor),
                )
            }
        }

        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange,
            onValueChangeFinished = onValueChangeFinished,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .alpha(0.01f),
            colors = SliderDefaults.colors(
                thumbColor = Color.Transparent,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
                disabledThumbColor = Color.Transparent,
                disabledActiveTrackColor = Color.Transparent,
                disabledInactiveTrackColor = Color.Transparent,
                disabledActiveTickColor = Color.Transparent,
                disabledInactiveTickColor = Color.Transparent,
            ),
        )
    }
}

// Set aside June 2026 MVP layout so we can revert quickly if this redesign misses the mark.
@Composable
private fun LegacyBookContent(
    book: BookDetail,
    uiState: BookUiState,
    onNavigateBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onOpenSpeedDialog: () -> Unit,
    onOpenChapters: () -> Unit,
    onRemoveBook: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onNavigateBack,
                shape = RoundedCornerShape(999.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Library")
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 172.dp, height = 244.dp)
                            .clip(RoundedCornerShape(24.dp)),
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
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (!book.author.isNullOrBlank()) {
                            Text(
                                text = book.author,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        LegacyPlayerPanel(
            uiState = uiState,
            onPlayPause = onPlayPause,
            onSeekBack = onSeekBack,
            onSeekForward = onSeekForward,
            onSeekTo = onSeekTo,
            onOpenSpeedDialog = onOpenSpeedDialog,
            onOpenChapters = onOpenChapters,
            onRemoveBook = onRemoveBook,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
        )
    }
}

@Composable
private fun LegacyPlayerPanel(
    uiState: BookUiState,
    onPlayPause: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onOpenSpeedDialog: () -> Unit,
    onOpenChapters: () -> Unit,
    onRemoveBook: () -> Unit,
    modifier: Modifier = Modifier,
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
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = uiState.currentChapter?.title ?: "Current chapter",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = uiState.chapterElapsedLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = uiState.chapterRemainingLabel,
                        style = MaterialTheme.typography.bodyMedium,
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
                    modifier = Modifier.size(60.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Replay30,
                        contentDescription = "Back 30 seconds",
                    )
                }

                FilledIconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(78.dp),
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
                    modifier = Modifier.size(60.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Forward30,
                        contentDescription = "Forward 30 seconds",
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onOpenSpeedDialog,
                    enabled = uiState.isActiveBook,
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Speed ${uiState.playbackSpeedLabel}")
                }

                Button(
                    onClick = onOpenChapters,
                    enabled = uiState.book?.hasChapters == true,
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Chapters")
                }
            }

            TextButton(
                onClick = onRemoveBook,
                enabled = !uiState.isDeleting,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(if (uiState.isDeleting) "Removing..." else "Remove from library")
            }
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
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Start,
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
                    title = "World War Z: The Complete Edition With A Very Long Title",
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
                            title = "00: Intro and setup for the first oral history segment",
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
private val WAVEFORM_PATTERN = listOf(
    0.24f, 0.46f, 0.31f, 0.58f, 0.42f, 0.67f, 0.38f, 0.54f,
    0.29f, 0.62f, 0.44f, 0.73f, 0.35f, 0.57f, 0.26f, 0.49f,
    0.33f, 0.69f, 0.41f, 0.60f, 0.37f, 0.65f, 0.28f, 0.52f,
    0.25f, 0.47f, 0.34f, 0.63f, 0.39f, 0.71f, 0.32f, 0.56f,
)

private fun formatSpeedLabel(speed: Float): String = String.format(Locale.US, "%.1fx", speed)
