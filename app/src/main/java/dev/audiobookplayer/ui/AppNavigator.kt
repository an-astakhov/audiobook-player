package dev.audiobookplayer.ui

import androidx.navigation.NavHostController
import dev.audiobookplayer.ui.navigation.BookRoutePattern
import dev.audiobookplayer.ui.navigation.LibraryDestination

class AppNavigator(
    val navController: NavHostController,
) {
    fun openLibrary() {
        navController.navigate(LibraryDestination) {
            popUpTo(navController.graph.startDestinationId) {
                inclusive = false
            }
            launchSingleTop = true
        }
    }

    fun openBook(bookId: String) {
        navController.navigate(BookRoutePattern.create(bookId))
    }

    fun navigateBack() {
        navController.popBackStack()
    }
}
