package dev.audiobookplayer

import android.app.Application

class AudiobookPlayerApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}
