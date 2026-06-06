package dev.audiobookplayer.domain.model

data class BookSummary(
    val id: String,
    val title: String,
    val author: String?,
    val progressPercent: Int,
    val durationLabel: String,
    val hasChapters: Boolean,
)

