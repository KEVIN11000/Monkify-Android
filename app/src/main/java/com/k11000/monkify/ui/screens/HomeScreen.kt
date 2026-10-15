package com.k11000.monkify.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.k11000.monkify.domain.model.Track
import com.k11000.monkify.playback.PlayerViewModel
import com.k11000.monkify.ui.components.ZenBackground
import com.k11000.monkify.ui.screens.viewmodel.HomeViewModel
import com.k11000.monkify.ui.screens.viewmodel.SettingsViewModel
import com.k11000.monkify.ui.theme.MonkifyBackground
import com.k11000.monkify.ui.theme.MonkifyDarkGray
import com.k11000.monkify.ui.theme.MonkifyGray
import com.k11000.monkify.ui.theme.MonkifyOnBackground
import com.k11000.monkify.ui.theme.MonkifyPrimary

@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit = {},
    scrollToTopTrigger: Int = 0,
    viewModel: HomeViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val listState = rememberLazyListState()
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val recentTracks by viewModel.recentTracks.collectAsState()
    val madeForYouTracks by viewModel.madeForYouTracks.collectAsState()
    val feedTracks by viewModel.feedTracks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val hasMore by viewModel.hasMore.collectAsState()
    val downloadedTrackIds by viewModel.downloadedTrackIds.collectAsState()
    val downloadingTrackIds by viewModel.downloadingTrackIds.collectAsState()

    val settingsState by settingsViewModel.uiState.collectAsState()
    val isZenCleared by playerViewModel.isZenCleared.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()

    // Manejo de desplazamiento al tope y restauración de pantalla despejada al pulsar Inicio
    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger > 0) {
            playerViewModel.setZenCleared(false)
            listState.animateScrollToItem(0)
        }
    }

    val isEffectivelyCleared = isZenCleared && settingsState.zenModeEnabled
    val contentAlpha by animateFloatAsState(
        targetValue = if (isEffectivelyCleared) 0f else 1f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "ZenContentAlpha"
    )
    val contentScale by animateFloatAsState(
        targetValue = if (isEffectivelyCleared) 0.94f else 1f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "ZenContentScale"
    )

    // Trigger de paginación infinita al aproximarse al final del feed
    val shouldLoadMore by remember {
        derivedStateOf {
            val totalCount = listState.layoutInfo.totalItemsCount
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalCount > 0 && lastVisibleIndex >= totalCount - 4
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && hasMore && !isLoadingMore) {
            viewModel.loadMore()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MonkifyBackground)
    ) {
        // Capa 1: Fondo Zen animado (Aurora o Visualizador reactivo)
        ZenBackground(
            isPlaying = isPlaying,
            engine = settingsState.zenEngine,
            modifier = Modifier.fillMaxSize()
        )

        // Capa 2: Si está despejado, overlay táctil para restaurar
        if (isEffectivelyCleared) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { playerViewModel.setZenCleared(false) }
                    .padding(bottom = 120.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Toca la pantalla o la barra para volver",
                    color = MonkifyGray.copy(alpha = 0.65f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Capa 3: Contenido de tarjetas y secciones con animación fluida de desvanecimiento
        LazyColumn(
            state = listState,
            userScrollEnabled = !isEffectivelyCleared,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = contentAlpha
                    scaleX = contentScale
                    scaleY = contentScale
                }
                .padding(top = statusBarPadding + 16.dp),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
        // Cabecera
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Buenas noches",
                    color = MonkifyOnBackground,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )
                Row {
                    IconButton(onClick = { /* TODO: Notificaciones */ }) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = MonkifyOnBackground
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MonkifyOnBackground
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        // Chips de filtro
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(text = "Todo", isSelected = true)
                FilterChip(text = "Música", isSelected = false)
                FilterChip(text = "Podcasts", isSelected = false)
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        // Grilla 2x3 de Reproducción Rápida con datos reales
        item {
            val quickItems = if (recentTracks.isNotEmpty()) {
                recentTracks.take(6)
            } else {
                emptyList()
            }

            if (quickItems.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickItems.chunked(2).forEach { rowTracks ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowTracks.forEach { track ->
                                QuickGridItem(
                                    track = track,
                                    isDownloaded = track.id in downloadedTrackIds,
                                    onClick = { playerViewModel.playTrack(track) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowTracks.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            } else {
                // Fallback de carga inicial con botón de prueba interactivo
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickGridFallbackItem(
                        title = "Play Test Track",
                        onClick = { playerViewModel.playTestTrack() },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }

        // Carrusel: Escuchado recientemente
        if (recentTracks.isNotEmpty()) {
            item {
                Text(
                    text = "Escuchado recientemente",
                    color = MonkifyOnBackground,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(recentTracks) { track ->
                        AlbumCard(
                            title = if (track.album.isNotBlank()) track.album else track.title,
                            subtitle = track.artist,
                            coverUrl = track.coverUrl,
                            isDownloaded = track.id in downloadedTrackIds,
                            onClick = { playerViewModel.playTrack(track) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // Carrusel: Hecho para ti
        if (madeForYouTracks.isNotEmpty()) {
            item {
                Text(
                    text = "Hecho para ti",
                    color = MonkifyOnBackground,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(madeForYouTracks) { track ->
                        MadeForYouCard(
                            title = "Mix: ${track.artist}",
                            subtitle = track.title,
                            onClick = { playerViewModel.playTrack(track) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // Sección Principal Paginada: Todas las canciones (Feed Vertical)
        item {
            Text(
                text = "Todas las canciones",
                color = MonkifyOnBackground,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (feedTracks.isEmpty() && isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MonkifyPrimary, modifier = Modifier.size(32.dp))
                }
            }
        } else {
            items(feedTracks, key = { it.id }) { track ->
                TrackRowItem(
                    track = track,
                    isDownloaded = track.id in downloadedTrackIds,
                    isDownloading = track.id in downloadingTrackIds,
                    onDownloadClick = { viewModel.toggleDownload(track) },
                    onClick = { playerViewModel.playTrack(track) }
                )
            }
        }

        // Indicador de paginación adicional al pie del feed
        if (isLoadingMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MonkifyPrimary,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
    }
}

@Composable
fun FilterChip(text: String, isSelected: Boolean) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MonkifyPrimary else MonkifyDarkGray,
        onClick = { /* TODO */ }
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
fun QuickGridItem(
    track: Track,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDownloaded: Boolean = false
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MonkifyDarkGray)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            AsyncImage(
                model = track.coverUrl ?: "https://via.placeholder.com/150",
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(54.dp)
                    .background(Color.DarkGray)
            )
            if (isDownloaded) {
                Surface(
                    color = MonkifyDarkGray.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(bottomEnd = 4.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Icon(
                        imageVector = Icons.Default.DownloadDone,
                        contentDescription = "Descargada",
                        tint = MonkifyPrimary,
                        modifier = Modifier
                            .padding(2.dp)
                            .size(10.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            Text(
                text = track.title,
                color = MonkifyOnBackground,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                maxLines = if (isDownloaded) 1 else 2,
                overflow = TextOverflow.Ellipsis
            )
            if (isDownloaded) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DownloadDone,
                        contentDescription = null,
                        tint = MonkifyPrimary,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Descargada",
                        color = MonkifyPrimary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
fun QuickGridFallbackItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MonkifyDarkGray)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .background(Color.Gray)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = MonkifyOnBackground,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        )
    }
}

@Composable
fun AlbumCard(
    title: String,
    subtitle: String,
    coverUrl: String?,
    onClick: () -> Unit = {},
    isDownloaded: Boolean = false
) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick)
    ) {
        Box {
            AsyncImage(
                model = coverUrl ?: "https://via.placeholder.com/150",
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.DarkGray)
            )
            if (isDownloaded) {
                Surface(
                    color = MonkifyDarkGray.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(bottomStart = 8.dp),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.DownloadDone,
                        contentDescription = "Descargada",
                        tint = MonkifyPrimary,
                        modifier = Modifier
                            .padding(4.dp)
                            .size(14.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            color = MonkifyOnBackground,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            color = MonkifyGray,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun MadeForYouCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .height(70.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E293B))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            color = MonkifyPrimary,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            color = MonkifyGray,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun TrackRowItem(
    track: Track,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDownloaded: Boolean = false,
    isDownloading: Boolean = false,
    onDownloadClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = track.coverUrl ?: "https://via.placeholder.com/150",
            contentDescription = track.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MonkifyDarkGray)
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
            Text(
                text = if (track.album.isNotBlank()) "${track.artist} • ${track.album}" else track.artist,
                color = MonkifyGray,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))

        if (onDownloadClick != null) {
            IconButton(
                onClick = onDownloadClick,
                modifier = Modifier.size(36.dp)
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MonkifyPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = if (isDownloaded) Icons.Default.DownloadDone else Icons.Default.Download,
                        contentDescription = if (isDownloaded) "Descargada" else "Descargar pista",
                        tint = if (isDownloaded) MonkifyPrimary else MonkifyGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(4.dp))
        }

        Text(
            text = formatDuration(track.durationMs),
            color = MonkifyGray,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

private fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0L) return "--:--"
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
