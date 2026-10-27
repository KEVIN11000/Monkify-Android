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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.tooling.preview.Preview
import com.k11000.monkify.ui.theme.MonkifyBackground
import com.k11000.monkify.ui.theme.MonkifyPrimary
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Franja horaria para el paisaje Zen dinámico (Day/Night cycle de Froggy).
 */
enum class ZenTimeOfDay {
    DAWN,    // 06:00 - 08:59 (Rosa dorado, ámbar suave, neblina fresca)
    DAY,     // 09:00 - 17:59 (Cielo azul vibrante, sol radiante, nubes suaves)
    SUNSET,  // 18:00 - 20:59 (Carmesí crepuscular, violeta, ascuas doradas)
    NIGHT    // 21:00 - 05:59 (Azul profundo, luna brillante, luciérnagas titilantes)
}

/**
 * Resuelve la franja horaria actual en base a la hora del sistema (0-23).
 */
fun resolveCurrentZenTimeOfDay(
    hour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
): ZenTimeOfDay {
    return when (hour) {
        in 6..8 -> ZenTimeOfDay.DAWN
        in 9..17 -> ZenTimeOfDay.DAY
        in 18..20 -> ZenTimeOfDay.SUNSET
        else -> ZenTimeOfDay.NIGHT
    }
}

