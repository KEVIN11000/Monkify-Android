package com.k11000.monkify.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.text.style.TextAlign
import com.k11000.monkify.ui.screens.viewmodel.HomeFilter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.k11000.monkify.domain.model.Track
import com.k11000.monkify.playback.PlayerViewModel
import com.k11000.monkify.ui.components.TrackArtwork
import com.k11000.monkify.ui.components.ZenBackground
import com.k11000.monkify.ui.screens.viewmodel.HomeViewModel
import com.k11000.monkify.ui.screens.viewmodel.SettingsViewModel
import com.k11000.monkify.ui.theme.MonkifyBackground
import com.k11000.monkify.ui.theme.MonkifyDarkGray
import com.k11000.monkify.ui.theme.MonkifyGray
import com.k11000.monkify.ui.theme.MonkifyOnBackground
import com.k11000.monkify.ui.theme.MonkifyPrimary
import kotlinx.coroutines.delay
import kotlin.math.abs

@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAlbum: (String, String, String?) -> Unit = { _, _, _ -> },
    scrollToTopTrigger: Int = 0,
    viewModel: HomeViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val listState = rememberLazyListState()
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val recentTracks by viewModel.recentTracks.collectAsState()
    val madeForYouTracks by viewModel.madeForYouTracks.collectAsState()
    val latestUploadedTracks by viewModel.latestUploadedTracks.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val isLoadingAlbums by viewModel.isLoadingAlbums.collectAsState()
    val greeting by viewModel.greeting.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val downloadedTrackIds by viewModel.downloadedTrackIds.collectAsState()
    val downloadingTrackIds by viewModel.downloadingTrackIds.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    var showNovedadesModal by remember { mutableStateOf(false) }

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

        // Capa 2: Contenido de tarjetas y secciones con animación fluida de desvanecimiento
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
                .pointerInput(settingsState.zenModeEnabled, isEffectivelyCleared) {
                    if (settingsState.zenModeEnabled && !isEffectivelyCleared) {
                        detectTapGestures {
                            playerViewModel.setZenCleared(true)
                        }
                    }
                }
                .padding(top = statusBarPadding + 16.dp),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
        // Cabecera Dinámica con Saludo y Frase Motivadora
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (settingsState.zenModeEnabled) playerViewModel.setZenCleared(true)
                        }
                ) {
                    Text(
                        text = greeting.title,
                        color = MonkifyOnBackground,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = greeting.phrase,
                        color = MonkifyGray,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row {
                    IconButton(onClick = { showNovedadesModal = true }) {
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

        item { ZenSpacer(16.dp, settingsState.zenModeEnabled) { playerViewModel.setZenCleared(true) } }

        // Chips de filtro
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HomeFilter.values().forEach { filter ->
                    FilterChip(
                        text = filter.displayName,
                        isSelected = selectedFilter == filter,
                        onClick = { viewModel.setFilter(filter) }
                    )
                }
            }
        }

        if (selectedFilter == HomeFilter.PODCASTS) {
            item {
                ZenSpacer(24.dp, settingsState.zenModeEnabled) { playerViewModel.setZenCleared(true) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Podcasts,
                            contentDescription = null,
                            tint = MonkifyGray,
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = "Aún no tienes podcasts en tu biblioteca",
                            color = MonkifyOnBackground,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Puedes importar episodios pegando enlaces desde el buscador o explorar tu música en 'Todo'.",
                            color = MonkifyGray,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else if (selectedFilter == HomeFilter.ALBUMS) {
            item { ZenSpacer(24.dp, settingsState.zenModeEnabled) { playerViewModel.setZenCleared(true) } }
            if (isLoadingAlbums && albums.isEmpty()) {
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
            } else if (albums.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No se encontraron álbumes disponibles.",
                            color = MonkifyGray,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        albums.chunked(2).forEach { rowAlbums ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                rowAlbums.forEach { album ->
                                    AlbumCard(
                                        title = album.album,
                                        subtitle = "${album.artist} • ${album.trackCount} temas",
                                        coverUrl = album.coverUrl,
                                        onClick = { onNavigateToAlbum(album.album, album.artist, album.coverUrl) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (rowAlbums.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        } else {
            item { ZenSpacer(24.dp, settingsState.zenModeEnabled) { playerViewModel.setZenCleared(true) } }

            // Grilla 2x3 de Reproducción Rápida con los últimos 6 temas subidos al servidor
            item {
                val quickItems = if (latestUploadedTracks.isNotEmpty()) {
                    latestUploadedTracks.take(6)
                } else if (recentTracks.isNotEmpty()) {
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

            item { ZenSpacer(32.dp, settingsState.zenModeEnabled) { playerViewModel.setZenCleared(true) } }

            // Carrusel: Escuchado recientemente
            if (recentTracks.isNotEmpty()) {
                item {
                    Text(
                        text = "Escuchado recientemente",
                        color = MonkifyOnBackground,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (settingsState.zenModeEnabled) playerViewModel.setZenCleared(true)
                            }
                    )
                    ZenSpacer(16.dp, settingsState.zenModeEnabled) { playerViewModel.setZenCleared(true) }
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
                                onClick = {
                                    if (track.album.isNotBlank()) {
                                        onNavigateToAlbum(track.album, track.artist, track.coverUrl)
                                    } else {
                                        playerViewModel.playTrack(track)
                                    }
                                }
                            )
                        }
                    }
                    ZenSpacer(32.dp, settingsState.zenModeEnabled) { playerViewModel.setZenCleared(true) }
                }
            }

            // Carrusel: Hecho para ti
            if (madeForYouTracks.isNotEmpty()) {
                item {
                    Text(
                        text = "Hecho para ti",
                        color = MonkifyOnBackground,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (settingsState.zenModeEnabled) playerViewModel.setZenCleared(true)
                            }
                    )
                    ZenSpacer(16.dp, settingsState.zenModeEnabled) { playerViewModel.setZenCleared(true) }
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
                    ZenSpacer(32.dp, settingsState.zenModeEnabled) { playerViewModel.setZenCleared(true) }
                }
            }
        }
        }

        // Capa 3: Overlay gestual exclusivo para Modo Zen despejado (al frente de la jerarquía)
        if (isEffectivelyCleared) {
            ZenGestureOverlay(
                playerViewModel = playerViewModel,
                statusBarPadding = statusBarPadding
            )
        }

        if (showNovedadesModal) {
            NovedadesBottomSheet(onDismiss = { showNovedadesModal = false })
        }
    }
}

@Composable
fun FilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit = {}
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovedadesBottomSheet(
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MonkifyDarkGray
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(MonkifyPrimary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MonkifyPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = "Novedades de Monkify",
                        color = MonkifyOnBackground,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Versión activa v0.4.0 • Release",
                        color = MonkifyGray,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MonkifyBackground)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NovedadItem(
                    emoji = "🖼️",
                    title = "Carátulas Oficiales en Todo el Catálogo",
                    description = "Más de 8.600 canciones enriquecidas con sus portadas originales de Spotify y arte vectorial nativo."
                )
                NovedadItem(
                    emoji = "💿",
                    title = "Exploración y Descarga de Álbumes",
                    description = "Nuevo filtro 'Álbumes' en Tu Biblioteca y vista de detalle para reproducir o descargar álbumes completos offline."
                )
                NovedadItem(
                    emoji = "🌐",
                    title = "Títulos Traducidos y Artistas Normalizados",
                    description = "Canciones en japonés, coreano, chino y cirílico muestran su nombre original junto con su traducción al español entre paréntesis."
                )
                NovedadItem(
                    emoji = "🎛️",
                    title = "Reordenamiento en Cola y Monkify Connect",
                    description = "Mueve pistas en la cola sobre la marcha y selecciona salidas de audio dinámicas."
                )
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MonkifyPrimary)
            ) {
                Text(
                    text = "Entendido",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun NovedadItem(
    emoji: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = emoji, style = MaterialTheme.typography.titleMedium)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MonkifyOnBackground,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = description,
                color = MonkifyGray,
                style = MaterialTheme.typography.bodySmall
            )
        }
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
            TrackArtwork(
                coverUrl = track.coverUrl,
                contentDescription = track.title,
                shape = RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp),
                iconSize = 24.dp,
                modifier = Modifier.size(54.dp)
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
    isDownloaded: Boolean = false,
    modifier: Modifier = Modifier.width(140.dp)
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
    ) {
        Box {
            TrackArtwork(
                coverUrl = coverUrl,
                contentDescription = title,
                shape = RoundedCornerShape(8.dp),
                iconSize = 48.dp,
                isAlbum = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
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

@Composable
private fun ZenSpacer(
    height: Dp,
    zenEnabled: Boolean,
    onClick: () -> Unit
) {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (zenEnabled) onClick()
            }
    )
}

@Composable
private fun ZenGestureOverlay(
    playerViewModel: PlayerViewModel,
    statusBarPadding: Dp,
    modifier: Modifier = Modifier
) {
    val currentTitle by playerViewModel.currentTrackTitle.collectAsState()
    val currentArtist by playerViewModel.currentTrackArtist.collectAsState()

    var feedbackIcon by remember { mutableStateOf<ImageVector?>(null) }
    var feedbackText by remember { mutableStateOf("") }
    var feedbackTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(feedbackTrigger) {
        if (feedbackTrigger > 0) {
            delay(750)
            feedbackIcon = null
            feedbackText = ""
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Área receptora de gestos que cubre toda la pantalla
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    var lastTapTime = 0L
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = true)
                        val startTime = System.currentTimeMillis()
                        var totalDragX = 0f
                        var isDrag = false
                        val slop = viewConfiguration.touchSlop

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                val now = System.currentTimeMillis()
                                val duration = now - startTime
                                if (!isDrag && duration < 500) {
                                    if (now - lastTapTime < 350) {
                                        // Doble toque: Restaurar tarjetas
                                        playerViewModel.setZenCleared(false)
                                        lastTapTime = 0L
                                    } else {
                                        lastTapTime = now
                                        // Toque simple: Pausar o reanudar
                                        val willPlay = !playerViewModel.isPlaying.value
                                        playerViewModel.togglePlayPause()
                                        feedbackIcon = if (willPlay) Icons.Default.PlayArrow else Icons.Default.Pause
                                        feedbackText = if (willPlay) "Reanudar" else "Pausado"
                                        feedbackTrigger++
                                    }
                                } else if (isDrag) {
                                    val swipeThreshold = 90f
                                    if (totalDragX < -swipeThreshold) {
                                        // Deslizar a la izquierda: Siguiente canción
                                        playerViewModel.skipToNext()
                                        feedbackIcon = Icons.Default.SkipNext
                                        feedbackText = "Siguiente"
                                        feedbackTrigger++
                                    } else if (totalDragX > swipeThreshold) {
                                        // Deslizar a la derecha: Canción anterior
                                        playerViewModel.skipToPrevious()
                                        feedbackIcon = Icons.Default.SkipPrevious
                                        feedbackText = "Anterior"
                                        feedbackTrigger++
                                    }
                                }
                                break
                            }
                            val dx = change.position.x - down.position.x
                            val dy = change.position.y - down.position.y
                            totalDragX = dx
                            if (abs(dx) > slop || abs(dy) > slop) {
                                isDrag = true
                                change.consume()
                            }
                        }
                    }
                }
        )

        // Pill superior para volver a la vista normal
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.Black.copy(alpha = 0.55f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = statusBarPadding + 16.dp)
                .clickable { playerViewModel.setZenCleared(false) }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Volver",
                    tint = MonkifyOnBackground.copy(alpha = 0.85f),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Modo Zen • Toca para volver",
                    style = MaterialTheme.typography.labelSmall,
                    color = MonkifyOnBackground.copy(alpha = 0.85f)
                )
            }
        }

        // Indicador visual animado de acción (Play, Pausa, Siguiente, Anterior)
        AnimatedVisibility(
            visible = feedbackIcon != null,
            enter = fadeIn() + scaleIn(initialScale = 0.7f),
            exit = fadeOut() + scaleOut(targetScale = 0.9f),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.72f))
                    .padding(horizontal = 28.dp, vertical = 20.dp)
            ) {
                feedbackIcon?.let { icon ->
                    Icon(
                        imageVector = icon,
                        contentDescription = feedbackText,
                        tint = MonkifyPrimary,
                        modifier = Modifier.size(54.dp)
                    )
                }
                if (feedbackText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = feedbackText,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MonkifyOnBackground
                    )
                }
            }
        }

        // Información inferior de la pista y guía de gestos
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 110.dp, start = 24.dp, end = 24.dp)
        ) {
            if (currentTitle.isNotBlank()) {
                Text(
                    text = currentTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MonkifyOnBackground.copy(alpha = 0.95f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = currentArtist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MonkifyPrimary.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
            Text(
                text = "Toca para pausar/reanudar • Desliza para cambiar • Doble toque para volver",
                style = MaterialTheme.typography.labelSmall,
                color = MonkifyGray.copy(alpha = 0.75f)
            )
        }
    }
}


