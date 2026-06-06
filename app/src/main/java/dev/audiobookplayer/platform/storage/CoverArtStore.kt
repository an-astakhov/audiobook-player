package dev.audiobookplayer.platform.storage

import android.content.Context
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CoverArtStore(
    private val context: Context,
) {
    suspend fun save(
        bookId: String,
        artworkBytes: ByteArray?,
    ): String? = withContext(Dispatchers.IO) {
        if (artworkBytes == null) {
            return@withContext null
        }

        val coverDirectory = File(context.filesDir, "covers").apply {
            mkdirs()
        }
        val outputFile = File(coverDirectory, "$bookId.art")
        outputFile.writeBytes(artworkBytes)
        outputFile.absolutePath
    }
}

