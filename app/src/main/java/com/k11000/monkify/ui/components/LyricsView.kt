package com.k11000.monkify.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.k11000.monkify.playback.lyrics.LyricLine
import com.k11000.monkify.playback.lyrics.LrcParser
import com.k11000.monkify.ui.theme.MonkifyGray
import com.k11000.monkify.ui.theme.MonkifyPrimary

@Composable
fun LyricsView(
    lyrics: List<LyricLine>,
    currentPositionMs: Long,
    onLineClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (lyrics.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Letra no disponible para esta pista 🎵",
                color = MonkifyGray,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(24.dp)
            )
        }
        return
    }

    val isStatic = lyrics.all { it.timestampMs < 0L }
    val activeIndex = if (isStatic) -1 else LrcParser.getCurrentLineIndex(lyrics, currentPositionMs)
    val listState = rememberLazyListState()

    LaunchedEffect(activeIndex) {
        if (!isStatic && activeIndex in lyrics.indices) {
            val targetIndex = (activeIndex - 2).coerceAtLeast(0)
            listState.animateScrollToItem(targetIndex)
        }
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(vertical = 32.dp, horizontal = 16.dp),
        modifier = modifier.fillMaxSize()
    ) {
        itemsIndexed(lyrics, key = { index, line -> "${line.timestampMs}_$index" }) { index, line ->
            val isActive = !isStatic && index == activeIndex
            val textColor by animateColorAsState(
                targetValue = when {
                    isActive -> MonkifyPrimary
                    isStatic -> Color.White.copy(alpha = 0.9f)
                    else -> MonkifyGray.copy(alpha = 0.5f)
                },
                animationSpec = tween(durationMillis = 250),
                label = "lyric_color"
            )

            Text(
                text = line.text.ifBlank { "♪ ♪ ♪" },
                color = textColor,
                fontSize = if (isActive) 22.sp else 18.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (line.timestampMs >= 0L) {
                            Modifier.clickable { onLineClick(line.timestampMs) }
                        } else {
                            Modifier
                        }
                    )
                    .padding(vertical = 8.dp)
            )
        }
    }
}
