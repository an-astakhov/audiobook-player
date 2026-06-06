package dev.audiobookplayer.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import dev.audiobookplayer.AppContainer
import dev.audiobookplayer.ui.navigation.LibraryDestination
import dev.audiobookplayer.ui.navigation.AudiobookNavHost
import dev.audiobookplayer.ui.theme.AudiobookPlayerTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AudiobookPlayerRoot(appContainer: AppContainer) {
    AudiobookPlayerTheme {
        val navController = rememberNavController()
        val navigator = remember(navController) { AppNavigator(navController) }
        val playbackState by appContainer.playbackController.state.collectAsStateWithLifecycle()
        val backStackEntry by navController.currentBackStackEntryAsState()
        var restoredBookOpened by rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }

        LaunchedEffect(playbackState.activeBookId, backStackEntry?.destination?.route, restoredBookOpened) {
            val activeBookId = playbackState.activeBookId ?: return@LaunchedEffect
            if (restoredBookOpened) return@LaunchedEffect
            if (backStackEntry?.destination?.route == LibraryDestination) {
                restoredBookOpened = true
                navigator.openBook(activeBookId)
            }
        }

        AudiobookNavHost(
            navigator = navigator,
            appContainer = appContainer,
        )
    }
}
