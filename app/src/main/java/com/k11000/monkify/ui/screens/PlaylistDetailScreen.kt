package com.k11000.monkify.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.k11000.monkify.domain.model.TrackSortOption
import com.k11000.monkify.ui.components.PlaylistCollageArtwork
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.k11000.monkify.playback.PlayerViewModel
import com.k11000.monkify.playback.PlaybackSource
import com.k11000.monkify.ui.screens.viewmodel.PlaylistDetailViewModel
import com.k11000.monkify.ui.theme.MonkifyBackground
import com.k11000.monkify.ui.theme.MonkifyDarkGray
import com.k11000.monkify.ui.theme.MonkifyGray
import com.k11000.monkify.ui.theme.MonkifyOnBackground
import com.k11000.monkify.ui.theme.MonkifyPrimary
import com.k11000.monkify.ui.theme.MonkifySurface

@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    initialName: String = "",
    onBack: () -> Unit = {},
    viewModel: PlaylistDetailViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val playlistName by viewModel.playlistName.collectAsState()
    val tracks by viewModel.tracks.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val filteredAndSortedTracks by viewModel.filteredAndSortedTracks.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }
    val isLoading by viewModel.isLoading.collectAsState()
    val downloadedTrackIds by viewModel.downloadedTrackIds.collectAsState()
    val isDownloadingAll by viewModel.isDownloadingAll.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }


    LaunchedEffect(playlistId) {
        viewModel.loadPlaylist(playlistId, initialName)
    }

    val displayName = playlistName.ifBlank { initialName.ifBlank { "Playlist" } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MonkifyBackground)
            .padding(top = statusBarPadding + 8.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = MonkifyOnBackground
                )
            }
            Text(
                text = "Playlist",
                color = MonkifyOnBackground,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(start = 8.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar playlist",
                    tint = MonkifyGray
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header: Carátula, Nombre y Acción Reproducir Todo
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PlaylistCollageArtwork(
                        coverUrls = tracks.mapNotNull { it.coverUrl },
                        contentDescription = "Carátula de playlist",
                        modifier = Modifier.size(160.dp),
                        shape = RoundedCornerShape(16.dp),
                        iconSize = 80.dp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = displayName,
                        color = MonkifyOnBackground,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "${tracks.size} canciones",
                        color = MonkifyGray,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (tracks.isNotEmpty()) {
                        val allDownloaded = tracks.all { it.id in downloadedTrackIds }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    val source = PlaybackSource.playlist(playlistId, playlistName.ifBlank { initialName })
                                    playerViewModel.playQueue(filteredAndSortedTracks, 0, source)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MonkifyPrimary,
                                    contentColor = Color.Black
                                ),
                                shape = CircleShape,
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "Reproducir Todo", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    if (isDownloadingAll) {
                                        viewModel.cancelDownloadAll()
                                        Toast.makeText(context, "Descarga de playlist cancelada", Toast.LENGTH_SHORT).show()
                                    } else if (!allDownloaded) {
                                        viewModel.downloadAllTracks { res ->
                                            res.fold(
                                                onSuccess = {
                                                    Toast.makeText(context, "Descargando playlist en segundo plano...", Toast.LENGTH_SHORT).show()
                                                },
                                                onFailure = { err ->
                                                    Toast.makeText(context, err.message ?: "Error al descargar", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }
                                    } else {
                                        Toast.makeText(context, "Todas las canciones están descargadas", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (allDownloaded) MonkifyPrimary else MonkifyOnBackground
                                ),
                                border = BorderStroke(1.dp, if (allDownloaded) MonkifyPrimary else MonkifyDarkGray),
                                shape = CircleShape,
                                modifier = Modifier.height(48.dp)
                            ) {
                                if (isDownloadingAll) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = MonkifyPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${downloadProgress.first}/${downloadProgress.second}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (allDownloaded) Icons.Default.DownloadDone else Icons.Default.Download,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = if (allDownloaded) MonkifyPrimary else MonkifyGray
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (allDownloaded) "Descargada" else "Descargar",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (allDownloaded) MonkifyPrimary else MonkifyOnBackground
                                        )
                                    )
                                }
                            }
                        }
                    }

                    if (tracks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))

                        // Buscador interno y selector de ordenamiento
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextField(
                                value = searchQuery,
                                onValueChange = { viewModel.onSearchQueryChange(it) },
                                placeholder = {
                                    Text(
                                        text = "Buscar en esta playlist...",
                                        color = MonkifyGray,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Buscar",
                                        tint = MonkifyGray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotBlank()) {
                                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Limpiar",
                                                tint = MonkifyGray,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MonkifyDarkGray,
                                    unfocusedContainerColor = MonkifyDarkGray,
                                    focusedTextColor = MonkifyOnBackground,
                                    unfocusedTextColor = MonkifyOnBackground,
                                    cursorColor = MonkifyPrimary,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            )

                            Box {
                                IconButton(
                                    onClick = { showSortMenu = true },
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MonkifyDarkGray)
                                        .size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sort,
                                        contentDescription = "Ordenar canciones",
                                        tint = if (sortOption != TrackSortOption.DEFAULT) MonkifyPrimary else MonkifyOnBackground
                                    )
                                }
                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false },
                                    modifier = Modifier.background(MonkifyDarkGray)
                                ) {
                                    TrackSortOption.values().forEach { option ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = option.displayName,
                                                    color = if (sortOption == option) MonkifyPrimary else MonkifyOnBackground,
                                                    fontWeight = if (sortOption == option) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            onClick = {
                                                viewModel.setSortOption(option)
                                                showSortMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Lista de canciones
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MonkifyPrimary)
                    }
                }
            } else if (filteredAndSortedTracks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "No se encontraron canciones coincidentes" else "Esta playlist no tiene canciones aún 🎵",
                            color = MonkifyGray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                itemsIndexed(filteredAndSortedTracks, key = { _, track -> track.id }) { index, track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val source = PlaybackSource.playlist(playlistId, playlistName.ifBlank { initialName })
                                playerViewModel.playQueue(filteredAndSortedTracks, index, source)
                            }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Posición / Número
                        Text(
                            text = "${index + 1}",
                            color = MonkifyGray,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.width(28.dp)
                        )

                        // Carátula pequeña o Icono
                        com.k11000.monkify.ui.components.TrackArtwork(
                            coverUrl = track.coverUrl,
                            contentDescription = track.title,
                            shape = RoundedCornerShape(8.dp),
                            iconSize = 22.dp,
                            modifier = Modifier.size(46.dp)
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        // Título y Artista
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                color = MonkifyOnBackground,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = track.artist,
                                color = MonkifyGray,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (track.id in downloadedTrackIds) {
                            Icon(
                                imageVector = Icons.Default.DownloadDone,
                                contentDescription = "Descargada offline",
                                tint = MonkifyPrimary,
                                modifier = Modifier
                                    .padding(horizontal = 6.dp)
                                    .size(18.dp)
                            )
                        }

                        // Botón Quitar de Playlist
                        IconButton(
                            onClick = {
                                viewModel.removeTrack(track.id) { result ->
                                    result.fold(
                                        onSuccess = {
                                            Toast.makeText(context, "Canción quitada de la playlist", Toast.LENGTH_SHORT).show()
                                        },
                                        onFailure = { err ->
                                            Toast.makeText(context, err.message ?: "Error al quitar canción", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Quitar de playlist",
                                tint = MonkifyGray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }


            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = MonkifySurface,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = "Eliminar playlist",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MonkifyOnBackground
                )
            },
            text = {
                Text(
                    text = "¿Estás seguro de que deseas eliminar \"$displayName\"? Esta acción no se puede deshacer.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MonkifyGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deletePlaylist { result ->
                            result.fold(
                                onSuccess = {
                                    Toast.makeText(context, "Playlist eliminada", Toast.LENGTH_SHORT).show()
                                    onBack()
                                },
                                onFailure = { err ->
                                    Toast.makeText(context, err.message ?: "Error al eliminar playlist", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53935),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Eliminar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar", color = MonkifyGray)
                }
            }
        )
    }
}
