package dev.audiobookplayer.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.compose.rememberNavController
import dev.audiobookplayer.AppContainer
import dev.audiobookplayer.ui.navigation.AudiobookNavHost
import dev.audiobookplayer.ui.theme.AudiobookPlayerTheme

@Composable
fun AudiobookPlayerRoot(appContainer: AppContainer) {
    AudiobookPlayerTheme {
        val navController = rememberNavController()
        val navigator = remember(navController) { AppNavigator(navController) }

        AudiobookNavHost(
            navigator = navigator,
            appContainer = appContainer,
        )
    }
}

