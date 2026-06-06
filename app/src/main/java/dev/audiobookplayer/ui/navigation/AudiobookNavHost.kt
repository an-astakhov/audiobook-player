package dev.audiobookplayer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import dev.audiobookplayer.AppContainer
import dev.audiobookplayer.ui.AppNavigator
import dev.audiobookplayer.ui.book.BookRoute
import dev.audiobookplayer.ui.library.LibraryRoute

const val LibraryDestination = "library"

object BookRoutePattern {
    private const val Base = "book"
    const val BookIdArg = "bookId"
    const val Route = "$Base/{$BookIdArg}"

    fun create(bookId: String): String = "$Base/$bookId"
}

@Composable
fun AudiobookNavHost(
    navigator: AppNavigator,
    appContainer: AppContainer,
) {
    NavHost(
        navController = navigator.navController,
        startDestination = LibraryDestination,
    ) {
        composable(route = LibraryDestination) {
            LibraryRoute(
                appContainer = appContainer,
                onOpenBook = navigator::openBook,
                onImportBook = {},
            )
        }

        composable(
            route = BookRoutePattern.Route,
            arguments = listOf(navArgument(BookRoutePattern.BookIdArg) { type = NavType.StringType }),
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString(BookRoutePattern.BookIdArg).orEmpty()
            BookRoute(
                bookId = bookId,
                appContainer = appContainer,
                onNavigateBack = navigator::navigateBack,
            )
        }
    }
}
