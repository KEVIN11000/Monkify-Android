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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
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
    if (engine == "NONE") {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
        )
        return
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ZenTransition")

    // Fase angular para el desplazamiento sinusoidal de animaciones
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

            when (engine) {
                "VISUALIZER" -> drawVisualizerBackground(width, height, phase, pulse, isPlaying)
                "COSMOS" -> drawCosmosBackground(width, height, phase, pulse, isPlaying)
                "FROGGY" -> drawFroggyBackground(width, height, phase, pulse, isPlaying)
                else -> drawAuroraBackground(width, height, phase, pulse, isPlaying)
            }
        }
    }
}

private fun DrawScope.drawAuroraBackground(
    width: Float,
    height: Float,
    phase: Float,
    pulse: Float,
    isPlaying: Boolean
) {
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

private fun DrawScope.drawVisualizerBackground(
    width: Float,
    height: Float,
    phase: Float,
    pulse: Float,
    isPlaying: Boolean
) {
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
}

private fun DrawScope.drawCosmosBackground(
    width: Float,
    height: Float,
    phase: Float,
    pulse: Float,
    isPlaying: Boolean
) {
    // Espacio profundo
    drawRect(Color(0xFF04060E))

    // Nebulosa Cósmica 1: Púrpura Profundo
    val neb1X = width * (0.32f + 0.08f * sin(phase * 0.7f))
    val neb1Y = height * (0.35f + 0.06f * sin(phase * 0.5f))
    val neb1Radius = width * 0.95f * pulse
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF5A189A).copy(alpha = if (isPlaying) 0.35f else 0.16f),
                Color(0xFF240046).copy(alpha = if (isPlaying) 0.20f else 0.08f),
                Color.Transparent
            ),
            center = Offset(neb1X, neb1Y),
            radius = neb1Radius
        ),
        radius = neb1Radius,
        center = Offset(neb1X, neb1Y)
    )

    // Nebulosa Cósmica 2: Magenta y Violeta Espacial
    val neb2X = width * (0.68f - 0.09f * sin(phase * 0.6f))
    val neb2Y = height * (0.62f + 0.07f * sin(phase * 0.8f))
    val neb2Radius = width * 0.85f * pulse
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF9D4EDD).copy(alpha = if (isPlaying) 0.28f else 0.12f),
                Color(0xFF3C096C).copy(alpha = if (isPlaying) 0.18f else 0.07f),
                Color.Transparent
            ),
            center = Offset(neb2X, neb2Y),
            radius = neb2Radius
        ),
        radius = neb2Radius,
        center = Offset(neb2X, neb2Y)
    )

    // Nebulosa Cósmica 3: Azul Cósmico / Cyan Estelar
    val neb3X = width * (0.50f + 0.10f * sin(phase * 0.9f))
    val neb3Y = height * (0.20f + 0.05f * sin(phase * 1.1f))
    val neb3Radius = width * 0.70f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF0077B6).copy(alpha = if (isPlaying) 0.22f else 0.10f),
                Color.Transparent
            ),
            center = Offset(neb3X, neb3Y),
            radius = neb3Radius
        ),
        radius = neb3Radius,
        center = Offset(neb3X, neb3Y)
    )

    // Campo de Estrellas Parallax y Centelleantes
    val starCount = 48
    for (i in 0 until starCount) {
        val seed = i * 137.5f
        val starX = (abs(sin(seed)) * width)
        val starY = (abs(sin(seed * 2.3f)) * height)
        val starSpeed = 1.0f + (i % 5) * 0.4f
        val twinkle = (sin(phase * starSpeed + i.toFloat()) + 1f) / 2f
        val alpha = (0.25f + 0.75f * twinkle) * (if (isPlaying) 1.0f else 0.6f)
        val starRadius = (1.2f + (i % 3) * 0.8f) * (if (i % 7 == 0) pulse else 1f)
        val starColor = when (i % 4) {
            0 -> Color(0xFFFFFFFF)
            1 -> Color(0xFFE0E7FF)
            2 -> Color(0xFFFEF08A)
            else -> Color(0xFFC7D2FE)
        }.copy(alpha = alpha.coerceIn(0f, 1f))

        drawCircle(
            color = starColor,
            radius = starRadius,
            center = Offset(starX, starY)
        )
    }
}

