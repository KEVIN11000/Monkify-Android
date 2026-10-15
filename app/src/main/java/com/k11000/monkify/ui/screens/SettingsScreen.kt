package com.k11000.monkify.ui.screens

import android.content.Intent
import android.net.Uri
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
import android.widget.Toast
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import com.k11000.monkify.BuildConfig
import com.k11000.monkify.ui.components.UpdateDialog
import com.k11000.monkify.util.AppLogger
import com.k11000.monkify.util.GitHubIssueUrlBuilder
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showLogsDialog by remember { mutableStateOf(false) }
    var feedbackComment by remember { mutableStateOf("") }
    var attachLogs by remember { mutableStateOf(true) }
    var reportType by remember { mutableStateOf(GitHubIssueUrlBuilder.IssueType.BUG) }

    uiState.availableUpdate?.let { update ->
        UpdateDialog(
            update = update,
            onConfirmUpdate = { appUpdate ->
                viewModel.dismissUpdateDialog()
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(appUpdate.downloadUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            },
            onDismiss = {
                viewModel.dismissUpdateDialog()
            }
        )
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

                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(MonkifyDarkGray)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Reproductor en Pantalla de Bloqueo",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MonkifyOnBackground
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Proyecta el Modo Zen interactivo sobre el bloqueo. Protege contra burn-in OLED y se apaga en el bolsillo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MonkifyGray
                            )
                        }
                        Switch(
                            checked = uiState.lockScreenEnabled,
                            onCheckedChange = { viewModel.setLockScreenEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MonkifyPrimary,
                                uncheckedThumbColor = MonkifyGray,
                                uncheckedTrackColor = MonkifyDarkGray
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
                        text = "Versión: ${BuildConfig.VERSION_NAME} (Cloudflare Serverless Edition)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MonkifyGray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Servidor: monkify-backend.monkify.workers.dev",
                        style = MaterialTheme.typography.bodySmall,
                        color = MonkifyGray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.checkForUpdates(manual = true) },
                        enabled = !uiState.isCheckingUpdate,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MonkifyPrimary,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState.isCheckingUpdate) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Comprobando...", style = MaterialTheme.typography.bodySmall)
                        } else {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Buscar actualizaciones",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sección 5: Diagnóstico y Logs del Sistema
            SettingsSectionHeader(title = "DIAGNÓSTICO Y REGISTROS", icon = Icons.Default.BugReport)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MonkifyDarkGray)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Visor de Logs y Eventos",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MonkifyOnBackground
                    )
                    Text(
                        text = "Registra eventos de streaming, errores de audio, descargas y estado en vivo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MonkifyGray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showLogsDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MonkifyPrimary,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Ver Logs", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        }
                        OutlinedButton(
                            onClick = {
                                val logsText = AppLogger.getLogsAsText()
                                if (logsText.isNotBlank()) {
                                    clipboardManager.setText(AnnotatedString(logsText))
                                    Toast.makeText(context, "Logs copiados al portapapeles 📋", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "No hay logs registrados aún", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Copiar", color = MonkifyOnBackground, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(MonkifyGray.copy(alpha = 0.3f))
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Centro de Soporte y Tickets",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MonkifyOnBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Reporta fallos o envía sugerencias. Se creará un ticket directamente en GitHub con los datos de tu dispositivo para resolverlo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MonkifyGray
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = reportType == GitHubIssueUrlBuilder.IssueType.BUG,
                            onClick = { reportType = GitHubIssueUrlBuilder.IssueType.BUG },
                            label = { Text("🐞 Error / Bug", style = MaterialTheme.typography.bodySmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MonkifyPrimary,
                                selectedLabelColor = Color.Black,
                                containerColor = MonkifyDarkGray,
                                labelColor = MonkifyOnBackground
                            )
                        )
                        FilterChip(
                            selected = reportType == GitHubIssueUrlBuilder.IssueType.FEATURE,
                            onClick = { reportType = GitHubIssueUrlBuilder.IssueType.FEATURE },
                            label = { Text("💡 Sugerencia", style = MaterialTheme.typography.bodySmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MonkifyPrimary,
                                selectedLabelColor = Color.Black,
                                containerColor = MonkifyDarkGray,
                                labelColor = MonkifyOnBackground
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = feedbackComment,
                        onValueChange = { feedbackComment = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(115.dp),
                        placeholder = {
                            Text(
                                if (reportType == GitHubIssueUrlBuilder.IssueType.BUG)
                                    "Describe el fallo (ej. error 404 en canción, problema al pausar...)"
                                else
                                    "Describe tu sugerencia o idea de mejora...",
                                color = MonkifyGray,
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = MonkifyOnBackground),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MonkifyPrimary,
                            unfocusedBorderColor = MonkifyGray.copy(alpha = 0.5f),
                            cursorColor = MonkifyPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Adjuntar últimos registros de diagnóstico",
                            style = MaterialTheme.typography.bodySmall,
                            color = MonkifyOnBackground
                        )
                        Switch(
                            checked = attachLogs,
                            onCheckedChange = { attachLogs = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MonkifyPrimary,
                                uncheckedThumbColor = MonkifyGray,
                                uncheckedTrackColor = MonkifyDarkGray
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val comment = feedbackComment.trim()
                                if (comment.isBlank()) {
                                    Toast.makeText(context, "Por favor escribe una breve descripción antes de continuar", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                val logsText = if (attachLogs) AppLogger.getLogsAsText().takeLast(3000) else null

                                val params = GitHubIssueUrlBuilder.IssueParams(
                                    type = reportType,
                                    comment = comment,
                                    appVersion = BuildConfig.VERSION_NAME,
                                    versionCode = BuildConfig.VERSION_CODE,
                                    deviceModel = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
                                    androidVersion = "Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})",
                                    logs = logsText
                                )

                                val issueUrl = GitHubIssueUrlBuilder.buildUrl(params)
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(issueUrl)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    clipboardManager.setText(AnnotatedString(issueUrl))
                                    Toast.makeText(context, "No se pudo abrir el navegador. Enlace copiado al portapapeles 📋", Toast.LENGTH_LONG).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MonkifyPrimary,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInBrowser,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Abrir en GitHub", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        }

                        OutlinedButton(
                            onClick = {
                                val comment = feedbackComment.trim()
                                val reportBuilder = StringBuilder()
                                reportBuilder.appendLine("=== REPORTE DE SOPORTE MONKIFY ===")
                                reportBuilder.appendLine("Tipo: ${reportType.name}")
                                reportBuilder.appendLine("Fecha: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}")
                                reportBuilder.appendLine("Versión: ${BuildConfig.VERSION_NAME} (code: ${BuildConfig.VERSION_CODE})")
                                reportBuilder.appendLine("Dispositivo: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE}, API ${android.os.Build.VERSION.SDK_INT})")
                                if (comment.isNotBlank()) {
                                    reportBuilder.appendLine("--- COMENTARIO ---")
                                    reportBuilder.appendLine(comment)
                                }
                                if (attachLogs) {
                                    reportBuilder.appendLine("--- REGISTROS RECIENTES (Últimos eventos) ---")
                                    val logs = AppLogger.getLogsAsText().takeLast(3000)
                                    reportBuilder.appendLine(logs.ifBlank { "Sin eventos registrados." })
                                }
                                reportBuilder.appendLine("==================================")

                                clipboardManager.setText(AnnotatedString(reportBuilder.toString()))
                                Toast.makeText(context, "Diagnóstico copiado al portapapeles 📋", Toast.LENGTH_LONG).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Copiar", color = MonkifyOnBackground, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )

        if (showLogsDialog) {
            val logsList by AppLogger.logsFlow.collectAsState()
            AlertDialog(
                onDismissRequest = { showLogsDialog = false },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Logs del Sistema (${logsList.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MonkifyOnBackground
                        )
                        IconButton(onClick = { AppLogger.clear() }) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Limpiar logs",
                                tint = MonkifyGray
                            )
                        }
                    }
                },
                text = {
                    val logScroll = rememberScrollState()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black)
                            .padding(8.dp)
                            .verticalScroll(logScroll)
                    ) {
                        Text(
                            text = if (logsList.isEmpty()) "No hay logs registrados en esta sesión." else logsList.joinToString("\n\n") { it.toString() },
                            color = Color(0xFF4ADE80),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val logsText = AppLogger.getLogsAsText()
                        clipboardManager.setText(AnnotatedString(logsText))
                        Toast.makeText(context, "Logs copiados al portapapeles 📋", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("Copiar Todo", color = MonkifyPrimary, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogsDialog = false }) {
                        Text("Cerrar", color = MonkifyGray)
                    }
                },
                containerColor = MonkifyDarkGray
            )
        }
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
