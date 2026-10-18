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