@Composable
fun ZenBackground(
    isPlaying: Boolean,
    engine: String = "AURORA",
    timeOfDay: ZenTimeOfDay = resolveCurrentZenTimeOfDay(),
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

    // Fase angular para el ciclo continuo infinito (sin saltos en los bordes)
    // Velocidad Zen sosegada: 30 segundos en reproducción, 50 segundos en pausa
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.2831853f, // 2 * PI
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isPlaying) 30000 else 50000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ZenPhase"
    )

    // Pulsación suave de respiración armónica
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.93f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isPlaying) 6500 else 12000, easing = FastOutSlowInEasing),
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
                "FROGGY" -> drawFroggyBackground(width, height, phase, pulse, isPlaying, timeOfDay)
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
    val blob1Y = height * (0.28f + 0.10f * sin(phase))
    val blob1Radius = width * 0.80f * pulse

    val blob2X = width * (0.68f - 0.18f * sin(phase))
    val blob2Y = height * (0.42f + 0.12f * cos(phase))
    val blob2Radius = width * 0.90f * pulse

    val blob3X = width * (0.50f + 0.14f * cos(phase))
    val blob3Y = height * (0.62f - 0.10f * sin(phase))
    val blob3Radius = width * 0.75f

    // Blob 1: Azul Eléctrico (#2979FF)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                MonkifyPrimary.copy(alpha = if (isPlaying) 0.38f else 0.18f),
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
                Color(0xFF00E5FF).copy(alpha = if (isPlaying) 0.28f else 0.12f),
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
                Color(0xFF7C4DFF).copy(alpha = if (isPlaying) 0.30f else 0.14f),
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
    val maxAmplitude = if (isPlaying) height * 0.20f else height * 0.05f

    for (i in 0 until barCount) {
        val normalizedIndex = i.toFloat() / barCount
        val wave1 = sin(phase * 2f + normalizedIndex * 6.283f)
        val wave2 = cos(phase * 2f - normalizedIndex * 6.283f)
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
                MonkifyPrimary.copy(alpha = if (isPlaying) 0.22f else 0.09f),
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
    // 1. Espacio profundo obsidiana
    drawRect(Color(0xFF03050C))

    // 2. Galaxia Espiral Lejana (HD Cosmic Core)
    val galX = width * 0.72f
    val galY = height * 0.26f
    rotate(degrees = -26f, pivot = Offset(galX, galY)) {
        // Disco de gas exterior con respiración zen suave
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF7C3AED).copy(alpha = if (isPlaying) 0.35f else 0.18f),
                    Color(0xFF4338CA).copy(alpha = if (isPlaying) 0.20f else 0.08f),
                    Color.Transparent
                ),
                center = Offset(galX, galY),
                radius = width * 0.30f * pulse
            ),
            topLeft = Offset(galX - width * 0.30f, galY - width * 0.10f),
            size = Size(width * 0.60f, width * 0.20f)
        )
        // Núcleo galáctico denso y brillante
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFFBEB).copy(alpha = if (isPlaying) 0.95f else 0.75f),
                    Color(0xFFFDE047).copy(alpha = if (isPlaying) 0.60f else 0.35f),
                    Color(0xFFEC4899).copy(alpha = if (isPlaying) 0.35f else 0.15f),
                    Color.Transparent
                ),
                center = Offset(galX, galY),
                radius = width * 0.12f
            ),
            topLeft = Offset(galX - width * 0.12f, galY - width * 0.045f),
            size = Size(width * 0.24f, width * 0.09f)
        )
    }

    // 3. Nebulosa Cósmica 1: Púrpura Profundo (desplazamiento armónico sin saltos)
    val neb1X = width * (0.30f + 0.06f * sin(phase))
    val neb1Y = height * (0.35f + 0.05f * cos(phase))
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

    // 4. Nebulosa Cósmica 2: Magenta y Violeta Eléctrico
    val neb2X = width * (0.68f - 0.07f * cos(phase))
    val neb2Y = height * (0.62f + 0.05f * sin(phase))
    val neb2Radius = width * 0.85f * pulse
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF9D4EDD).copy(alpha = if (isPlaying) 0.30f else 0.14f),
                Color(0xFF3C096C).copy(alpha = if (isPlaying) 0.18f else 0.07f),
                Color.Transparent
            ),
            center = Offset(neb2X, neb2Y),
            radius = neb2Radius
        ),
        radius = neb2Radius,
        center = Offset(neb2X, neb2Y)
    )

    // 5. Nebulosa Cósmica 3: Azul Cósmico / Cyan Estelar
    val neb3X = width * (0.50f + 0.07f * sin(phase))
    val neb3Y = height * (0.18f + 0.04f * cos(phase))
    val neb3Radius = width * 0.75f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF0077B6).copy(alpha = if (isPlaying) 0.25f else 0.12f),
                Color.Transparent
            ),
            center = Offset(neb3X, neb3Y),
            radius = neb3Radius
        ),
        radius = neb3Radius,
        center = Offset(neb3X, neb3Y)
    )

    // 6. Nebulosa Cósmica 4: Vivero Estelar Rosa
    val neb4X = width * (0.22f - 0.05f * cos(phase))
    val neb4Y = height * (0.75f - 0.04f * sin(phase))
    val neb4Radius = width * 0.65f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF9F1239).copy(alpha = if (isPlaying) 0.18f else 0.07f),
                Color.Transparent
            ),
            center = Offset(neb4X, neb4Y),
            radius = neb4Radius
        ),
        radius = neb4Radius,
        center = Offset(neb4X, neb4Y)
    )

    // 7. Campo Estelar HD (72 estrellas periódicamente continuas)
    val starCount = 72
    for (i in 0 until starCount) {
        val seed = i * 137.5f
        val starX = (abs(sin(seed)) * width)
        val starY = (abs(sin(seed * 2.3f)) * height)
        val freq = 1 + (i % 3)
        val twinkle = (sin(phase * freq + i.toFloat()) + 1f) / 2f
        val alpha = (0.25f + 0.75f * twinkle) * (if (isPlaying) 1.0f else 0.6f)
        val starRadius = (1.1f + (i % 3) * 0.7f) * (if (i % 7 == 0) pulse else 1f)

        val starColor = when (i % 5) {
            0 -> Color.White
            1 -> Color(0xFFBAE6FD) // Cyan pálido
            2 -> Color(0xFFFEF08A) // Oro pálido
            3 -> Color(0xFFE9D5FF) // Lavanda suave
            else -> Color(0xFFF1F5F9)
        }.copy(alpha = alpha.coerceIn(0f, 1f))

        drawCircle(
            color = starColor,
            radius = starRadius,
            center = Offset(starX, starY)
        )

        // Picos de difracción en cruz de 4 puntas en estrellas prominentes
        if (i % 8 == 0) {
            val spikeLen = (7f + (i % 3) * 4f) * pulse
            val spikeAlpha = (alpha * 0.60f).coerceIn(0f, 1f)
            val spikeColor = starColor.copy(alpha = spikeAlpha)

            drawLine(
                color = spikeColor,
                start = Offset(starX - spikeLen, starY),
                end = Offset(starX + spikeLen, starY),
                strokeWidth = 1.1f
            )
            drawLine(
                color = spikeColor,
                start = Offset(starX, starY - spikeLen),
                end = Offset(starX, starY + spikeLen),
                strokeWidth = 1.1f
            )
        }
    }

    // 8. Estrella Fugaz periódica y continua (sin saltos bruscos: aparece y desvanece suavemente con seno)
    val meteorWave = sin(phase * 2f)
    if (meteorWave > 0.88f) {
        val progress = (meteorWave - 0.88f) / 0.12f // 0f a 1f
        val headX = width * 0.20f + progress * width * 0.45f
        val headY = height * 0.08f + progress * height * 0.18f
        val tailLen = 50f * sin(progress * 3.14159f)
        val tailX = headX - tailLen
        val tailY = headY - tailLen * 0.45f
        val meteorAlpha = (sin(progress * 3.14159f) * 0.90f).coerceIn(0f, 1f)

        if (tailLen > 2f) {
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFFBAE6FD).copy(alpha = meteorAlpha * 0.5f),
                        Color.White.copy(alpha = meteorAlpha)
                    ),
                    start = Offset(tailX, tailY),
                    end = Offset(headX, headY)
                ),
                start = Offset(tailX, tailY),
                end = Offset(headX, headY),
                strokeWidth = 2.0f
            )
            drawCircle(
                color = Color.White.copy(alpha = meteorAlpha),
                radius = 2.0f,
                center = Offset(headX, headY)
            )
        }
    }
}

