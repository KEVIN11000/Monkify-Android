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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.k11000.monkify.data.remote.dto.PlaylistSummaryDto

@Composable
fun LibraryScreen(
    onNavigateToPlaylist: (String, String) -> Unit = { _, _ -> },
    viewModel: LibraryViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val displayedTracks by viewModel.displayedTracks.collectAsState()
    val favoriteTracks by viewModel.favoriteTracks.collectAsState()
    val downloadedTracks by viewModel.downloadedTracks.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val isLoadingPlaylists by viewModel.isLoadingPlaylists.collectAsState()
    val downloadedIds = remember(downloadedTracks) { downloadedTracks.map { it.id }.toSet() }

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

            IconButton(onClick = { /* TODO */ }) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search in Library",
                    tint = MonkifyOnBackground
                )
            }
            IconButton(onClick = { /* TODO */ }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add",
                    tint = MonkifyOnBackground
                )
            }
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
                    },
                    color = MonkifyOnBackground,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
            Text(
                text = if (selectedFilter == LibraryFilter.PLAYLISTS) "${playlists.size} listas" else "${displayedTracks.size} pistas",
                color = MonkifyGray,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

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
            } else if (playlists.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No se encontraron playlists disponibles en la nube.",
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
                    itemsIndexed(playlists, key = { _, pl -> pl.id }) { _, pl ->
                        LibraryPlaylistItem(
                            playlist = pl,
                            onClick = { onNavigateToPlaylist(pl.id, pl.name) }
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
                    LibraryFilter.ALL, LibraryFilter.PLAYLISTS -> "Tu biblioteca local está vacía.\nGuarda pistas como favoritas o descárgalas para verlas aquí."
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
                    LibraryTrackItem(
                        track = track,
                        isDownloaded = track.id in downloadedIds || track.streamUrl.startsWith("/"),
                        onClick = {
                            playerViewModel.playQueue(displayedTracks, index)
                        }
                    )
                }
            }
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
        AsyncImage(
            model = track.coverUrl ?: "https://via.placeholder.com/150",
            contentDescription = track.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.DarkGray)
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
