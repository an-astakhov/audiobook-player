package dev.audiobookplayer.data.metadata

data class ImportedBookMetadata(
    val contentUri: String,
    val displayName: String,
    val title: String,
    val author: String?,
    val durationMs: Long,
    val fileSizeBytes: Long?,
    val mimeType: String?,
    val embeddedArtwork: ByteArray?,
    val chapters: List<ImportedChapterMetadata>,
)
