package com.k11000.monkify.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.k11000.monkify.ui.screens.viewmodel.SettingsViewModel
import com.k11000.monkify.ui.theme.MonkifyBackground
import com.k11000.monkify.ui.theme.MonkifyDarkGray
import com.k11000.monkify.ui.theme.MonkifyGray
import com.k11000.monkify.ui.theme.MonkifyOnBackground
import com.k11000.monkify.ui.theme.MonkifyPrimary
import com.k11000.monkify.ui.theme.MonkifySurface

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MonkifyBackground)
                .verticalScroll(scrollState)
                .padding(top = statusBarPadding + 8.dp, bottom = 24.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = MonkifyOnBackground
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Ajustes y Preferencias",
                    color = MonkifyOnBackground,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sección 1: Almacenamiento y Caché
            SettingsSectionHeader(title = "ALMACENAMIENTO Y CACHÉ", icon = Icons.Default.Storage)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MonkifySurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Caché de Reproducción Automática",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MonkifyOnBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val usedMb = uiState.cacheUsedBytes / (1024 * 1024)
                    val limitMb = uiState.cacheLimitMb
                    val progress = if (limitMb > 0) (usedMb.toFloat() / limitMb.toFloat()).coerceIn(0f, 1f) else 0f

                    Text(
                        text = "Ocupado: $usedMb MB / ${limitMb} MB",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MonkifyGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MonkifyPrimary,
                        trackColor = MonkifyDarkGray,
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Límite máximo permitido:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MonkifyOnBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Selector de opciones de límite
                    val cacheOptions = listOf(500L to "500 MB", 1024L to "1 GB", 2048L to "2 GB", 5120L to "5 GB")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        cacheOptions.forEach { (mb, label) ->
                            val isSelected = uiState.cacheLimitMb == mb
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setCacheLimit(mb) },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MonkifyPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = MonkifyDarkGray,
                                    labelColor = MonkifyGray
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.clearCache() },
                        enabled = !uiState.isClearingCache,
                        colors = ButtonDefaults.buttonColors(containerColor = MonkifyDarkGray),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = null,
                            tint = MonkifyPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.isClearingCache) "Liberando..." else "Vaciar Caché de Audio",
                            color = MonkifyOnBackground
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Sección 2: Apariencia
            SettingsSectionHeader(title = "APARIENCIA Y COLOR", icon = Icons.Default.Palette)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MonkifySurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tema Dinámico (Material You)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MonkifyOnBackground
                        )
                        Text(
                            text = "Adaptar colores al fondo de pantalla en Android 12+",
                            style = MaterialTheme.typography.bodySmall,
                            color = MonkifyGray
                        )
                    }
                    Switch(
                        checked = uiState.dynamicColor,
                        onCheckedChange = { viewModel.setDynamicColor(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MonkifyPrimary,
                            uncheckedThumbColor = MonkifyGray,
                            uncheckedTrackColor = MonkifyDarkGray
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Sección 3: Modo Zen y Fondo Dinámico
            SettingsSectionHeader(title = "MODO ZEN Y FONDO AMBIENTAL", icon = Icons.Default.AutoAwesome)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MonkifySurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Despejar pantalla al pulsar barra",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MonkifyOnBackground
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Al presionar la barra flotante en Inicio, las tarjetas se desvanecen para mostrar la animación de fondo",
                                style = MaterialTheme.typography.bodySmall,
                                color = MonkifyGray
                            )
                        }
                        Switch(
                            checked = uiState.zenModeEnabled,
                            onCheckedChange = { viewModel.setZenModeEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MonkifyPrimary,
                                uncheckedThumbColor = MonkifyGray,
                                uncheckedTrackColor = MonkifyDarkGray
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Motor Visual de Fondo:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MonkifyOnBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = uiState.zenEngine == "AURORA",
                            onClick = { viewModel.setZenEngine("AURORA") },
                            label = { Text("Aurora Boreal") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MonkifyPrimary,
                                selectedLabelColor = Color.Black,
                                containerColor = MonkifyDarkGray,
                                labelColor = MonkifyOnBackground
                            )
                        )
                        FilterChip(
                            selected = uiState.zenEngine == "VISUALIZER",
                            onClick = { viewModel.setZenEngine("VISUALIZER") },
                            label = { Text("Visualizador de Audio") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MonkifyPrimary,
                                selectedLabelColor = Color.Black,
                                containerColor = MonkifyDarkGray,
                                labelColor = MonkifyOnBackground
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Sección 4: Información
            SettingsSectionHeader(title = "INFORMACIÓN DEL SISTEMA", icon = Icons.Default.Info)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MonkifySurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Monkify Android Client",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MonkifyOnBackground
                    )
                    Text(
                        text = "Versión: 1.0.0 (Cloudflare Serverless Edition)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MonkifyGray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Servidor: monkify-backend.monkify.workers.dev",
                        style = MaterialTheme.typography.bodySmall,
                        color = MonkifyGray
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MonkifyPrimary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MonkifyGray
        )
    }
}
