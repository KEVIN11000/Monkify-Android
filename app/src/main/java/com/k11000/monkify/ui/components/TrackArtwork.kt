package com.k11000.monkify.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import coil3.compose.AsyncImage
import com.k11000.monkify.ui.theme.MonkifyDarkGray
import com.k11000.monkify.ui.theme.MonkifyPrimary

@Composable
fun TrackArtwork(
    coverUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(6.dp),
    iconSize: Dp = 24.dp,
    isAlbum: Boolean = false
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(MonkifyDarkGray, Color(0xFF1E293B))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isAlbum) Icons.Default.Album else Icons.Default.MusicNote,
            contentDescription = null,
            tint = MonkifyPrimary.copy(alpha = 0.7f),
            modifier = Modifier.size(iconSize)
        )

        if (!coverUrl.isNullOrBlank()) {
            AsyncImage(
                model = coverUrl,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun PlaylistCollageArtwork(
    coverUrls: List<String>?,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(6.dp),
    iconSize: Dp = 24.dp
) {
    val validUrls = coverUrls?.filter { it.isNotBlank() } ?: emptyList()

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(MonkifyDarkGray, Color(0xFF1E293B))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        when {
            validUrls.isEmpty() -> {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MonkifyPrimary.copy(alpha = 0.7f),
                    modifier = Modifier.size(iconSize)
                )
            }
            validUrls.size in 1..3 -> {
                AsyncImage(
                    model = validUrls.first(),
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            validUrls.size in 4..5 -> {
                // Cuadrícula 2x2 (primeras 4 portadas)
                Column(modifier = Modifier.fillMaxSize()) {
                    validUrls.take(4).chunked(2).forEach { rowUrls ->
                        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            rowUrls.forEach { url ->
                                AsyncImage(
                                    model = url,
                                    contentDescription = contentDescription,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                )
                            }
                        }
                    }
                }
            }
            else -> {
                // Cuadrícula 3x2 (primeras 6 portadas)
                Column(modifier = Modifier.fillMaxSize()) {
                    validUrls.take(6).chunked(3).forEach { rowUrls ->
                        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            rowUrls.forEach { url ->
                                AsyncImage(
                                    model = url,
                                    contentDescription = contentDescription,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