private fun DrawScope.drawFroggyBackground(
    width: Float,
    height: Float,
    phase: Float,
    pulse: Float,
    isPlaying: Boolean,
    timeOfDay: ZenTimeOfDay
) {
    // 1. Cielo con paleta según franja horaria
    val skyColors = when (timeOfDay) {
        ZenTimeOfDay.DAWN -> listOf(
            Color(0xFF2E1C4E), // Púrpura matinal en cúspide
            Color(0xFF8C4F6E), // Rosa cálido
            Color(0xFFF39C6B)  // Ámbar suave en horizonte
        )
        ZenTimeOfDay.DAY -> listOf(
            Color(0xFF1E88E5), // Azul cerúleo cielo
            Color(0xFF42A5F5), // Azul brillante medio
            Color(0xFF90CAF9)  // Azul brisa luminoso horizonte
        )
        ZenTimeOfDay.SUNSET -> listOf(
            Color(0xFF1F1235), // Violeta profundo
            Color(0xFF831843), // Carmesí / púrpura crepúsculo
            Color(0xFFD97706), // Naranja cálido
            Color(0xFFF59E0B)  // Resplandor dorado horizonte
        )
        ZenTimeOfDay.NIGHT -> listOf(
            Color(0xFF0F2027),
            Color(0xFF203A43),
            Color(0xFF2C5364)
        )
    }

    drawRect(
        brush = Brush.verticalGradient(
            colors = skyColors,
            startY = 0f,
            endY = height * 0.70f
        )
    )

    // 2. Elementos celestes según franja (Sol / Luna / Nubes continuas)
    when (timeOfDay) {
        ZenTimeOfDay.NIGHT -> {
            // Luna suave con recorte
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

            // Estrellas nocturnas continuas
            for (i in 0 until 24) {
                val sX = (abs(sin(i * 91.3f)) * width)
                val sY = (abs(sin(i * 47.7f)) * height * 0.55f)
                val freq = 1 + (i % 3)
                val starTwinkle = (sin(phase * freq + i.toFloat()) + 1f) / 2f
                drawCircle(
                    color = Color.White.copy(alpha = (0.3f + 0.7f * starTwinkle).coerceIn(0f, 1f)),
                    radius = if (i % 4 == 0) 2.2f else 1.4f,
                    center = Offset(sX, sY)
                )
            }
        }
        ZenTimeOfDay.DAWN -> {
            // Estrellas matinales atenuándose
            for (i in 0 until 8) {
                val sX = (abs(sin(i * 57.3f)) * width)
                val sY = (abs(sin(i * 31.1f)) * height * 0.28f)
                val starTwinkle = (sin(phase + i.toFloat()) + 1f) / 2f
                drawCircle(
                    color = Color(0xFFFFF9C4).copy(alpha = (0.15f + 0.35f * starTwinkle)),
                    radius = 1.3f,
                    center = Offset(sX, sY)
                )
            }

            // Sol naciente en el horizonte este
            val sunX = width * 0.76f
            val sunY = height * 0.44f
            val sunGlow = width * 0.28f * pulse
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFE082).copy(alpha = if (isPlaying) 0.40f else 0.22f),
                        Color.Transparent
                    ),
                    center = Offset(sunX, sunY),
                    radius = sunGlow
                ),
                radius = sunGlow,
                center = Offset(sunX, sunY)
            )
            drawCircle(
                color = Color(0xFFFFF8E1),
                radius = 24f,
                center = Offset(sunX, sunY)
            )

            // Franja de neblina matinal dorada
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFFFFE0B2).copy(alpha = 0.16f),
                        Color.Transparent
                    ),
                    startY = height * 0.56f,
                    endY = height * 0.66f
                ),
                topLeft = Offset(0f, height * 0.56f),
                size = Size(width, height * 0.10f)
            )
        }
        ZenTimeOfDay.DAY -> {
            // Sol diurno radiante
            val sunX = width * 0.80f
            val sunY = height * 0.15f
            val sunGlow = width * 0.32f * pulse
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFF9C4).copy(alpha = if (isPlaying) 0.42f else 0.22f),
                        Color.Transparent
                    ),
                    center = Offset(sunX, sunY),
                    radius = sunGlow
                ),
                radius = sunGlow,
                center = Offset(sunX, sunY)
            )
            drawCircle(
                color = Color(0xFFFFF59D),
                radius = 26f,
                center = Offset(sunX, sunY)
            )
            drawCircle(
                color = Color.White,
                radius = 18f,
                center = Offset(sunX, sunY)
            )

            // Nubes suaves estilo Google Weather con movimiento armónico continuo (sin saltos)
            val c1X = width * 0.45f + sin(phase) * (width * 0.35f)
            val c1Y = height * 0.18f + sin(phase * 0.5f) * 6f
            drawCircle(Color.White.copy(alpha = 0.82f), radius = 24f, center = Offset(c1X, c1Y))
            drawCircle(Color.White.copy(alpha = 0.82f), radius = 32f, center = Offset(c1X + 22f, c1Y - 6f))
            drawCircle(Color.White.copy(alpha = 0.82f), radius = 26f, center = Offset(c1X + 48f, c1Y + 2f))
            drawCircle(Color.White.copy(alpha = 0.82f), radius = 18f, center = Offset(c1X + 66f, c1Y + 6f))

            val c2X = width * 0.50f - cos(phase) * (width * 0.38f)
            val c2Y = height * 0.32f + cos(phase * 0.5f) * 6f
            drawCircle(Color.White.copy(alpha = 0.68f), radius = 18f, center = Offset(c2X, c2Y))
            drawCircle(Color.White.copy(alpha = 0.68f), radius = 26f, center = Offset(c2X + 20f, c2Y - 4f))
            drawCircle(Color.White.copy(alpha = 0.68f), radius = 20f, center = Offset(c2X + 42f, c2Y + 2f))
        }
        ZenTimeOfDay.SUNSET -> {
            // Estrellas vespertinas tempranas
            for (i in 0 until 6) {
                val sX = (abs(sin(i * 43.1f)) * width)
                val sY = (abs(sin(i * 19.5f)) * height * 0.22f)
                val starTwinkle = (sin(phase + i.toFloat()) + 1f) / 2f
                drawCircle(
                    color = Color(0xFFFEF08A).copy(alpha = (0.25f + 0.5f * starTwinkle)),
                    radius = 1.4f,
                    center = Offset(sX, sY)
                )
            }

            // Sol grande del ocaso recortándose en el horizonte
            val sunX = width * 0.62f
            val sunY = height * 0.50f
            val sunGlow = width * 0.35f * pulse
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFB923C).copy(alpha = if (isPlaying) 0.50f else 0.30f),
                        Color.Transparent
                    ),
                    center = Offset(sunX, sunY),
                    radius = sunGlow
                ),
                radius = sunGlow,
                center = Offset(sunX, sunY)
            )
            drawCircle(
                color = Color(0xFFF97316),
                radius = 32f,
                center = Offset(sunX, sunY)
            )
            drawCircle(
                color = Color(0xFFFBBF24),
                radius = 22f,
                center = Offset(sunX, sunY)
            )

            // Nubes vespertinas teñidas con oscilación armónica continua
            val twCloudX = width * 0.30f + sin(phase) * (width * 0.18f)
            val tcY = height * 0.38f + cos(phase) * 5f
            drawCircle(Color(0xFF9D4EDD).copy(alpha = 0.35f), radius = 22f, center = Offset(twCloudX, tcY))
            drawCircle(Color(0xFFF59E0B).copy(alpha = 0.30f), radius = 28f, center = Offset(twCloudX + 24f, tcY - 4f))
            drawCircle(Color(0xFF9D4EDD).copy(alpha = 0.35f), radius = 20f, center = Offset(twCloudX + 50f, tcY + 2f))
        }
    }

    // 3. Colinas suaves en el horizonte con iluminación ambiental
    val (hill1Color, hill2Color) = when (timeOfDay) {
        ZenTimeOfDay.DAWN -> Color(0xFF3B2342) to Color(0xFF26132C)
        ZenTimeOfDay.DAY -> Color(0xFF2E7D32) to Color(0xFF1B5E20)
        ZenTimeOfDay.SUNSET -> Color(0xFF381B34) to Color(0xFF220C1F)
        ZenTimeOfDay.NIGHT -> Color(0xFF13222A) to Color(0xFF0C191F)
    }

    val hill1Path = Path().apply {
        moveTo(0f, height * 0.65f)
        quadraticTo(width * 0.35f, height * 0.58f, width * 0.70f, height * 0.64f)
        quadraticTo(width * 0.88f, height * 0.66f, width, height * 0.62f)
        lineTo(width, height)
        lineTo(0f, height)
        close()
    }
    drawPath(hill1Path, color = hill1Color)

    val hill2Path = Path().apply {
        moveTo(0f, height * 0.69f)
        quadraticTo(width * 0.45f, height * 0.63f, width, height * 0.71f)
        lineTo(width, height)
        lineTo(0f, height)
        close()
    }
    drawPath(hill2Path, color = hill2Color)

    // 4. Estanque de agua en la parte inferior
    val pondTop = height * 0.74f
    val (pondColor1, pondColor2) = when (timeOfDay) {
        ZenTimeOfDay.DAWN -> Color(0xFF221526) to Color(0xFF130A17)
        ZenTimeOfDay.DAY -> Color(0xFF0A3D62) to Color(0xFF062338)
        ZenTimeOfDay.SUNSET -> Color(0xFF2D1224) to Color(0xFF180613)
        ZenTimeOfDay.NIGHT -> Color(0xFF0B171C) to Color(0xFF060E12)
    }

    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(pondColor1, pondColor2),
            startY = pondTop,
            endY = height
        ),
        topLeft = Offset(0f, pondTop),
        size = Size(width, height - pondTop)
    )

    // Reflejo ambiental en el agua según cuerpo celeste
    val (reflectionX, reflectionColor) = when (timeOfDay) {
        ZenTimeOfDay.DAWN -> (width * 0.76f) to Color(0xFFFFE082)
        ZenTimeOfDay.DAY -> (width * 0.80f) to Color(0xFFFFF9C4)
        ZenTimeOfDay.SUNSET -> (width * 0.62f) to Color(0xFFF97316)
        ZenTimeOfDay.NIGHT -> (width * 0.82f) to Color(0xFFFFF9C4)
    }

    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(
                reflectionColor.copy(alpha = if (isPlaying) 0.26f * pulse else 0.14f),
                Color.Transparent
            ),
            center = Offset(reflectionX, pondTop + 35f),
            radius = width * 0.28f
        ),
        topLeft = Offset(reflectionX - width * 0.22f, pondTop + 15f),
        size = Size(width * 0.44f, 42f)
    )

    // 5. Nenúfares adaptados a la iluminación
    val (outerPadColor, frogPadColor) = when (timeOfDay) {
        ZenTimeOfDay.DAY -> Color(0xFF388E3C) to Color(0xFF4CAF50)
        ZenTimeOfDay.DAWN -> Color(0xFF254B3E) to Color(0xFF387259)
        ZenTimeOfDay.SUNSET -> Color(0xFF2E4839) to Color(0xFF3C6650)
        ZenTimeOfDay.NIGHT -> Color(0xFF1B4332) to Color(0xFF2D6A4F)
    }

    drawOval(
        color = outerPadColor,
        topLeft = Offset(width * 0.12f, pondTop + 45f),
        size = Size(75f, 24f)
    )
    drawOval(
        color = outerPadColor,
        topLeft = Offset(width * 0.75f, pondTop + 55f),
        size = Size(85f, 26f)
    )

    // Nenúfar central donde reposa Froggy
    val padCenterX = width * 0.48f
    val padCenterY = pondTop + 38f
    drawOval(
        color = frogPadColor,
        topLeft = Offset(padCenterX - 65f, padCenterY - 14f),
        size = Size(130f, 32f)
    )

    // 6. Ranita Froggy contemplando el horizonte
    val frogBobbing = (pulse - 1f) * 6f
    val frogBaseX = padCenterX
    val frogBaseY = padCenterY - 4f + frogBobbing

    val (frogBodyColor, frogBellyColor, frogFootColor) = when (timeOfDay) {
        ZenTimeOfDay.DAY -> Triple(Color(0xFF55B382), Color(0xFFA3E4C1), Color(0xFF388E3C))
        ZenTimeOfDay.DAWN -> Triple(Color(0xFF4BA678), Color(0xFF8FD8B3), Color(0xFF2D6A4F))
        ZenTimeOfDay.SUNSET -> Triple(Color(0xFF488C6C), Color(0xFF82C8A4), Color(0xFF2D6A4F))
        ZenTimeOfDay.NIGHT -> Triple(Color(0xFF40916C), Color(0xFF74C69D), Color(0xFF2D6A4F))
    }

    // Cuerpo y vientre
    drawOval(
        color = frogBodyColor,
        topLeft = Offset(frogBaseX - 22f, frogBaseY - 32f),
        size = Size(44f, 34f)
    )
    drawOval(
        color = frogBellyColor,
        topLeft = Offset(frogBaseX - 12f, frogBaseY - 26f),
        size = Size(24f, 22f)
    )

    // Mejillas tiernas (blush)
    val cheekColor = Color(0xFFFF8DA1).copy(alpha = if (timeOfDay == ZenTimeOfDay.DAY) 0.55f else 0.35f)
    drawCircle(color = cheekColor, radius = 3.5f, center = Offset(frogBaseX - 16f, frogBaseY - 21f))
    drawCircle(color = cheekColor, radius = 3.5f, center = Offset(frogBaseX + 16f, frogBaseY - 21f))

    // Ojos
    val eyeY = frogBaseY - 35f
    val eyeSclera = if (timeOfDay == ZenTimeOfDay.DAY) Color.White else Color(0xFFFFFDE7)
    val eyePupil = Color(0xFF1B4332)

    // Ojo izquierdo
    drawCircle(color = frogBodyColor, radius = 8f, center = Offset(frogBaseX - 11f, eyeY))
    drawCircle(color = eyeSclera, radius = 5.5f, center = Offset(frogBaseX - 11f, eyeY))
    drawCircle(color = eyePupil, radius = 3f, center = Offset(frogBaseX - 10f, eyeY - 1f))
    if (timeOfDay == ZenTimeOfDay.DAY) {
        drawCircle(color = Color.White, radius = 1.2f, center = Offset(frogBaseX - 11f, eyeY - 2f))
    }

    // Ojo derecho
    drawCircle(color = frogBodyColor, radius = 8f, center = Offset(frogBaseX + 11f, eyeY))
    drawCircle(color = eyeSclera, radius = 5.5f, center = Offset(frogBaseX + 11f, eyeY))
    drawCircle(color = eyePupil, radius = 3f, center = Offset(frogBaseX + 10f, eyeY - 1f))
    if (timeOfDay == ZenTimeOfDay.DAY) {
        drawCircle(color = Color.White, radius = 1.2f, center = Offset(frogBaseX + 9f, eyeY - 2f))
    }

    // Patitas sentadas
    drawOval(
        color = frogFootColor,
        topLeft = Offset(frogBaseX - 26f, frogBaseY - 12f),
        size = Size(16f, 12f)
    )
    drawOval(
        color = frogFootColor,
        topLeft = Offset(frogBaseX + 10f, frogBaseY - 12f),
        size = Size(16f, 12f)
    )

    // 7. Partículas continuas periódicas según franja (cero saltos al reiniciar ciclo)
    when (timeOfDay) {
        ZenTimeOfDay.NIGHT -> {
            // Luciérnagas doradas en oscilación armónica continua
            val fireflyCount = 12
            for (i in 0 until fireflyCount) {
                val fSeed = i * 83.1f
                val fx = (width * (0.15f + 0.70f * abs(sin(fSeed)))) + sin(phase + i * 1.4f) * 25f
                val fy = (height * (0.60f + 0.30f * abs(sin(fSeed * 1.7f)))) + cos(phase + i * 2.1f) * 16f
                val fPulse = (sin(phase * 2f + i * 1.8f) + 1f) / 2f
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
        ZenTimeOfDay.DAY -> {
            // Semillas de diente de león y motas de polen solar flotando suavemente en la brisa
            val seedCount = 14
            for (i in 0 until seedCount) {
                val pSeed = i * 71.3f
                val px = (width * (0.10f + 0.80f * abs(sin(pSeed)))) + sin(phase + i * 0.9f) * 30f
                val py = (height * 0.65f) + cos(phase + i * 1.2f) * (height * 0.16f)
                val pAlpha = (0.35f + 0.65f * sin(phase * 2f + i)).coerceIn(0.2f, 0.9f)

                drawCircle(
                    color = Color.White.copy(alpha = pAlpha * (if (isPlaying) 0.85f else 0.50f)),
                    radius = 2.2f,
                    center = Offset(px, py)
                )
                drawLine(
                    color = Color.White.copy(alpha = pAlpha * 0.40f),
                    start = Offset(px, py),
                    end = Offset(px - 3f, py + 5f),
                    strokeWidth = 1f
                )
            }
        }
        ZenTimeOfDay.DAWN -> {
            // Rocío matutino fresco destellando armónicamente
            val dewCount = 12
            for (i in 0 until dewCount) {
                val dSeed = i * 61.7f
                val dx = (width * (0.15f + 0.70f * abs(sin(dSeed)))) + sin(phase + i) * 12f
                val dy = height * (0.68f + 0.22f * abs(sin(dSeed * 2.1f))) + cos(phase + i) * 6f
                val dPulse = (sin(phase * 2f + i * 1.5f) + 1f) / 2f
                val dAlpha = (0.3f + 0.7f * dPulse) * (if (isPlaying) 0.8f else 0.4f)

                drawCircle(
                    color = Color(0xFFE0F7FA).copy(alpha = dAlpha),
                    radius = 2.5f,
                    center = Offset(dx, dy)
                )
            }
        }
        ZenTimeOfDay.SUNSET -> {
            // Ascuas doradas cálidas flotando suavemente
            val emberCount = 14
            for (i in 0 until emberCount) {
                val eSeed = i * 79.9f
                val ex = (width * (0.15f + 0.70f * abs(sin(eSeed)))) + sin(phase + i * 1.1f) * 20f
                val ey = (height * 0.68f) + cos(phase + i * 1.3f) * (height * 0.14f)
                val eAlpha = (0.35f + 0.65f * sin(phase * 2f + i)).coerceIn(0.1f, 0.95f)

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFF9800).copy(alpha = eAlpha * 0.70f),
                            Color.Transparent
                        ),
                        center = Offset(ex, ey),
                        radius = 12f
                    ),
                    radius = 12f,
                    center = Offset(ex, ey)
                )
                drawCircle(
                    color = Color(0xFFFFE082).copy(alpha = eAlpha),
                    radius = 2.2f,
                    center = Offset(ex, ey)
                )
            }
        }
    }
}

// ==========================================
// PREVIEWS COMPOSE PARA VALIDACIÓN VISUAL
// ==========================================

@Preview(name = "Zen Froggy Day", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun ZenFroggyDayPreview() {
    ZenBackground(
        isPlaying = true,
        engine = "FROGGY",
        timeOfDay = ZenTimeOfDay.DAY
    )
}

@Preview(name = "Zen Froggy Sunset", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun ZenFroggySunsetPreview() {
    ZenBackground(
        isPlaying = true,
        engine = "FROGGY",
        timeOfDay = ZenTimeOfDay.SUNSET
    )
}

@Preview(name = "Zen Froggy Night", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun ZenFroggyNightPreview() {
    ZenBackground(
        isPlaying = true,
        engine = "FROGGY",
        timeOfDay = ZenTimeOfDay.NIGHT
    )
}

@Preview(name = "Zen Froggy Dawn", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun ZenFroggyDawnPreview() {
    ZenBackground(
        isPlaying = true,
        engine = "FROGGY",
        timeOfDay = ZenTimeOfDay.DAWN
    )
}

@Preview(name = "Zen Cosmos HD", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun ZenCosmosHDPreview() {
    ZenBackground(
        isPlaying = true,
        engine = "COSMOS"
    )
}
