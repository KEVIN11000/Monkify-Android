package com.k11000.monkify.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import android.widget.Toast
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext

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
import com.k11000.monkify.domain.model.Track
import com.k11000.monkify.playback.PlayerViewModel
import com.k11000.monkify.ui.screens.viewmodel.LibraryFilter
import com.k11000.monkify.ui.screens.viewmodel.LibraryViewModel
import com.k11000.monkify.ui.theme.MonkifyBackground
import com.k11000.monkify.ui.theme.MonkifyDarkGray
import com.k11000.monkify.ui.theme.MonkifyGray
import com.k11000.monkify.ui.theme.MonkifyOnBackground
import com.k11000.monkify.ui.theme.MonkifyPrimary

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.k11000.monkify.data.remote.dto.AlbumSummaryDto
import com.k11000.monkify.data.remote.dto.PlaylistSummaryDto
import com.k11000.monkify.ui.components.CreatePlaylistDialog
import com.k11000.monkify.ui.components.TrackArtwork

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val mb = bytes.toDouble() / (1024.0 * 1024.0)
    return if (mb < 1024.0) {
        String.format(java.util.Locale.US, "%.1f MB", mb)
    } else {
        val gb = mb / 1024.0
        String.format(java.util.Locale.US, "%.2f GB", gb)
    }
}

