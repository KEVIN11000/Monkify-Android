package com.k11000.monkify.lockscreen

import android.app.KeyguardManager
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.k11000.monkify.playback.PlayerViewModel
import com.k11000.monkify.ui.components.ZenBackground
import com.k11000.monkify.ui.screens.viewmodel.SettingsViewModel
import com.k11000.monkify.ui.theme.MonkifyBackground
import com.k11000.monkify.ui.theme.MonkifyGray
import com.k11000.monkify.ui.theme.MonkifyOnBackground
import com.k11000.monkify.ui.theme.MonkifyPrimary
import com.k11000.monkify.ui.theme.MyApplicationTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.roundToInt

@AndroidEntryPoint
class LockScreenActivity : ComponentActivity(), SensorEventListener {

    private val playerViewModel: PlayerViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    private var sensorManager: SensorManager? = null
    private var proximitySensor: Sensor? = null
    private var isCoveredInPocket by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureLockScreenFlags()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        setContent {
            MyApplicationTheme {
                val isPlaying by playerViewModel.isPlaying.collectAsState()
                val currentTitle by playerViewModel.currentTrackTitle.collectAsState()
                val currentArtist by playerViewModel.currentTrackArtist.collectAsState()
                val coverUrl by playerViewModel.coverUrl.collectAsState()
                val progress by playerViewModel.progress.collectAsState()
                val currentPosition by playerViewModel.currentPosition.collectAsState()
                val duration by playerViewModel.duration.collectAsState()
                val settingsState by settingsViewModel.uiState.collectAsState()

                // Actualizar bandera de pantalla activa según reproducción
                DisposableEffect(isPlaying) {
                    if (isPlaying) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                    onDispose { }
                }

                LockScreenContent(
                    isPlaying = isPlaying,
                    title = currentTitle,
                    artist = currentArtist,
                    coverUrl = coverUrl,
                    progress = progress,
                    currentPosition = currentPosition,
                    duration = duration,
                    zenEngine = settingsState.zenEngine,
                    isCovered = isCoveredInPocket,
                    onPlayPause = { playerViewModel.togglePlayPause() },
                    onSkipNext = { playerViewModel.skipToNext() },
                    onSkipPrevious = { playerViewModel.skipToPrevious() },
                    onUnlock = { unlockAndDismiss() }
                )
            }
        }
    }

    private fun configureLockScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )
    }

    private fun unlockAndDismiss() {
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            keyguardManager?.requestDismissKeyguard(this, null)
        }
        finish()
    }

    override fun onResume() {
        super.onResume()
        proximitySensor?.let { sensor ->
            sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || proximitySensor == null) return
        val distance = event.values[0]
        val maxRange = proximitySensor?.maximumRange ?: 5f
        // Si está tapado en el bolsillo (distancia cercana a 0)
        isCoveredInPocket = distance < maxRange.coerceAtMost(4f)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

@Composable
fun LockScreenContent(
    isPlaying: Boolean,
    title: String,
    artist: String,
    coverUrl: String?,
    progress: Float,
    currentPosition: Long,
    duration: Long,
    zenEngine: String,
    isCovered: Boolean,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onUnlock: () -> Unit
) {
    // Si el sensor de proximidad detecta bolsillo, oscurecemos 100% la pantalla para ahorro extremo de batería
    if (isCovered) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        )
        return
    }

    // Pixel-shifting sutil anti-burn-in para paneles OLED
    val infiniteTransition = rememberInfiniteTransition(label = "PixelShift")
    val shiftX by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 60000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shiftX"
    )
    val shiftY by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 45000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shiftY"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MonkifyBackground)
    ) {
        // Fondo ambiental Zen dinámico
        ZenBackground(
            engine = zenEngine,
            isPlaying = isPlaying,
            modifier = Modifier.fillMaxSize()
        )

        // Contenido con pixel-shifting
        Column(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(shiftX.roundToInt(), shiftY.roundToInt()) }
                .padding(horizontal = 28.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Cabecera Minimalista
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MonkifyPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "MONKIFY ZEN DISPLAY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MonkifyGray
                )
            }

            // Centro: Carátula e Información de Canción
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Carátula flotante con sombra suave
                Box(
                    modifier = Modifier
                        .size(230.dp)
                        .shadow(
                            elevation = 20.dp,
                            shape = RoundedCornerShape(20.dp),
                            spotColor = MonkifyPrimary
                        )
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1E1E1E)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!coverUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = coverUrl,
                            contentDescription = "Carátula",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = MonkifyPrimary,
                            modifier = Modifier.size(72.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = title.ifEmpty { "Sin reproducir" },
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MonkifyOnBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = artist.ifEmpty { "Monkify" },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MonkifyGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Barra de Progreso
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MonkifyPrimary,
                    trackColor = Color(0x33FFFFFF)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(0.85f),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatMillis(currentPosition),
                        style = MaterialTheme.typography.labelSmall,
                        color = MonkifyGray
                    )
                    Text(
                        text = formatMillis(duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = MonkifyGray
                    )
                }
            }

            // Controles de Reproducción y Desbloqueo
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onSkipPrevious,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Anterior",
                            tint = MonkifyOnBackground,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(MonkifyPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = onPlayPause,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                                tint = Color.Black,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onSkipNext,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Siguiente",
                            tint = MonkifyOnBackground,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Botón minimalista de Desbloqueo
                OutlinedButton(
                    onClick = onUnlock,
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = null,
                        tint = MonkifyGray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tocar para desbloquear",
                        style = MaterialTheme.typography.labelMedium,
                        color = MonkifyGray
                    )
                }
            }
        }
    }
}

private fun formatMillis(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
