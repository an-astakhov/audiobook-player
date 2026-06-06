package dev.audiobookplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import dev.audiobookplayer.ui.AudiobookPlayerRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            AudiobookPlayerRoot(
                appContainer = (application as AudiobookPlayerApplication).appContainer,
            )
        }
    }
}