private fun DrawScope.drawFroggyBackground(
    width: Float,
    height: Float,
    phase: Float,
    pulse: Float,
    isPlaying: Boolean
) {
    // 1. Cielo crepúsculo / noche Google Weather
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0F2027),
                Color(0xFF203A43),
                Color(0xFF2C5364)
            ),
            startY = 0f,
            endY = height * 0.70f
        )
    )

    // 2. Luna suave en la esquina superior derecha con halo cálido
    val moonX = width * 0.82f
    val moonY = height * 0.12f
    val moonGlowRadius = width * 0.28f * pulse
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFF9C4).copy(alpha = if (isPlaying) 0.35f else 0.18f),
                Color.Transparent
            ),
            center = Offset(moonX, moonY),
            radius = moonGlowRadius
        ),
        radius = moonGlowRadius,
        center = Offset(moonX, moonY)
    )
    drawCircle(
        color = Color(0xFFFFFDE7),
        radius = 22f,
        center = Offset(moonX, moonY)
    )
    drawCircle(
        color = Color(0xFF1E3545),
        radius = 18f,
        center = Offset(moonX - 6f, moonY - 4f)
    )

    // 3. Estrellas tranquilas
    for (i in 0 until 24) {
        val sX = (abs(sin(i * 91.3f)) * width)
        val sY = (abs(sin(i * 47.7f)) * height * 0.55f)
        val starTwinkle = (sin(phase * 1.8f + i) + 1f) / 2f
        drawCircle(
            color = Color.White.copy(alpha = (0.3f + 0.7f * starTwinkle).coerceIn(0f, 1f)),
            radius = if (i % 4 == 0) 2.2f else 1.4f,
            center = Offset(sX, sY)
        )
    }

    // 4. Siluetas de colinas suaves en el horizonte
    val hill1Path = Path().apply {
        moveTo(0f, height * 0.65f)
        quadraticTo(width * 0.35f, height * 0.58f, width * 0.70f, height * 0.64f)
        quadraticTo(width * 0.88f, height * 0.66f, width, height * 0.62f)
        lineTo(width, height)
        lineTo(0f, height)
        close()
    }
    drawPath(hill1Path, color = Color(0xFF13222A))

    val hill2Path = Path().apply {
        moveTo(0f, height * 0.69f)
        quadraticTo(width * 0.45f, height * 0.63f, width, height * 0.71f)
        lineTo(width, height)
        lineTo(0f, height)
        close()
    }
    drawPath(hill2Path, color = Color(0xFF0C191F))

    // 5. Estanque de agua en la parte inferior
    val pondTop = height * 0.74f
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0B171C),
                Color(0xFF060E12)
            ),
            startY = pondTop,
            endY = height
        ),
        topLeft = Offset(0f, pondTop),
        size = Size(width, height - pondTop)
    )

    // Reflejo suave de la luna en el agua
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFF9C4).copy(alpha = 0.18f * pulse),
                Color.Transparent
            ),
            center = Offset(moonX, pondTop + 35f),
            radius = width * 0.25f
        ),
        topLeft = Offset(moonX - width * 0.2f, pondTop + 15f),
        size = Size(width * 0.4f, 40f)
    )

    // 6. Nenúfares
    drawOval(
        color = Color(0xFF1B4332),
        topLeft = Offset(width * 0.12f, pondTop + 45f),
        size = Size(75f, 24f)
    )
    drawOval(
        color = Color(0xFF1B4332),
        topLeft = Offset(width * 0.75f, pondTop + 55f),
        size = Size(85f, 26f)
    )

    // Nenúfar central donde reposa Froggy
    val padCenterX = width * 0.48f
    val padCenterY = pondTop + 38f
    drawOval(
        color = Color(0xFF2D6A4F),
        topLeft = Offset(padCenterX - 65f, padCenterY - 14f),
        size = Size(130f, 32f)
    )

    // 7. Ranita Froggy sentada contemplando el cielo nocturno
    val frogBobbing = (pulse - 1f) * 6f
    val frogBaseX = padCenterX
    val frogBaseY = padCenterY - 4f + frogBobbing

    drawOval(
        color = Color(0xFF40916C),
        topLeft = Offset(frogBaseX - 22f, frogBaseY - 32f),
        size = Size(44f, 34f)
    )
    drawOval(
        color = Color(0xFF74C69D),
        topLeft = Offset(frogBaseX - 12f, frogBaseY - 26f),
        size = Size(24f, 22f)
    )

    val eyeY = frogBaseY - 35f
    // Ojo izquierdo
    drawCircle(color = Color(0xFF40916C), radius = 8f, center = Offset(frogBaseX - 11f, eyeY))
    drawCircle(color = Color.White, radius = 5.5f, center = Offset(frogBaseX - 11f, eyeY))
    drawCircle(color = Color(0xFF1B4332), radius = 3f, center = Offset(frogBaseX - 10f, eyeY - 1f))
    // Ojo derecho
    drawCircle(color = Color(0xFF40916C), radius = 8f, center = Offset(frogBaseX + 11f, eyeY))
    drawCircle(color = Color.White, radius = 5.5f, center = Offset(frogBaseX + 11f, eyeY))
    drawCircle(color = Color(0xFF1B4332), radius = 3f, center = Offset(frogBaseX + 10f, eyeY - 1f))

    // Patitas sentadas
    drawOval(
        color = Color(0xFF2D6A4F),
        topLeft = Offset(frogBaseX - 26f, frogBaseY - 12f),
        size = Size(16f, 12f)
    )
    drawOval(
        color = Color(0xFF2D6A4F),
        topLeft = Offset(frogBaseX + 10f, frogBaseY - 12f),
        size = Size(16f, 12f)
    )

    // 8. Luciérnagas flotantes alrededor de la charca
    val fireflyCount = 12
    for (i in 0 until fireflyCount) {
        val fSeed = i * 83.1f
        val driftX = sin(phase * 1.2f + i * 1.4f) * 25f
        val driftY = sin(phase * 0.9f + i * 2.1f) * 15f
        val fx = (width * (0.15f + 0.70f * abs(sin(fSeed)))) + driftX
        val fy = (height * (0.60f + 0.32f * abs(sin(fSeed * 1.7f)))) + driftY
        val fPulse = (sin(phase * 3.0f + i * 1.8f) + 1f) / 2f
        val fAlpha = (0.25f + 0.75f * fPulse) * (if (isPlaying) 1.0f else 0.5f)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFD54F).copy(alpha = fAlpha * 0.65f),
                    Color.Transparent
                ),
                center = Offset(fx, fy),
                radius = 18f
            ),
            radius = 18f,
            center = Offset(fx, fy)
        )
        drawCircle(
            color = Color(0xFFFFF9C4).copy(alpha = fAlpha.coerceIn(0f, 1f)),
            radius = 2.8f,
            center = Offset(fx, fy)
        )
    }
}
