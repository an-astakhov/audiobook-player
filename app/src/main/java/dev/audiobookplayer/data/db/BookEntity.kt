package dev.audiobookplayer.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "books",
    indices = [
        Index(value = ["contentUri"], unique = true),
    ],
)
data class BookEntity(
    @PrimaryKey val id: String,
    val contentUri: String,
    val displayName: String,
    val title: String,
    val author: String?,
    val durationMs: Long,
    val coverImagePath: String?,
    val fileSizeBytes: Long?,
    val mimeType: String?,
    val dateImportedEpochMs: Long,
    val lastPlayedAtEpochMs: Long,
    val currentPositionMs: Long,
    val playbackSpeed: Float,
    val hasChapters: Boolean,
)

