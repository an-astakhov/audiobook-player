package dev.audiobookplayer.playback.runtime

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionResult
import dev.audiobookplayer.MainActivity
import dev.audiobookplayer.data.repository.PlaybackChapter
import dev.audiobookplayer.data.repository.LibraryRepository
import dev.audiobookplayer.data.repository.PlaybackSource
import dev.audiobookplayer.playback.controller.PlaybackState
import dev.audiobookplayer.playback.session.PlaybackSessionSnapshot
import dev.audiobookplayer.playback.session.PlaybackSessionStore
import dev.audiobookplayer.playback.service.AudiobookPlaybackService
import dev.audiobookplayer.playback.service.PlaybackNotificationCommands
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlaybackRuntime(
    private val context: Context,
    private val libraryRepository: LibraryRepository,
    private val appScope: CoroutineScope,
    private val sessionStore: PlaybackSessionStore,
) {
    private val appContext = context.applicationContext
    private val exoPlayer = ExoPlayer.Builder(appContext)
        .setSeekBackIncrementMs(SEEK_INTERVAL_MS)
        .setSeekForwardIncrementMs(SEEK_INTERVAL_MS)
        .build()
        .apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .build(),
                true,
            )
            setHandleAudioBecomingNoisy(true)
        }
    private val mutableState = MutableStateFlow(PlaybackState())
    private var currentSource: PlaybackSource? = null
    private var progressJob: Job? = null
    private val notificationCustomLayout: List<CommandButton> =
        PlaybackNotificationCommands.buildCustomLayout(appContext)

    val state: StateFlow<PlaybackState> = mutableState.asStateFlow()

    val mediaSession: MediaSession = MediaSession.Builder(appContext, exoPlayer)
        .setSessionActivity(buildSessionActivity())
        .setCustomLayout(notificationCustomLayout)
        .setCallback(
            object : MediaSession.Callback {
                override fun onConnect(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                ): MediaSession.ConnectionResult {
                    return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                        .setAvailableSessionCommands(
                            PlaybackNotificationCommands.buildAvailableSessionCommands(),
                        )
                        .setCustomLayout(notificationCustomLayout)
                        .build()
                }

                override fun onPostConnect(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                ) {
                    session.setCustomLayout(controller, notificationCustomLayout)
                }

                override fun onCustomCommand(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    customCommand: androidx.media3.session.SessionCommand,
                    args: android.os.Bundle,
                ): ListenableFuture<SessionResult> {
                    return when {
                        PlaybackNotificationCommands.isSeekBack(customCommand) -> {
                            seekBack()
                            Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                        }

                        PlaybackNotificationCommands.isSeekForward(customCommand) -> {
                            seekForward()
                            Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                        }

                        else -> super.onCustomCommand(session, controller, customCommand, args)
                    }
                }
            },
        )
        .build()

    init {
        exoPlayer.addListener(
            object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    updateState()
                    syncProgressLoop()
                    if (!isPlaying) {
                        persistCurrentProgress()
                    }
                }

                override fun onEvents(player: Player, events: Player.Events) {
                    if (
                        events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION) ||
                        events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED) ||
                        events.contains(Player.EVENT_POSITION_DISCONTINUITY) ||
                        events.contains(Player.EVENT_PLAYBACK_PARAMETERS_CHANGED)
                    ) {
                        updateState()
                    }
                }
            },
        )

        appScope.launch {
            restoreLastSession()
        }
    }

    fun isPlaybackOngoing(): Boolean = exoPlayer.isPlaying

    fun pause() {
        exoPlayer.pause()
    }

    fun release() {
        progressJob?.cancel()
        mediaSession.release()
        exoPlayer.release()
    }

    suspend fun stopForRemovedBook(bookId: String) {
        if (currentSource?.bookId != bookId) return

        currentSource = null
        exoPlayer.pause()
        exoPlayer.clearMediaItems()
        updateState()
        sessionStore.clear()
        appContext.stopService(Intent(appContext, AudiobookPlaybackService::class.java))
    }

    suspend fun playBook(
        source: PlaybackSource,
        startPositionMs: Long? = null,
    ) {
        val previousBookId = currentSource?.bookId
        currentSource = source
        ensureServiceRunning()
        val requestedStartPositionMs = when {
            previousBookId == source.bookId && startPositionMs == null && exoPlayer.mediaItemCount > 0 ->
                currentGlobalPositionMs()

            else -> startPositionMs ?: source.resumePositionMs
        }
        val playbackTarget = source.resolvePlaybackTarget(requestedStartPositionMs)

        if (previousBookId != source.bookId || exoPlayer.mediaItemCount == 0) {
            exoPlayer.setMediaItems(
                source.toMediaItems(),
                playbackTarget.itemIndex,
                playbackTarget.itemPositionMs,
            )
            exoPlayer.prepare()
        } else if (
            playbackTarget.itemIndex != exoPlayer.currentMediaItemIndex ||
            kotlin.math.abs(exoPlayer.currentPosition - playbackTarget.itemPositionMs) > 1_000L
        ) {
            exoPlayer.seekTo(playbackTarget.itemIndex, playbackTarget.itemPositionMs)
        }
        exoPlayer.setPlaybackParameters(PlaybackParameters(source.playbackSpeed))

        exoPlayer.playWhenReady = true
        exoPlayer.play()
        updateState()
        syncProgressLoop()
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else if (exoPlayer.currentMediaItem != null) {
            ensureServiceRunning()
            exoPlayer.play()
        }
        updateState()
    }

    fun seekBack() {
        seekTo((currentGlobalPositionMs() - SEEK_INTERVAL_MS).coerceAtLeast(0L))
    }

    fun seekForward() {
        val durationMs = effectiveDurationMs()
        val targetPosition = if (durationMs > 0) {
            (currentGlobalPositionMs() + SEEK_INTERVAL_MS).coerceAtMost(durationMs)
        } else {
            currentGlobalPositionMs() + SEEK_INTERVAL_MS
        }
        seekTo(targetPosition)
    }

    fun seekTo(positionMs: Long) {
        val source = currentSource
        if (source == null) {
            exoPlayer.seekTo(positionMs.coerceAtLeast(0L))
            updateState()
            return
        }

        val playbackTarget = source.resolvePlaybackTarget(positionMs)
        exoPlayer.seekTo(playbackTarget.itemIndex, playbackTarget.itemPositionMs)
        updateState()
    }

    fun setPlaybackSpeed(speed: Float) {
        val normalizedSpeed = speed.coerceIn(MIN_PLAYBACK_SPEED, MAX_PLAYBACK_SPEED)
        exoPlayer.setPlaybackParameters(PlaybackParameters(normalizedSpeed))
        updateState()
        persistCurrentProgress()
    }

    fun persistCurrentProgress() {
        val source = currentSource ?: return
        val currentPositionMs = currentGlobalPositionMs()
        val playbackSpeed = exoPlayer.playbackParameters.speed

        appScope.launch {
            libraryRepository.updatePlaybackState(
                bookId = source.bookId,
                positionMs = currentPositionMs,
                lastPlayedAtEpochMs = System.currentTimeMillis(),
                playbackSpeed = playbackSpeed,
            )
        }
    }

    private fun ensureServiceRunning() {
        val serviceIntent = Intent(appContext, AudiobookPlaybackService::class.java).apply {
            component = ComponentName(appContext, AudiobookPlaybackService::class.java)
        }
        ContextCompat.startForegroundService(appContext, serviceIntent)
    }

    private fun effectiveDurationMs(): Long {
        return currentSource?.durationMs ?: exoPlayer.duration.coerceAtLeast(0L)
    }

    private fun syncProgressLoop() {
        if (exoPlayer.isPlaying) {
            if (progressJob?.isActive == true) return

            progressJob = appScope.launch {
                var elapsedSincePersistMs = 0L
                while (isActive && exoPlayer.isPlaying) {
                    updateState()
                    delay(UI_UPDATE_INTERVAL_MS)
                    elapsedSincePersistMs += UI_UPDATE_INTERVAL_MS
                    if (elapsedSincePersistMs >= PROGRESS_PERSIST_INTERVAL_MS) {
                        persistCurrentProgress()
                        elapsedSincePersistMs = 0L
                    }
                }
            }
        } else {
            progressJob?.cancel()
            progressJob = null
        }
    }

    private fun updateState() {
        val playbackState = PlaybackState(
            isPlaying = exoPlayer.isPlaying,
            activeBookId = currentSource?.bookId ?: exoPlayer.currentMediaItem?.mediaId,
            currentPositionMs = currentGlobalPositionMs(),
            durationMs = effectiveDurationMs(),
            playbackSpeed = exoPlayer.playbackParameters.speed,
        )
        mutableState.value = playbackState
        persistSessionSnapshot(playbackState)
    }

    private fun buildSessionActivity(): PendingIntent {
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            appContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun currentGlobalPositionMs(): Long {
        val source = currentSource ?: return exoPlayer.currentPosition.coerceAtLeast(0L)
        if (source.chapters.isEmpty()) {
            return exoPlayer.currentPosition.coerceIn(0L, source.durationMs)
        }

        val chapter = source.chapters.getOrNull(exoPlayer.currentMediaItemIndex)
            ?: return exoPlayer.currentPosition.coerceIn(0L, source.durationMs)
        return (chapter.startPositionMs + exoPlayer.currentPosition)
            .coerceIn(0L, source.durationMs)
    }

    private suspend fun restoreLastSession() {
        val snapshot = sessionStore.read() ?: return
        val source = libraryRepository.getPlaybackSource(snapshot.activeBookId)
            ?.copy(
                resumePositionMs = snapshot.positionMs,
                playbackSpeed = snapshot.playbackSpeed,
            )

        if (source == null) {
            sessionStore.clear()
            return
        }

        currentSource = source
        val playbackTarget = source.resolvePlaybackTarget(snapshot.positionMs)
        exoPlayer.setMediaItems(
            source.toMediaItems(),
            playbackTarget.itemIndex,
            playbackTarget.itemPositionMs,
        )
        exoPlayer.prepare()
        exoPlayer.playWhenReady = false
        exoPlayer.setPlaybackParameters(PlaybackParameters(snapshot.playbackSpeed))
        updateState()
    }

    private fun persistSessionSnapshot(playbackState: PlaybackState) {
        val source = currentSource ?: run {
            sessionStore.clear()
            return
        }

        sessionStore.write(
            PlaybackSessionSnapshot(
                activeBookId = source.bookId,
                positionMs = playbackState.currentPositionMs,
                playbackSpeed = playbackState.playbackSpeed,
                wasPlaying = playbackState.isPlaying,
            ),
        )
    }

    private fun PlaybackSource.resolvePlaybackTarget(positionMs: Long): PlaybackTarget {
        val clampedPositionMs = positionMs.coerceIn(0L, durationMs)
        if (chapters.isEmpty()) {
            return PlaybackTarget(
                itemIndex = 0,
                itemPositionMs = clampedPositionMs,
            )
        }

        val chapter = chapters.lastOrNull { it.startPositionMs <= clampedPositionMs } ?: chapters.first()
        return PlaybackTarget(
            itemIndex = chapter.index,
            itemPositionMs = (clampedPositionMs - chapter.startPositionMs)
                .coerceIn(0L, chapter.endPositionMs - chapter.startPositionMs),
        )
    }

    private fun PlaybackSource.toMediaItems(): List<MediaItem> {
        if (chapters.isEmpty()) {
            return listOf(createMediaItem())
        }

        return chapters.map { chapter ->
            createMediaItem(chapter = chapter)
        }
    }

    private fun PlaybackSource.createMediaItem(chapter: PlaybackChapter? = null): MediaItem {
        val builder = MediaItem.Builder()
            .setMediaId(
                if (chapter == null) {
                    bookId
                } else {
                    "$bookId:${chapter.index}"
                },
            )
            .setUri(Uri.parse(contentUri))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(
                        when {
                            chapter != null -> chapter.title
                            !author.isNullOrBlank() -> author
                            else -> null
                        },
                    )
                    .build(),
            )

        if (chapter != null) {
            builder.setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(chapter.startPositionMs)
                    .setEndPositionMs(chapter.endPositionMs)
                    .build(),
            )
        }

        return builder.build()
    }

    private data class PlaybackTarget(
        val itemIndex: Int,
        val itemPositionMs: Long,
    )

    companion object {
        const val SEEK_INTERVAL_MS = 30_000L
        const val UI_UPDATE_INTERVAL_MS = 1_000L
        const val PROGRESS_PERSIST_INTERVAL_MS = 5_000L
        const val MIN_PLAYBACK_SPEED = 0.5f
        const val MAX_PLAYBACK_SPEED = 2.5f
    }
}
