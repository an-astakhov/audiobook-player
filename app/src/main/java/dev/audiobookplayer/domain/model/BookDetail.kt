package dev.audiobookplayer.domain.model

data class BookDetail(
    val id: String,
    val title: String,
    val author: String?,
    val displayName: String,
    val durationMs: Long,
    val currentPositionMs: Long,
    val durationLabel: String,
    val currentPositionLabel: String,
    val progressLabel: String,
    val progressPercent: Int,
    val coverImagePath: String?,
    val hasChapters: Boolean,
    val chapters: List<BookChapter>,
)
