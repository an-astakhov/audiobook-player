package dev.audiobookplayer.data.repository

import android.net.Uri
import androidx.room.withTransaction
import dev.audiobookplayer.data.db.AppDatabase
import dev.audiobookplayer.data.db.BookDao
import dev.audiobookplayer.data.db.BookEntity
import dev.audiobookplayer.data.db.ChapterDao
import dev.audiobookplayer.data.db.ChapterEntity
import dev.audiobookplayer.data.metadata.M4bMetadataExtractor
import dev.audiobookplayer.domain.model.BookDetail
import dev.audiobookplayer.domain.model.BookSummary
import dev.audiobookplayer.platform.storage.CoverArtStore
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class RoomLibraryRepository(
    private val database: AppDatabase,
    private val bookDao: BookDao,
    private val chapterDao: ChapterDao,
    private val metadataExtractor: M4bMetadataExtractor,
    private val coverArtStore: CoverArtStore,
) : LibraryRepository {
    override fun observeLibrary(): Flow<List<BookSummary>> {
        return bookDao.observeAll().map { books -> books.map(BookEntity::toBookSummary) }
    }

    override fun observeBook(bookId: String): Flow<BookDetail?> {
        return combine(
            bookDao.observeById(bookId),
            chapterDao.observeByBookId(bookId),
        ) { book, chapters ->
            book?.toBookDetail(chapters)
        }
    }

    override suspend fun importBook(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val metadata = metadataExtractor.extract(uri)
        val existing = bookDao.getByContentUri(metadata.contentUri)
        val bookId = existing?.id ?: UUID.randomUUID().toString()
        val coverImagePath = coverArtStore.save(
            bookId = bookId,
            artworkBytes = metadata.embeddedArtwork,
        ) ?: existing?.coverImagePath
        val now = System.currentTimeMillis()

        val entity = BookEntity(
            id = bookId,
            contentUri = metadata.contentUri,
            displayName = metadata.displayName,
            title = metadata.title,
            author = metadata.author,
            durationMs = metadata.durationMs,
            coverImagePath = coverImagePath,
            fileSizeBytes = metadata.fileSizeBytes,
            mimeType = metadata.mimeType,
            dateImportedEpochMs = existing?.dateImportedEpochMs ?: now,
            lastPlayedAtEpochMs = existing?.lastPlayedAtEpochMs ?: now,
            currentPositionMs = existing?.currentPositionMs ?: 0L,
            playbackSpeed = existing?.playbackSpeed ?: 1f,
            hasChapters = metadata.chapters.isNotEmpty(),
        )
        val chapterEntities = metadata.chapters.mapIndexed { index, chapter ->
            ChapterEntity(
                bookId = bookId,
                chapterIndex = index,
                title = chapter.title,
                startPositionMs = chapter.startPositionMs,
            )
        }

        database.withTransaction {
            bookDao.upsert(entity)
            chapterDao.deleteByBookId(bookId)
            if (chapterEntities.isNotEmpty()) {
                chapterDao.insertAll(chapterEntities)
            }
        }
        ImportResult(
            bookId = bookId,
            wasUpdated = existing != null,
        )
    }

    override suspend fun getPlaybackSource(bookId: String): PlaybackSource? = withContext(Dispatchers.IO) {
        bookDao.getById(bookId)?.toPlaybackSource()
    }

    override suspend fun updatePlaybackState(
        bookId: String,
        positionMs: Long,
        lastPlayedAtEpochMs: Long,
        playbackSpeed: Float,
    ) = withContext(Dispatchers.IO) {
        bookDao.updatePlaybackState(
            bookId = bookId,
            positionMs = positionMs,
            lastPlayedAtEpochMs = lastPlayedAtEpochMs,
            playbackSpeed = playbackSpeed,
        )
    }
}
