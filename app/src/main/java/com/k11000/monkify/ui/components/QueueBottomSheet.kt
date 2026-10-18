package com.k11000.monkify.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.k11000.monkify.domain.model.Track
import com.k11000.monkify.ui.theme.MonkifyDarkGray
import com.k11000.monkify.ui.theme.MonkifyGray
import com.k11000.monkify.ui.theme.MonkifyOnBackground
import com.k11000.monkify.ui.theme.MonkifyPrimary
import com.k11000.monkify.ui.theme.MonkifySurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueBottomSheet(
    queue: List<Track>,
    currentTrackId: String?,
    onDismiss: () -> Unit,
    onTrackClick: (Int) -> Unit,
    onSaveQueueAsPlaylist: (String, (Result<Unit>) -> Unit) -> Unit,
    onMoveTrack: ((Int, Int) -> Unit)? = null,
    onRemoveTrack: ((Int) -> Unit)? = null,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val context = LocalContext.current
    var showCreateDialog by remember { mutableStateOf(false) }

    fun formatDuration(ms: Long): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return String.format("%d:%02d", min, sec)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MonkifyDarkGray,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Cabecera de la cola
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Cola de Reproducción",
                        color = MonkifyOnBackground,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${queue.size} canciones en la lista",
                        color = MonkifyGray,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar cola",
                        tint = MonkifyGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Botón de acción: Guardar como Playlist
            Button(
                onClick = { showCreateDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MonkifyPrimary,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Guardar como playlist",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lista secuencial de pistas
            if (queue.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay canciones en la cola",
                        color = MonkifyGray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(queue, key = { index, track -> "${track.id}_$index" }) { index, track ->
                        val isCurrent = track.id == currentTrackId

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isCurrent) MonkifySurface else Color.Transparent)
                                .clickable { onTrackClick(index) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Indicador de reproducción o índice
                            Box(
                                modifier = Modifier.width(30.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isCurrent) {
                                    Icon(
                                        imageVector = Icons.Default.Equalizer,
                                        contentDescription = "En reproducción",
                                        tint = MonkifyPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Text(
                                        text = "${index + 1}",
                                        color = MonkifyGray,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Miniatura de carátula
                            TrackArtwork(
                                coverUrl = track.coverUrl,
                                contentDescription = null,
                                shape = RoundedCornerShape(6.dp),
                                iconSize = 20.dp,
                                modifier = Modifier.size(42.dp)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            // Título y artista
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    color = if (isCurrent) MonkifyPrimary else MonkifyOnBackground,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = track.artist,
                                    color = MonkifyGray,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Duración
                            Text(
                                text = formatDuration(track.durationMs),
                                color = MonkifyGray,
                                style = MaterialTheme.typography.bodySmall
                            )

                            if (onMoveTrack != null || onRemoveTrack != null) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (onMoveTrack != null) {
                                        IconButton(
                                            onClick = { onMoveTrack(index, index - 1) },
                                            enabled = index > 0,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowUp,
                                                contentDescription = "Subir pista",
                                                tint = if (index > 0) MonkifyGray else MonkifyGray.copy(alpha = 0.3f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { onMoveTrack(index, index + 1) },
                                            enabled = index < queue.size - 1,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowDown,
                                                contentDescription = "Bajar pista",
                                                tint = if (index < queue.size - 1) MonkifyGray else MonkifyGray.copy(alpha = 0.3f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    if (onRemoveTrack != null) {
                                        IconButton(
                                            onClick = { onRemoveTrack(index) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Eliminar pista",
                                                tint = Color(0xFFEF5350),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                showCreateDialog = false
                onSaveQueueAsPlaylist(name) { result ->
                    result.fold(
                        onSuccess = {
                            Toast.makeText(context, "Playlist '$name' guardada con éxito", Toast.LENGTH_SHORT).show()
                        },
                        onFailure = { err ->
                            Toast.makeText(context, "Error al guardar playlist: ${err.message}", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            }
        )
    }
}
