package dev.audiobookplayer.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterDao {
    @Query(
        """
        SELECT * FROM chapters
        WHERE bookId = :bookId
        ORDER BY chapterIndex ASC
        """,
    )
    fun observeByBookId(bookId: String): Flow<List<ChapterEntity>>

    @Query(
        """
        SELECT * FROM chapters
        WHERE bookId = :bookId
        ORDER BY chapterIndex ASC
        """,
    )
    suspend fun getByBookId(bookId: String): List<ChapterEntity>

    @Query("DELETE FROM chapters WHERE bookId = :bookId")
    suspend fun deleteByBookId(bookId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chapters: List<ChapterEntity>)
}
