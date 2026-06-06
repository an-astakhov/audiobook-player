package dev.audiobookplayer

import dev.audiobookplayer.data.repository.InMemoryLibraryRepository
import dev.audiobookplayer.data.repository.LibraryRepository
import dev.audiobookplayer.playback.controller.PlaybackController
import dev.audiobookplayer.playback.controller.StubPlaybackController

class AppContainer {
    val libraryRepository: LibraryRepository = InMemoryLibraryRepository()
    val playbackController: PlaybackController = StubPlaybackController()
}

