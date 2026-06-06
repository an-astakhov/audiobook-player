package dev.audiobookplayer.data.db

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "chapters",
    primaryKeys = ["bookId", "chapterIndex"],
    indices = [
        Index(value = ["bookId"]),
    ],
)
data class ChapterEntity(
    val bookId: String,
    val chapterIndex: Int,
    val title: String,
    val startPositionMs: Long,
)
