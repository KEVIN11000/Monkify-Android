package com.k11000.monkify.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.k11000.monkify.ui.theme.MonkifyBackground
import com.k11000.monkify.ui.theme.MonkifyPrimary
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun ZenBackground(
    isPlaying: Boolean,
    engine: String = "AURORA",
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ZenTransition")

    // Fase angular para el desplazamiento sinusoidal de la aurora o visualizador
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f, // 2 * PI
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isPlaying) 6500 else 15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ZenPhase"
    )

    // Pulsación suave de respiración
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isPlaying) 2600 else 6000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ZenPulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MonkifyBackground)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            if (engine == "VISUALIZER") {
                // Motor: Visualizador de Audio (Barras de espectro luminescentes)
                val barCount = 32
                val barWidth = width / (barCount * 1.55f)
                val centerY = height * 0.48f
                val maxAmplitude = if (isPlaying) height * 0.22f else height * 0.05f

                for (i in 0 until barCount) {
                    val normalizedIndex = i.toFloat() / barCount
                    val wave1 = sin(phase * 2.2f + normalizedIndex * 8f)
                    val wave2 = sin(phase * 3.1f - normalizedIndex * 4.5f)
                    val combined = (wave1 + wave2) / 2f
                    val barHeight = (abs(combined) * maxAmplitude * pulse).coerceAtLeast(8f)

                    val x = (i * (barWidth * 1.55f)) + (barWidth * 0.25f)
                    val top = centerY - barHeight / 2f

                    val barBrush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF00E5FF).copy(alpha = if (isPlaying) 0.85f else 0.35f),
                            MonkifyPrimary.copy(alpha = if (isPlaying) 0.95f else 0.45f),
                            Color(0xFF7C4DFF).copy(alpha = if (isPlaying) 0.70f else 0.25f)
                        ),
                        startY = top,
                        endY = top + barHeight
                    )

                    drawRoundRect(
                        brush = barBrush,
                        topLeft = Offset(x, top),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2f)
                    )
                }

                // Resplandor difuso ambiental centrado
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MonkifyPrimary.copy(alpha = if (isPlaying) 0.24f else 0.09f),
                            Color.Transparent
                        ),
                        center = Offset(width * 0.5f, centerY),
                        radius = width * 0.7f
                    ),
                    radius = width * 0.7f,
                    center = Offset(width * 0.5f, centerY)
                )
            } else {
                // Motor por defecto: Aurora Boreal (Gradientes radiales orgánicos vivos)
                val blob1X = width * (0.35f + 0.16f * sin(phase))
                val blob1Y = height * (0.28f + 0.10f * sin(phase * 0.85f))
                val blob1Radius = width * 0.80f * pulse

                val blob2X = width * (0.68f - 0.18f * sin(phase * 1.15f))
                val blob2Y = height * (0.42f + 0.12f * sin(phase * 0.95f))
                val blob2Radius = width * 0.90f * pulse

                val blob3X = width * (0.50f + 0.14f * sin(phase * 0.75f + 1f))
                val blob3Y = height * (0.62f - 0.10f * sin(phase * 1.25f))
                val blob3Radius = width * 0.75f

                // Blob 1: Azul Eléctrico (#2979FF)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MonkifyPrimary.copy(alpha = if (isPlaying) 0.40f else 0.18f),
                            Color.Transparent
                        ),
                        center = Offset(blob1X, blob1Y),
                        radius = blob1Radius
                    ),
                    radius = blob1Radius,
                    center = Offset(blob1X, blob1Y)
                )

                // Blob 2: Cyan Neón (#00E5FF)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF00E5FF).copy(alpha = if (isPlaying) 0.30f else 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(blob2X, blob2Y),
                        radius = blob2Radius
                    ),
                    radius = blob2Radius,
                    center = Offset(blob2X, blob2Y)
                )

                // Blob 3: Violeta Espacial (#7C4DFF)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF7C4DFF).copy(alpha = if (isPlaying) 0.32f else 0.14f),
                            Color.Transparent
                        ),
                        center = Offset(blob3X, blob3Y),
                        radius = blob3Radius
                    ),
                    radius = blob3Radius,
                    center = Offset(blob3X, blob3Y)
                )
            }
        }
    }
}