@Composable
fun LibraryScreen(
    onNavigateToPlaylist: (String, String) -> Unit = { _, _ -> },
    onNavigateToAlbum: (String, String, String?) -> Unit = { _, _, _ -> },
    viewModel: LibraryViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val displayedTracks by viewModel.displayedTracks.collectAsState()
    val favoriteTracks by viewModel.favoriteTracks.collectAsState()
    val downloadedTracks by viewModel.downloadedTracks.collectAsState()
    val totalStorageBytes by viewModel.totalStorageBytes.collectAsState()
    val playlists by viewModel.filteredPlaylists.collectAsState()
    val isLoadingPlaylists by viewModel.isLoadingPlaylists.collectAsState()
    val albums by viewModel.filteredAlbums.collectAsState()
    val isLoadingAlbums by viewModel.isLoadingAlbums.collectAsState()
    val downloadedIds = remember(downloadedTracks) { downloadedTracks.map { it.id }.toSet() }
    val context = LocalContext.current
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var isSearchVisible by remember { mutableStateOf(false) }
    var trackToDeleteDownload by remember { mutableStateOf<Track?>(null) }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MonkifyBackground)
            .padding(top = statusBarPadding + 16.dp, bottom = 16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF59E0B)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "K", color = Color.Black, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Tu biblioteca",
                color = MonkifyOnBackground,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = {
                isSearchVisible = !isSearchVisible
                if (!isSearchVisible) {
                    viewModel.onSearchQueryChange("")
                }
            }) {
                Icon(
                    imageVector = if (isSearchVisible) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = if (isSearchVisible) "Cerrar búsqueda" else "Buscar en biblioteca",
                    tint = if (isSearchVisible) MonkifyPrimary else MonkifyOnBackground
                )
            }
            IconButton(onClick = { showCreatePlaylistDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Crear Playlist",
                    tint = MonkifyOnBackground
                )
            }
        }

        AnimatedVisibility(
            visible = isSearchVisible,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            val searchQuery by viewModel.searchQuery.collectAsState()
            TextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                placeholder = { Text("Buscar en biblioteca...", color = MonkifyGray) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = MonkifyGray)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpiar", tint = MonkifyGray)
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
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Interactive Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LibraryFilterChip(
                text = "Todo",
                isSelected = selectedFilter == LibraryFilter.ALL,
                onClick = { viewModel.setFilter(LibraryFilter.ALL) }
            )
            LibraryFilterChip(
                text = "Favoritos (${favoriteTracks.size})",
                isSelected = selectedFilter == LibraryFilter.FAVORITES,
                onClick = { viewModel.setFilter(LibraryFilter.FAVORITES) }
            )
            LibraryFilterChip(
                text = "Descargas (${downloadedTracks.size})",
                isSelected = selectedFilter == LibraryFilter.DOWNLOADS,
                onClick = { viewModel.setFilter(LibraryFilter.DOWNLOADS) }
            )
            LibraryFilterChip(
                text = "Playlists (${playlists.size})",
                isSelected = selectedFilter == LibraryFilter.PLAYLISTS,
                onClick = { viewModel.setFilter(LibraryFilter.PLAYLISTS) }
            )
            LibraryFilterChip(
                text = "Álbumes (${albums.size})",
                isSelected = selectedFilter == LibraryFilter.ALBUMS,
                onClick = { viewModel.setFilter(LibraryFilter.ALBUMS) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sort Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Sort,
                    contentDescription = "Sort",
                    tint = MonkifyOnBackground,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (selectedFilter) {
                        LibraryFilter.ALL -> "Todo en almacenamiento local"
                        LibraryFilter.FAVORITES -> "Tus canciones favoritas"
                        LibraryFilter.DOWNLOADS -> "Música descargada offline"
                        LibraryFilter.PLAYLISTS -> "Playlists en la nube"
                        LibraryFilter.ALBUMS -> "Álbumes del catálogo"
                    },
                    color = MonkifyOnBackground,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
            Text(
                text = when (selectedFilter) {
                    LibraryFilter.PLAYLISTS -> "${playlists.size} listas"
                    LibraryFilter.ALBUMS -> "${albums.size} álbumes"
                    else -> "${displayedTracks.size} pistas"
                },
                color = MonkifyGray,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Barra informativa de almacenamiento ocupado bajo el chip Descargas
        if (selectedFilter == LibraryFilter.DOWNLOADS && downloadedTracks.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = MonkifyDarkGray.copy(alpha = 0.7f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DownloadDone,
                        contentDescription = null,
                        tint = MonkifyPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "${downloadedTracks.size} canciones · ${formatBytes(totalStorageBytes)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MonkifyOnBackground
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Dynamic Library Content

        if (selectedFilter == LibraryFilter.PLAYLISTS) {
            if (isLoadingPlaylists) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MonkifyPrimary)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        CreatePlaylistHeaderItem(onClick = { showCreatePlaylistDialog = true })
                    }
                    if (playlists.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Aún no tienes playlists creadas.\nPresiona \"Nueva Playlist\" para comenzar.",
                                    color = MonkifyGray,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        itemsIndexed(playlists, key = { _, pl -> pl.id }) { _, pl ->
                            LibraryPlaylistItem(
                                playlist = pl,
                                onClick = { onNavigateToPlaylist(pl.id, pl.name) }
                            )
                        }
                    }
                }
            }
        } else if (selectedFilter == LibraryFilter.ALBUMS) {
            if (isLoadingAlbums) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MonkifyPrimary)
                }
            } else if (albums.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No se encontraron álbumes disponibles.",
                        color = MonkifyGray,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(albums, key = { idx, alb -> "${alb.album}_$idx" }) { _, alb ->
                        LibraryAlbumItem(
                            album = alb,
                            onClick = {
                                onNavigateToAlbum(alb.album, alb.artist, alb.coverUrl)
                            }
                        )
                    }
                }
            }
        } else if (displayedTracks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                val emptyMessage = when (selectedFilter) {
                    LibraryFilter.FAVORITES -> "No tienes canciones marcadas como favoritas aún.\nToca el corazón en el reproductor para agregarlas."
                    LibraryFilter.DOWNLOADS -> "No tienes canciones descargadas.\nToca el botón de descarga para escuchar sin conexión."
                    LibraryFilter.ALL, LibraryFilter.PLAYLISTS, LibraryFilter.ALBUMS -> "Tu biblioteca local está vacía.\nGuarda pistas como favoritas o descárgalas para verlas aquí."
                }
                Text(
                    text = emptyMessage,
                    color = MonkifyGray,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(displayedTracks, key = { _, track -> track.id }) { index, track ->
                    val isTrackDownloaded = track.id in downloadedIds || track.streamUrl.startsWith("/")
                    LibraryTrackItem(
                        track = track,
                        isDownloaded = isTrackDownloaded,
                        onClick = {
                            playerViewModel.playQueue(displayedTracks, index)
                        },
                        onDeleteClick = if (isTrackDownloaded && selectedFilter == LibraryFilter.DOWNLOADS) {
                            { trackToDeleteDownload = track }
                        } else null
                    )
                }
            }
        }

        if (showCreatePlaylistDialog) {
            CreatePlaylistDialog(
                onDismiss = { showCreatePlaylistDialog = false },
                onConfirm = { name ->
                    showCreatePlaylistDialog = false
                    viewModel.createPlaylist(name)
                }
            )
        }

        if (trackToDeleteDownload != null) {
            val track = trackToDeleteDownload!!
            AlertDialog(
                onDismissRequest = { trackToDeleteDownload = null },
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
                        text = "¿Deseas eliminar \"${track.title}\" del almacenamiento local?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MonkifyGray
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val id = track.id
                            trackToDeleteDownload = null
                            viewModel.deleteDownload(id) { res ->
                                res.fold(
                                    onSuccess = {
                                        Toast.makeText(context, "Descarga eliminada", Toast.LENGTH_SHORT).show()
                                    },
                                    onFailure = {
                                        Toast.makeText(context, "Error al eliminar descarga", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                    ) {
                        Text("Eliminar", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { trackToDeleteDownload = null }) {
                        Text("Cancelar", color = MonkifyGray)
                    }
                }
            )
        }
    }
}


@Composable
fun LibraryFilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MonkifyPrimary else MonkifyDarkGray,
        onClick = onClick
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.Black else MonkifyOnBackground,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun LibraryTrackItem(
    track: Track,
    isDownloaded: Boolean,
    onClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MonkifyDarkGray)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TrackArtwork(
            coverUrl = track.coverUrl,
            contentDescription = track.title,
            shape = RoundedCornerShape(6.dp),
            iconSize = 22.dp,
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = MonkifyOnBackground,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isDownloaded) {
                    Icon(
                        imageVector = Icons.Default.DownloadDone,
                        contentDescription = "Descargada",
                        tint = MonkifyPrimary,
                        modifier = Modifier
                            .size(14.dp)
                            .padding(end = 4.dp)
                    )
                }
                Text(
                    text = if (track.artist.isNotBlank()) "${track.artist} • ${track.album}" else track.album,
                    color = MonkifyGray,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (onDeleteClick != null) {
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar descarga",
                    tint = MonkifyGray,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
        }

        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Reproducir",
            tint = MonkifyPrimary,
            modifier = Modifier.size(24.dp)
        )
    }
}


@Composable
fun LibraryPlaylistItem(
    playlist: PlaylistSummaryDto,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MonkifyDarkGray),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlaylistPlay,
                contentDescription = null,
                tint = MonkifyPrimary,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                color = MonkifyOnBackground,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${playlist.trackCount} canciones",
                color = MonkifyGray,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Ver playlist",
            tint = MonkifyPrimary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun CreatePlaylistHeaderItem(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(MonkifyDarkGray)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MonkifyPrimary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Crear Playlist",
                tint = MonkifyPrimary,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = "Nueva Playlist",
                color = MonkifyOnBackground,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Crea y organiza tus canciones favoritas",
                color = MonkifyGray,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun LibraryAlbumItem(
    album: AlbumSummaryDto,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MonkifyDarkGray)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TrackArtwork(
            coverUrl = album.coverUrl,
            contentDescription = album.album,
            shape = RoundedCornerShape(6.dp),
            iconSize = 24.dp,
            isAlbum = true,
            modifier = Modifier.size(52.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = album.album,
                color = MonkifyOnBackground,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${album.artist} • ${album.trackCount} canciones",
                color = MonkifyGray,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Ver álbum",
            tint = MonkifyPrimary,
            modifier = Modifier.size(22.dp)
        )
    }
}

