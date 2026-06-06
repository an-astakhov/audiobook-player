package dev.audiobookplayer

import android.content.Context
import androidx.room.Room
import dev.audiobookplayer.data.db.AppDatabase
import dev.audiobookplayer.data.metadata.M4bMetadataExtractor
import dev.audiobookplayer.data.repository.LibraryRepository
import dev.audiobookplayer.data.repository.RoomLibraryRepository
import dev.audiobookplayer.playback.controller.AppPlaybackController
import dev.audiobookplayer.playback.controller.PlaybackController
import dev.audiobookplayer.playback.runtime.PlaybackRuntime
import dev.audiobookplayer.platform.storage.CoverArtStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val database: AppDatabase = Room.databaseBuilder(
        appContext,
        AppDatabase::class.java,
        "audiobook-player.db",
    )
        .fallbackToDestructiveMigration()
        .build()

    val libraryRepository: LibraryRepository = RoomLibraryRepository(
        bookDao = database.bookDao(),
        metadataExtractor = M4bMetadataExtractor(appContext),
        coverArtStore = CoverArtStore(appContext),
    )
    val playbackRuntime = PlaybackRuntime(
        context = appContext,
        libraryRepository = libraryRepository,
        appScope = applicationScope,
    )
    val playbackController: PlaybackController = AppPlaybackController(
        libraryRepository = libraryRepository,
        playbackRuntime = playbackRuntime,
    )
}
