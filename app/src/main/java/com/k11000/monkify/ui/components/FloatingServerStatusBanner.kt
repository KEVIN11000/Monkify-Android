package com.k11000.monkify.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.k11000.monkify.data.remote.api.GlobalServerStatus
import com.k11000.monkify.data.remote.api.ServerStatusMonitor
import com.k11000.monkify.ui.theme.MonkifyBackground
import com.k11000.monkify.ui.theme.MonkifyOnBackground
import com.k11000.monkify.ui.theme.MonkifyPrimary
import com.k11000.monkify.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay

/**
 * Aviso flotante global minimalista y no intrusivo que informa al usuario únicamente cuando:
 * 1. Hay un error de autorización HTTP 401 (con acceso directo a Ajustes).
 * 2. Hay una caída de red/timeout (con opción de Reintentar o descartar con '✕').
 * 3. Se ejecuta una reconexión explícita o se recupera la conexión tras un fallo (auto-oculta en 3s).
 */
@Composable
fun FloatingServerStatusBanner(
    modifier: Modifier = Modifier,
    monitor: ServerStatusMonitor = ServerStatusMonitor.shared,
    onNavigateToSettings: () -> Unit = {},
    onRetryConnection: () -> Unit = {}
) {
    val status by monitor.status.collectAsState()

    // Auto-ocultar la confirmación de "Conectado" tras 3 segundos para mantener la interfaz limpia
    LaunchedEffect(status) {
        if (status is GlobalServerStatus.Connected) {
            delay(3000L)
            monitor.dismiss(byUser = false)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = status !is GlobalServerStatus.Idle,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
        ) {
            ServerStatusBannerCard(
                status = status,
                onNavigateToSettings = onNavigateToSettings,
                onRetryConnection = onRetryConnection,
                onDismiss = { monitor.dismiss(byUser = true) }
            )
        }
    }
}

@Composable
fun ServerStatusBannerCard(
    status: GlobalServerStatus,
    onNavigateToSettings: () -> Unit = {},
    onRetryConnection: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    if (status is GlobalServerStatus.Idle) return

    val containerColor: Color
    val borderColor: Color
    val accentColor: Color

    when (status) {
        is GlobalServerStatus.Connecting -> {
            containerColor = Color(0xEB181818)
            borderColor = MonkifyPrimary.copy(alpha = 0.45f)
            accentColor = MonkifyPrimary
        }
        is GlobalServerStatus.Connected -> {
            containerColor = Color(0xEB11261B)
            borderColor = MonkifyPrimary.copy(alpha = 0.7f)
            accentColor = MonkifyPrimary
        }
        is GlobalServerStatus.Unauthorized -> {
            containerColor = Color(0xF02B1618)
            borderColor = Color(0xFFFF6B6B).copy(alpha = 0.75f)
            accentColor = Color(0xFFFF8787)
        }
        is GlobalServerStatus.Error -> {
            containerColor = Color(0xF0261E12)
            borderColor = Color(0xFFFFB74D).copy(alpha = 0.65f)
            accentColor = Color(0xFFFFCA7A)
        }
        GlobalServerStatus.Idle -> return
    }

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(24.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (status) {
                    is GlobalServerStatus.Connecting -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = accentColor,
                            strokeWidth = 2.dp
                        )
                    }
                    is GlobalServerStatus.Connected -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Conectado",
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    is GlobalServerStatus.Unauthorized -> {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "No autorizado",
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    is GlobalServerStatus.Error -> {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = "Sin conexión",
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    GlobalServerStatus.Idle -> {}
                }

                Spacer(modifier = Modifier.width(10.dp))

                val messageText = when (status) {
                    is GlobalServerStatus.Connecting -> status.message
                    is GlobalServerStatus.Connected -> status.message
                    is GlobalServerStatus.Unauthorized -> status.reason
                    is GlobalServerStatus.Error -> status.reason
                    GlobalServerStatus.Idle -> ""
                }

                Text(
                    text = messageText,
                    color = MonkifyOnBackground,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (status is GlobalServerStatus.Unauthorized) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.2f))
                            .clickable(onClick = onNavigateToSettings)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "Ajustes",
                            color = accentColor,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                } else if (status is GlobalServerStatus.Error) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.2f))
                            .clickable(onClick = onRetryConnection)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "Reintentar",
                            color = accentColor,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                if (status is GlobalServerStatus.Unauthorized || status is GlobalServerStatus.Error) {
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar aviso",
                            tint = MonkifyOnBackground.copy(alpha = 0.7f),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun FloatingServerStatusBannerPreview() {
    MyApplicationTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MonkifyBackground)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ServerStatusBannerCard(
                status = GlobalServerStatus.Connecting("Verificando conexión con el servidor...")
            )
            ServerStatusBannerCard(
                status = GlobalServerStatus.Unauthorized("Error HTTP 401: Clave de servidor inválida o no configurada")
            )
            ServerStatusBannerCard(
                status = GlobalServerStatus.Error("Sin conexión al servidor · Modo offline activo")
            )
            ServerStatusBannerCard(
                status = GlobalServerStatus.Connected("Conectado al servidor Monkify")
            )
        }
    }
}
