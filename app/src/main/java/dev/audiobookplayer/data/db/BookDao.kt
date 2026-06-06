package dev.audiobookplayer.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query(
        """
        SELECT * FROM books
        ORDER BY lastPlayedAtEpochMs DESC, dateImportedEpochMs DESC
        """,
    )
    fun observeAll(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :bookId LIMIT 1")
    fun observeById(bookId: String): Flow<BookEntity?>

    @Query("SELECT * FROM books WHERE id = :bookId LIMIT 1")
    suspend fun getById(bookId: String): BookEntity?

    @Query("SELECT * FROM books WHERE contentUri = :contentUri LIMIT 1")
    suspend fun getByContentUri(contentUri: String): BookEntity?

    @Query("DELETE FROM books WHERE id = :bookId")
    suspend fun deleteById(bookId: String)

    @Query(
        """
        UPDATE books
        SET currentPositionMs = :positionMs,
            lastPlayedAtEpochMs = :lastPlayedAtEpochMs,
            playbackSpeed = :playbackSpeed
        WHERE id = :bookId
        """,
    )
    suspend fun updatePlaybackState(
        bookId: String,
        positionMs: Long,
        lastPlayedAtEpochMs: Long,
        playbackSpeed: Float,
    )

    @Upsert
    suspend fun upsert(book: BookEntity)
}
