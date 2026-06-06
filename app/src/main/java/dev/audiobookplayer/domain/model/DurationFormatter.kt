package dev.audiobookplayer.domain.model

import kotlin.math.roundToInt

object DurationFormatter {
    fun formatDuration(durationMs: Long): String {
        val totalMinutes = (durationMs / 60_000).coerceAtLeast(0)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        return if (hours > 0) {
            "${hours}h ${minutes.toString().padStart(2, '0')}m"
        } else {
            "${minutes}m"
        }
    }

    fun formatElapsed(durationMs: Long): String {
        val totalMinutes = (durationMs / 60_000).coerceAtLeast(0)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        return if (hours > 0) {
            "${hours}h ${minutes.toString().padStart(2, '0')}m"
        } else {
            "${minutes}m"
        }
    }

    fun formatPlaybackPosition(durationMs: Long): String {
        val totalSeconds = (durationMs / 1_000).coerceAtLeast(0)
        val hours = totalSeconds / 3_600
        val minutes = (totalSeconds % 3_600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
    }

    fun progressPercent(
        positionMs: Long,
        durationMs: Long,
    ): Int {
        if (durationMs <= 0) return 0
        return ((positionMs.toDouble() / durationMs.toDouble()) * 100)
            .coerceIn(0.0, 100.0)
            .roundToInt()
    }
}

