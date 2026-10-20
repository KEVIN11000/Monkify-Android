package com.k11000.monkify.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.k11000.monkify.playback.PlayerViewModel
import com.k11000.monkify.ui.screens.viewmodel.SettingsViewModel
import com.k11000.monkify.ui.theme.MonkifyDarkGray
import com.k11000.monkify.ui.theme.MonkifyGray
import com.k11000.monkify.ui.theme.MonkifyOnBackground
import com.k11000.monkify.ui.theme.MonkifyPrimary

@Composable
fun MiniPlayer(
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    isZenPeek: Boolean = false,
    onRestoreZen: () -> Unit = {},
    onExpand: () -> Unit = {}
) {
    val isPlaying by viewModel.isPlaying.collectAsState()
    val title by viewModel.currentTrackTitle.collectAsState()
    val artist by viewModel.currentTrackArtist.collectAsState()
    val coverUrl by viewModel.coverUrl.collectAsState()
    val progress by viewModel.progress.collectAsState()

    if (title.isBlank()) return

    AnimatedContent(
        targetState = isZenPeek,
        transitionSpec = {
            (slideInVertically { it / 2 } + fadeIn(tween(250)))
                .togetherWith(slideOutVertically { it / 2 } + fadeOut(tween(250)))
        },
        label = "MiniPlayerZenPeek"
    ) { peekMode ->
        if (peekMode) {
            // Modo Peek en Modo Zen: lengüeta sutil asomando desde abajo (18-20dp)
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable(onClick = onRestoreZen),
                contentAlignment = Alignment.Center
            ) {
                // Tirador central
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(MonkifyGray.copy(alpha = 0.7f))
                )
                // Línea inferior de progreso discreta
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                        .height(2.dp)
                        .background(MonkifyPrimary)
                )
            }
        } else {
            // Modo normal completo del MiniPlayer
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MonkifyDarkGray)
                    .clickable(onClick = onExpand)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        // Cover
                        TrackArtwork(
                            coverUrl = coverUrl,
                            contentDescription = "Album Cover",
                            shape = RoundedCornerShape(6.dp),
                            iconSize = 22.dp,
                            modifier = Modifier.size(42.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = title,
                                color = MonkifyOnBackground,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.basicMarquee()
                            )
                            Text(
                                text = artist,
                                color = MonkifyPrimary,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Play / Pause
                        IconButton(onClick = { viewModel.togglePlayPause() }) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = MonkifyOnBackground
                            )
                        }
                    }

                    // Barra inferior de progreso
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(Color(0xFF3E3E3E))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                                .height(2.dp)
                                .background(MonkifyPrimary)
                        )
                    }
                }
            }
        }
    }
}
