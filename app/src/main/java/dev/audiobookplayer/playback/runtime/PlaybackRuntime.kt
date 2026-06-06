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
import androidx.media3.session.MediaSession
import dev.audiobookplayer.MainActivity
import dev.audiobookplayer.data.repository.LibraryRepository
import dev.audiobookplayer.data.repository.PlaybackSource
import dev.audiobookplayer.playback.controller.PlaybackState
import dev.audiobookplayer.playback.service.AudiobookPlaybackService
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

    val state: StateFlow<PlaybackState> = mutableState.asStateFlow()

    val mediaSession: MediaSession = MediaSession.Builder(appContext, exoPlayer)
        .setSessionActivity(buildSessionActivity())
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

    suspend fun playBook(
        source: PlaybackSource,
        startPositionMs: Long? = null,
    ) {
        currentSource = source
        ensureServiceRunning()
        val requestedStartPositionMs = startPositionMs ?: source.resumePositionMs

        if (exoPlayer.currentMediaItem?.mediaId != source.bookId) {
            exoPlayer.setMediaItem(source.toMediaItem(), requestedStartPositionMs)
            exoPlayer.prepare()
        } else if (startPositionMs != null) {
            exoPlayer.seekTo(requestedStartPositionMs)
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
        exoPlayer.seekTo((exoPlayer.currentPosition - SEEK_INTERVAL_MS).coerceAtLeast(0L))
        updateState()
    }

    fun seekForward() {
        val durationMs = effectiveDurationMs()
        val targetPosition = if (durationMs > 0) {
            (exoPlayer.currentPosition + SEEK_INTERVAL_MS).coerceAtMost(durationMs)
        } else {
            exoPlayer.currentPosition + SEEK_INTERVAL_MS
        }
        exoPlayer.seekTo(targetPosition)
        updateState()
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs.coerceAtLeast(0L))
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
        val currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
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
        val playerDuration = exoPlayer.duration
        return when {
            playerDuration > 0L -> playerDuration
            else -> currentSource?.durationMs ?: 0L
        }
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
        mutableState.value = PlaybackState(
            isPlaying = exoPlayer.isPlaying,
            activeBookId = currentSource?.bookId ?: exoPlayer.currentMediaItem?.mediaId,
            currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L),
            durationMs = effectiveDurationMs(),
            playbackSpeed = exoPlayer.playbackParameters.speed,
        )
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

    private fun PlaybackSource.toMediaItem(): MediaItem {
        return MediaItem.Builder()
            .setMediaId(bookId)
            .setUri(Uri.parse(contentUri))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(author)
                    .build(),
            )
            .build()
    }

    private companion object {
        const val SEEK_INTERVAL_MS = 30_000L
        const val UI_UPDATE_INTERVAL_MS = 1_000L
        const val PROGRESS_PERSIST_INTERVAL_MS = 5_000L
        const val MIN_PLAYBACK_SPEED = 0.5f
        const val MAX_PLAYBACK_SPEED = 2.5f
    }
}
