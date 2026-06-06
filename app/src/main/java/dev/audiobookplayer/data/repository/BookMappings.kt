package dev.audiobookplayer.data.repository

import dev.audiobookplayer.data.db.BookEntity
import dev.audiobookplayer.data.db.ChapterEntity
import dev.audiobookplayer.domain.model.BookChapter
import dev.audiobookplayer.domain.model.BookDetail
import dev.audiobookplayer.domain.model.BookSummary
import dev.audiobookplayer.domain.model.DurationFormatter

internal fun BookEntity.toBookSummary(): BookSummary {
    val progressPercent = DurationFormatter.progressPercent(
        positionMs = currentPositionMs,
        durationMs = durationMs,
    )

    return BookSummary(
        id = id,
        title = title,
        author = author,
        progressPercent = progressPercent,
        durationLabel = DurationFormatter.formatDuration(durationMs),
        progressLabel = "${DurationFormatter.formatElapsed(currentPositionMs)} / ${DurationFormatter.formatDuration(durationMs)}",
        coverImagePath = coverImagePath,
        hasChapters = hasChapters,
    )
}

internal fun BookEntity.toBookDetail(chapters: List<ChapterEntity>): BookDetail {
    val progressPercent = DurationFormatter.progressPercent(
        positionMs = currentPositionMs,
        durationMs = durationMs,
    )

    return BookDetail(
        id = id,
        title = title,
        author = author,
        displayName = displayName,
        durationMs = durationMs,
        currentPositionMs = currentPositionMs,
        durationLabel = DurationFormatter.formatDuration(durationMs),
        currentPositionLabel = DurationFormatter.formatPlaybackPosition(currentPositionMs),
        progressLabel = "${DurationFormatter.formatElapsed(currentPositionMs)} / ${DurationFormatter.formatDuration(durationMs)}",
        progressPercent = progressPercent,
        coverImagePath = coverImagePath,
        playbackSpeed = playbackSpeed,
        hasChapters = chapters.isNotEmpty(),
        chapters = chapters.map(ChapterEntity::toBookChapter),
    )
}

internal fun BookEntity.toPlaybackSource(): PlaybackSource {
    return PlaybackSource(
        bookId = id,
        contentUri = contentUri,
        title = title,
        author = author,
        resumePositionMs = currentPositionMs,
        durationMs = durationMs,
        playbackSpeed = playbackSpeed,
    )
}

private fun ChapterEntity.toBookChapter(): BookChapter {
    return BookChapter(
        index = chapterIndex,
        title = title,
        startPositionMs = startPositionMs,
        startPositionLabel = DurationFormatter.formatPlaybackPosition(startPositionMs),
    )
}
