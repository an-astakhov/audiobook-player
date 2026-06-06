package dev.audiobookplayer.data.metadata

import android.content.ContentResolver
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class M4bMetadataExtractor(
    private val context: Context,
) {
    suspend fun extract(uri: Uri): ImportedBookMetadata = withContext(Dispatchers.IO) {
        val documentInfo = queryDocumentInfo(uri)
        validateExtension(documentInfo.displayName)

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)

            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                .orIfBlank { documentInfo.displayName.substringBeforeLast('.', documentInfo.displayName) }
            val author = sequenceOf(
                MediaMetadataRetriever.METADATA_KEY_AUTHOR,
                MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST,
                MediaMetadataRetriever.METADATA_KEY_ARTIST,
                MediaMetadataRetriever.METADATA_KEY_WRITER,
            )
                .mapNotNull { key ->
                    retriever.extractMetadata(key)
                        ?.trim()
                        ?.takeIf { value -> value.isNotEmpty() }
                }
                .firstOrNull()
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?: 0L

            ImportedBookMetadata(
                contentUri = uri.toString(),
                displayName = documentInfo.displayName,
                title = title,
                author = author,
                durationMs = durationMs,
                fileSizeBytes = documentInfo.fileSizeBytes,
                mimeType = documentInfo.mimeType,
                embeddedArtwork = retriever.embeddedPicture,
            )
        } finally {
            retriever.release()
        }
    }

    private fun queryDocumentInfo(uri: Uri): DocumentInfo {
        val mimeType = context.contentResolver.getType(uri)
        var displayName = uri.lastPathSegment.orEmpty()
        var fileSizeBytes: Long? = null

        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    displayName = cursor.getString(nameIndex) ?: displayName
                }
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                    fileSizeBytes = cursor.getLong(sizeIndex)
                }
            }
        }

        return DocumentInfo(
            displayName = displayName,
            fileSizeBytes = fileSizeBytes,
            mimeType = mimeType,
        )
    }

    private fun validateExtension(displayName: String) {
        if (!displayName.lowercase().endsWith(".m4b")) {
            throw UnsupportedOperationException("Only .m4b files are supported in this build.")
        }
    }

    private fun String?.orIfBlank(fallback: () -> String): String {
        val value = this?.trim()
        return if (value.isNullOrEmpty()) fallback() else value
    }

    private data class DocumentInfo(
        val displayName: String,
        val fileSizeBytes: Long?,
        val mimeType: String?,
    )
}
