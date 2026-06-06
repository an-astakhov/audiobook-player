package dev.audiobookplayer.playback.session

import android.content.Context

class PlaybackSessionStore(
    context: Context,
) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun read(): PlaybackSessionSnapshot? {
        val bookId = preferences.getString(KEY_BOOK_ID, null) ?: return null
        return PlaybackSessionSnapshot(
            activeBookId = bookId,
            positionMs = preferences.getLong(KEY_POSITION_MS, 0L),
            playbackSpeed = preferences.getFloat(KEY_PLAYBACK_SPEED, 1f),
            wasPlaying = preferences.getBoolean(KEY_WAS_PLAYING, false),
        )
    }

    fun write(snapshot: PlaybackSessionSnapshot) {
        preferences.edit()
            .putString(KEY_BOOK_ID, snapshot.activeBookId)
            .putLong(KEY_POSITION_MS, snapshot.positionMs)
            .putFloat(KEY_PLAYBACK_SPEED, snapshot.playbackSpeed)
            .putBoolean(KEY_WAS_PLAYING, snapshot.wasPlaying)
            .apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "playback-session"
        const val KEY_BOOK_ID = "book_id"
        const val KEY_POSITION_MS = "position_ms"
        const val KEY_PLAYBACK_SPEED = "playback_speed"
        const val KEY_WAS_PLAYING = "was_playing"
    }
}

data class PlaybackSessionSnapshot(
    val activeBookId: String,
    val positionMs: Long,
    val playbackSpeed: Float,
    val wasPlaying: Boolean,
)
