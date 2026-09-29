package com.k11000.monkify.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.k11000.monkify.ui.theme.MonkifyBackground
import com.k11000.monkify.ui.theme.MonkifyGray
import com.k11000.monkify.ui.theme.MonkifyOnBackground
import com.k11000.monkify.ui.theme.MonkifyPrimary

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import com.k11000.monkify.playback.PlayerViewModel

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import com.k11000.monkify.ui.theme.MonkifyDarkGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenPlayer(
    onClose: () -> Unit = {},
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val isPlaying by viewModel.isPlaying.collectAsState()
    val title by viewModel.currentTrackTitle.collectAsState()
    val artist by viewModel.currentTrackArtist.collectAsState()
    val coverUrl by viewModel.coverUrl.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val devices by viewModel.devices.collectAsState()
    val activeDeviceName by viewModel.activeDeviceName.collectAsState()

    var showDeviceSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MonkifyBackground)
            .padding(24.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Close",
                    tint = MonkifyOnBackground
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "REPRODUCIENDO EN",
                    color = MonkifyGray,
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = activeDeviceName,
                    color = MonkifyPrimary,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            IconButton(onClick = { showDeviceSheet = true }) {
                Icon(
                    imageVector = Icons.Default.Devices,
                    contentDescription = "Monkify Connect",
                    tint = MonkifyPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Large Cover Image
        AsyncImage(
            model = coverUrl ?: "https://via.placeholder.com/500",
            contentDescription = "Album Cover",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.DarkGray)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Title and Artist
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = MonkifyOnBackground,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = artist,
                    color = MonkifyGray,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Progress Slider
        var sliderPosition by remember { mutableStateOf<Float?>(null) }
        
        val currentProgress = if (duration > 0) {
            (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        } else 0f

        Slider(
            value = sliderPosition ?: currentProgress,
            onValueChange = { newProgress -> 
                sliderPosition = newProgress
            },
            onValueChangeFinished = {
                sliderPosition?.let { newProgress ->
                    val newPosition = (newProgress * duration).toLong()
                    viewModel.seekTo(newPosition)
                    sliderPosition = null
                }
            },
            colors = SliderDefaults.colors(
                thumbColor = MonkifyOnBackground,
                activeTrackColor = MonkifyOnBackground,
                inactiveTrackColor = MonkifyGray.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = formatTime(currentPosition), color = MonkifyGray, style = MaterialTheme.typography.labelSmall)
            Text(text = formatTime(duration), color = MonkifyGray, style = MaterialTheme.typography.labelSmall)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Playback Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { /* TODO: Shuffle */ }) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = MonkifyPrimary // Active state
                )
            }
            IconButton(onClick = { /* TODO: Prev */ }) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    tint = MonkifyOnBackground,
                    modifier = Modifier.size(40.dp)
                )
            }
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MonkifyPrimary)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = { viewModel.togglePlayPause() }) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = MonkifyBackground,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            IconButton(onClick = { /* TODO: Next */ }) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = MonkifyOnBackground,
                    modifier = Modifier.size(40.dp)
                )
            }
            IconButton(onClick = { /* TODO: Repeat */ }) {
                Icon(
                    imageVector = Icons.Default.Repeat,
                    contentDescription = "Repeat",
                    tint = MonkifyGray
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Bottom Lyrics Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(onClick = { /* TODO: Open Lyrics */ }) {
                Icon(
                    imageVector = Icons.Default.Lyrics,
                    contentDescription = "Lyrics",
                    tint = MonkifyOnBackground
                )
            }
        }
    }

    // Monkify Connect Devices Modal
    if (showDeviceSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDeviceSheet = false },
            sheetState = sheetState,
            containerColor = MonkifyDarkGray
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Monkify Connect 🎧",
                    color = MonkifyOnBackground,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Escucha en otro dispositivo en tiempo real",
                    color = MonkifyGray,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(16.dp))

                devices.forEach { device ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (device.isSelected) MonkifyPrimary.copy(alpha = 0.2f) else Color.Transparent)
                            .clickable {
                                viewModel.selectDevice(device.id)
                                showDeviceSheet = false
                            }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Devices,
                                contentDescription = null,
                                tint = if (device.isSelected) MonkifyPrimary else MonkifyOnBackground
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = device.name,
                                color = if (device.isSelected) MonkifyPrimary else MonkifyOnBackground,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (device.isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                        if (device.isSelected) {
                            Text(
                                text = "Conectado",
                                color = MonkifyPrimary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
