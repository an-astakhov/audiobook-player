package dev.audiobookplayer.domain.model

data class BookChapter(
    val index: Int,
    val title: String,
    val startPositionMs: Long,
    val startPositionLabel: String,
)
