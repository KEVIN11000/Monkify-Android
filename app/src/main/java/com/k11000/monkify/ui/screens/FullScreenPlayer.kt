package com.k11000.monkify.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import com.k11000.monkify.domain.model.DownloadState
import com.k11000.monkify.playback.PlayerViewModel
import com.k11000.monkify.ui.components.AddToPlaylistDialog
import com.k11000.monkify.ui.components.CreatePlaylistDialog
import com.k11000.monkify.ui.components.LyricsView
import com.k11000.monkify.ui.components.ZenBackground
import com.k11000.monkify.ui.theme.MonkifyBackground
import com.k11000.monkify.ui.theme.MonkifyDarkGray
import com.k11000.monkify.ui.theme.MonkifyGray
import com.k11000.monkify.ui.theme.MonkifyOnBackground
import com.k11000.monkify.ui.theme.MonkifyPrimary


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
    val isShuffleEnabled by viewModel.isShuffleEnabled.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val isFavorite by viewModel.isCurrentFavorite.collectAsState()
    val isDownloaded by viewModel.isCurrentDownloaded.collectAsState()
    val isDownloading by viewModel.isCurrentDownloading.collectAsState()
    val downloadState by viewModel.currentDownloadState.collectAsState()
    val lyrics by viewModel.lyrics.collectAsState()
    val availablePlaylists by viewModel.availablePlaylists.collectAsState()
    val zenModeEnabled by viewModel.zenModeEnabled.collectAsState()
    val zenEngine by viewModel.zenEngine.collectAsState()

    val context = LocalContext.current
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showDeleteDownloadDialog by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var showDeviceSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    LaunchedEffect(downloadState) {
        if (downloadState is DownloadState.Error) {
            val errorMsg = (downloadState as DownloadState.Error).message
            Toast.makeText(context, "Error en descarga: $errorMsg", Toast.LENGTH_SHORT).show()
        }
    }


    fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MonkifyBackground)
    ) {
        if (zenModeEnabled) {
            ZenBackground(
                isPlaying = isPlaying,
                engine = zenEngine,
                modifier = Modifier.fillMaxSize()
            )
            // Viñeta ambiental superior e inferior para óptimo contraste de tipografías y controles
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.55f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.75f)
                            )
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = statusBarPadding + 12.dp, start = 24.dp, end = 24.dp, bottom = 24.dp)
        ) {
        // Barra Superior
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Cerrar reproductor",
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

        Spacer(modifier = Modifier.height(28.dp))

        // Área Central: Carátula o Letras Sincronizadas con transición fluida
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = showLyrics,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.95f, animationSpec = tween(300)))
                        .togetherWith(fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.95f, animationSpec = tween(200)))
                },
                label = "CoverLyricsTransition"
            ) { isLyricsVisible ->
                if (isLyricsVisible) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MonkifyDarkGray.copy(alpha = 0.5f))
                    ) {
                        LyricsView(
                            lyrics = lyrics,
                            currentPositionMs = currentPosition,
                            onLineClick = { timestampMs ->
                                viewModel.seekTo(timestampMs)
                            }
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        // Sombra / Aura difusa de resplandor ambiental
                        Box(
                            modifier = Modifier
                                .fillMaxSize(0.94f)
                                .clip(RoundedCornerShape(24.dp))
                                .background(MonkifyPrimary.copy(alpha = 0.22f))
                        )
                        AsyncImage(
                            model = coverUrl ?: "https://via.placeholder.com/500",
                            contentDescription = "Carátula de álbum",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.DarkGray)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Título y Artista con acciones de Favorito y Descarga
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

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Botón Añadir a Playlist
                IconButton(onClick = {
                    viewModel.loadPlaylistsForDialog()
                    showAddToPlaylistDialog = true
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                        contentDescription = "Añadir a playlist",
                        tint = MonkifyGray
                    )
                }

                // Botón Descargar
                IconButton(onClick = {
                    when (downloadState) {
                        is DownloadState.Downloaded -> {
                            showDeleteDownloadDialog = true
                        }
                        is DownloadState.Downloading -> {
                            val progress = (downloadState as DownloadState.Downloading).progressPercent
                            Toast.makeText(context, "Descargando pista ($progress%)...", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            viewModel.toggleDownload()
                        }
                    }
                }) {
                    when (val state = downloadState) {
                        is DownloadState.Downloading -> {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(28.dp)) {
                                if (state.progressPercent > 0) {
                                    CircularProgressIndicator(
                                        progress = { state.progressPercent / 100f },
                                        modifier = Modifier.size(28.dp),
                                        color = MonkifyPrimary,
                                        strokeWidth = 2.5.dp
                                    )
                                    Text(
                                        text = "${state.progressPercent}%",
                                        color = MonkifyPrimary,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                } else {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = MonkifyPrimary,
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }
                        is DownloadState.Downloaded -> {
                            Icon(
                                imageVector = Icons.Default.DownloadDone,
                                contentDescription = "Descargada (clic para eliminar)",
                                tint = MonkifyPrimary
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Descargar pista",
                                tint = MonkifyGray
                            )
                        }
                    }
                }


                // Botón Favorito
                IconButton(onClick = { viewModel.toggleFavorite() }) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "Quitar de favoritos" else "Marcar como favorito",
                        tint = if (isFavorite) Color(0xFFFF4081) else MonkifyGray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Slider de Progreso en Azul Eléctrico
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
                thumbColor = MonkifyPrimary,
                activeTrackColor = MonkifyPrimary,
                inactiveTrackColor = MonkifyDarkGray
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

        Spacer(modifier = Modifier.height(20.dp))

        // Botonera de Transporte Multimedia Completa
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shuffle (Aleatorio)
            IconButton(onClick = { viewModel.toggleShuffle() }) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Aleatorio",
                    tint = if (isShuffleEnabled) MonkifyPrimary else MonkifyGray
                )
            }

            // Anterior (Skip Previous)
            IconButton(onClick = { viewModel.skipToPrevious() }) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Pista anterior",
                    tint = MonkifyOnBackground,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Play / Pause Central
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MonkifyPrimary)
                    .clickable { viewModel.togglePlayPause() }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Reproducir o Pausar",
                    tint = MonkifyBackground,
                    modifier = Modifier.size(38.dp)
                )
            }

            // Siguiente (Skip Next)
            IconButton(onClick = { viewModel.skipToNext() }) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Pista siguiente",
                    tint = MonkifyOnBackground,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Repetir (Repeat Mode)
            IconButton(onClick = { viewModel.toggleRepeatMode() }) {
                Icon(
                    imageVector = if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                    contentDescription = "Repetir",
                    tint = if (repeatMode != Player.REPEAT_MODE_OFF) MonkifyPrimary else MonkifyGray
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Barra Inferior de Letras (Lyrics)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(onClick = { showLyrics = !showLyrics }) {
                Icon(
                    imageVector = Icons.Default.Lyrics,
                    contentDescription = if (showLyrics) "Ocultar letras" else "Ver letras",
                    tint = if (showLyrics) MonkifyPrimary else MonkifyGray
                )
            }
        }
    }

    // Modal de Selección de Dispositivos (Monkify Connect)
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

    if (showAddToPlaylistDialog) {
        AddToPlaylistDialog(
            playlists = availablePlaylists,
            onDismiss = { showAddToPlaylistDialog = false },
            onSelectPlaylist = { playlistId ->
                viewModel.addCurrentTrackToPlaylist(playlistId) { result ->
                    result.fold(
                        onSuccess = {
                            Toast.makeText(context, "Añadida a la playlist", Toast.LENGTH_SHORT).show()
                            showAddToPlaylistDialog = false
                        },
                        onFailure = { err ->
                            Toast.makeText(context, err.message ?: "Error al añadir a playlist", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            },
            onCreateNewPlaylist = {
                showAddToPlaylistDialog = false
                showCreatePlaylistDialog = true
            }
        )
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onConfirm = { name ->
                viewModel.createPlaylistAndAddCurrentTrack(name) { result ->
                    result.fold(
                        onSuccess = {
                            Toast.makeText(context, "Playlist creada y canción añadida", Toast.LENGTH_SHORT).show()
                            showCreatePlaylistDialog = false
                        },
                        onFailure = { err ->
                            Toast.makeText(context, err.message ?: "Error al crear playlist", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        )
    }

    if (showDeleteDownloadDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDownloadDialog = false },
            containerColor = MonkifyDarkGray,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = "Eliminar descarga",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MonkifyOnBackground
                )
            },
            text = {
                Text(
                    text = "¿Deseas eliminar \"$title\" del almacenamiento local de tu dispositivo?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MonkifyGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDownloadDialog = false
                        viewModel.toggleDownload()
                        Toast.makeText(context, "Descarga eliminada", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("Eliminar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDownloadDialog = false }) {
                    Text("Cancelar", color = MonkifyGray)
                }
            }
        )
    }
}
}

