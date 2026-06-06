package dev.audiobookplayer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Clay,
    onPrimary = WarmWhite,
    secondary = Moss,
    onSecondary = WarmWhite,
    background = Paper,
    onBackground = Ink,
    surface = WarmWhite,
    onSurface = Ink,
    secondaryContainer = Sand,
    onSecondaryContainer = Ink,
    tertiary = Stone,
    onTertiary = WarmWhite,
    onSurfaceVariant = Stone,
)

private val DarkColors = darkColorScheme(
    primary = Sand,
    onPrimary = Ink,
    secondary = ColorPalette.darkMoss,
    onSecondary = WarmWhite,
    background = ColorPalette.darkBackground,
    onBackground = WarmWhite,
    surface = ColorPalette.darkSurface,
    onSurface = WarmWhite,
    secondaryContainer = ColorPalette.darkContainer,
    onSecondaryContainer = WarmWhite,
    tertiary = ColorPalette.darkStone,
    onTertiary = WarmWhite,
    onSurfaceVariant = ColorPalette.darkStone,
)

private object ColorPalette {
    val darkBackground = androidx.compose.ui.graphics.Color(0xFF171311)
    val darkSurface = androidx.compose.ui.graphics.Color(0xFF211C19)
    val darkContainer = androidx.compose.ui.graphics.Color(0xFF3A3029)
    val darkMoss = androidx.compose.ui.graphics.Color(0xFF8FA68B)
    val darkStone = androidx.compose.ui.graphics.Color(0xFFC8BDB1)
}

@Composable
fun AudiobookPlayerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AudiobookTypography,
        content = content,
    )
}

