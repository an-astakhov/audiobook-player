package dev.audiobookplayer.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun BookCoverArtwork(
    title: String,
    coverImagePath: String?,
    modifier: Modifier = Modifier,
) {
    val imageBitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(
        initialValue = null,
        key1 = coverImagePath,
    ) {
        value = withContext(Dispatchers.IO) {
            coverImagePath
                ?.takeIf(String::isNotBlank)
                ?.let(BitmapFactory::decodeFile)
                ?.asImageBitmap()
        }
    }

    Box(
        modifier = modifier.background(
            brush = Brush.linearGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.95f),
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f),
                ),
            ),
        ),
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap!!,
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f),
                        ),
                    ),
                )
                .padding(12.dp),
        ) {
            if (imageBitmap == null) {
                Text(
                    text = "AUDIO\nBOOK",
                    modifier = Modifier.align(Alignment.BottomStart),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start,
                )
            }
        }
    }
}

