package com.k11000.monkify.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.rive.runtime.kotlin.RiveAnimationView
import com.k11000.monkify.domain.model.FroggyScenery
import com.k11000.monkify.domain.model.WeatherCondition
import com.k11000.monkify.domain.model.ZenDayPart
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
    NIGHT;   // 21:00 - 05:59 (Azul profundo, luna brillante, luciérnagas titilantes)

    fun toZenDayPart(): ZenDayPart = when (this) {
        DAWN -> ZenDayPart.DAWN
        DAY -> ZenDayPart.DAY
        SUNSET -> ZenDayPart.SUNSET
        NIGHT -> ZenDayPart.NIGHT
    }
}

fun ZenDayPart.toZenTimeOfDay(): ZenTimeOfDay = when (this) {
    ZenDayPart.DAWN -> ZenTimeOfDay.DAWN
    ZenDayPart.DAY -> ZenTimeOfDay.DAY
    ZenDayPart.SUNSET -> ZenTimeOfDay.SUNSET
    ZenDayPart.NIGHT -> ZenTimeOfDay.NIGHT
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
    weatherCondition: WeatherCondition = WeatherCondition.SUNNY,
    scenery: FroggyScenery = FroggyScenery.FIELDS,
    useOfficialFroggyAssets: Boolean = true,
    riveAnimationRes: Int? = null,
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
        // Capa 0: Fondo escénico de Google Weather en WebP de alta definición
        if (engine == "FROGGY" && useOfficialFroggyAssets) {
            val bgRes = FroggyAssetCatalog.getBackgroundRes(
                scenery = scenery,
                dayPart = timeOfDay.toZenDayPart(),
                condition = weatherCondition
            )
            Crossfade(
                targetState = bgRes,
                animationSpec = tween(durationMillis = 800),
                label = "FroggyBgCrossfade"
            ) { resId ->
                Image(
                    painter = painterResource(id = resId),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Capa 1: Efectos atmosféricos reactivos y personajes vectoriales (Canvas)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            when (engine) {
                "VISUALIZER" -> drawVisualizerBackground(width, height, phase, pulse, isPlaying)
                "COSMOS" -> drawCosmosBackground(width, height, phase, pulse, isPlaying)
                "FROGGY" -> {
                    if (useOfficialFroggyAssets) {
                        drawFroggyAtmosphericOverlay(
                            width = width,
                            height = height,
                            phase = phase,
                            pulse = pulse,
                            isPlaying = isPlaying,
                            timeOfDay = timeOfDay,
                            condition = weatherCondition
                        )
                    } else {
                        drawFroggyBackground(width, height, phase, pulse, isPlaying, timeOfDay)
                    }
                }
                else -> drawAuroraBackground(width, height, phase, pulse, isPlaying)
            }
        }

        // Capa 2: Contenedor Rive Animation (si se provee recurso binario .riv)
        if (engine == "FROGGY" && riveAnimationRes != null) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .align(Alignment.BottomCenter),
                factory = { ctx ->
                    RiveAnimationView(ctx).apply {
                        setRiveResource(riveAnimationRes)
                    }
                }
            )
        }
    }
}

/**
 * Superpone partículas atmosféricas reactivas al clima sobre los fondos oficiales de Froggy.
 */
private fun DrawScope.drawFroggyAtmosphericOverlay(
    width: Float,
    height: Float,
    phase: Float,
    pulse: Float,
    isPlaying: Boolean,
    timeOfDay: ZenTimeOfDay,
    condition: WeatherCondition
) {
    when (condition) {
        WeatherCondition.RAINY -> drawAtmosphericRain(width, height, phase, pulse, isPlaying)
        WeatherCondition.SNOWY -> drawAtmosphericSnow(width, height, phase, pulse, isPlaying)
        WeatherCondition.HAZY -> drawAtmosphericHaze(width, height, phase, pulse)
        WeatherCondition.SUNNY -> drawAtmosphericSunrays(width, height, phase, pulse, timeOfDay)
        WeatherCondition.CLOUDY -> drawAtmosphericClouds(width, height, phase, pulse)
    }

    // Luciérnagas y estrellas nocturnas en franja nocturna
    if (timeOfDay == ZenTimeOfDay.NIGHT) {
        drawNightFireflies(width, height, phase, pulse)
    }
}

private fun DrawScope.drawAtmosphericRain(
    width: Float,
    height: Float,
    phase: Float,
    pulse: Float,
    isPlaying: Boolean
) {
    val dropCount = 42
    val rainColor = Color(0x99B3E5FC)

    for (i in 0 until dropCount) {
        val seed = i * 137.5f
        val xProgress = ((seed % width) / width)
        val speed = 1.0f + (i % 5) * 0.25f

        // Movimiento continuo diagonal en cascada sin saltos
        val rawY = (phase * speed * 220f + seed * 3.7f) % (height * 1.2f)
        val x = (width * xProgress) - (rawY * 0.15f) // Inclinación suave por viento
        val y = rawY - (height * 0.1f)

        if (y in 0f..height && x in 0f..width) {
            drawLine(
                color = rainColor,
                start = Offset(x, y),
                end = Offset(x - 6f, y + 26f),
                strokeWidth = 2.2f
            )

            // Ondas de agua en la parte inferior (estanque/piso)
            if (y > height * 0.78f && i % 3 == 0) {
                val ripplePhase = (rawY / height) * 6.2831f
                val rippleRadius = 8f + 14f * abs(sin(ripplePhase))
                val rippleAlpha = (0.35f * (1f - (rippleRadius / 22f))).coerceIn(0f, 0.4f)

                drawCircle(
                    color = Color.White.copy(alpha = rippleAlpha),
                    center = Offset(x, y + 26f),
                    radius = rippleRadius
                )
            }
        }
    }
}

private fun DrawScope.drawAtmosphericSnow(
    width: Float,
    height: Float,
    phase: Float,
    pulse: Float,
    isPlaying: Boolean
) {
    val flakeCount = 38
    val snowColor = Color(0xDDF0F4F8)

    for (i in 0 until flakeCount) {
        val seed = i * 97.3f
        val speed = 0.5f + (i % 4) * 0.2f
        val rawY = (phase * speed * 90f + seed * 5.1f) % (height * 1.1f)
        val sway = 16f * sin(phase * 2f + seed)
        val x = ((seed % width) + sway).coerceIn(0f, width)
        val y = rawY

        val size = 2.5f + (i % 3) * 1.5f
        val alpha = 0.5f + 0.4f * abs(cos(phase + seed))

        drawCircle(
            color = snowColor.copy(alpha = alpha),
            center = Offset(x, y),
            radius = size * pulse
        )
    }
}

private fun DrawScope.drawAtmosphericHaze(
    width: Float,
    height: Float,
    phase: Float,
    pulse: Float
) {
    // Velo de neblina flotante en horizonte medio
    val hazeY1 = height * (0.45f + 0.04f * sin(phase * 0.8f))
    val hazeY2 = height * (0.62f - 0.03f * cos(phase * 0.6f))

    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                Color(0x33E0E0E0),
                Color(0x4DFAFAFA),
                Color.Transparent
            ),
            startY = hazeY1 - 80f,
            endY = hazeY1 + 100f
        ),
        topLeft = Offset(0f, hazeY1 - 80f),
        size = Size(width, 180f)
    )

    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                Color(0x28ECEFF1),
                Color.Transparent
            ),
            startY = hazeY2 - 60f,
            endY = hazeY2 + 80f
        ),
        topLeft = Offset(0f, hazeY2 - 60f),
        size = Size(width, 140f)
    )
}

private fun DrawScope.drawAtmosphericSunrays(
    width: Float,
    height: Float,
    phase: Float,
    pulse: Float,
    timeOfDay: ZenTimeOfDay
) {
    if (timeOfDay == ZenTimeOfDay.NIGHT) return

    val rayColor = if (timeOfDay == ZenTimeOfDay.SUNSET) {
        Color(0x2BFF7043)
    } else {
        Color(0x28FFF59D)
    }

    val sunCenterX = width * 0.78f
    val sunCenterY = height * 0.18f
    val rayRadius = width * 0.95f * pulse

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                rayColor,
                rayColor.copy(alpha = 0.10f),
                Color.Transparent
            ),
            center = Offset(sunCenterX, sunCenterY),
            radius = rayRadius
        ),
        center = Offset(sunCenterX, sunCenterY),
        radius = rayRadius
    )
}

private fun DrawScope.drawAtmosphericClouds(
    width: Float,
    height: Float,
    phase: Float,
    pulse: Float
) {
    // Nubes sutiles adicionales que derivan en capas
    val cloudX = width * (0.50f + 0.12f * cos(phase * 0.7f))
    val cloudY = height * 0.22f

    drawCircle(
        color = Color.White.copy(alpha = 0.12f),
        center = Offset(cloudX, cloudY),
        radius = 70f * pulse
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.15f),
        center = Offset(cloudX + 45f, cloudY - 10f),
        radius = 90f * pulse
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.10f),
        center = Offset(cloudX + 90f, cloudY + 5f),
        radius = 65f * pulse
    )
}

private fun DrawScope.drawNightFireflies(
    width: Float,
    height: Float,
    phase: Float,
    pulse: Float
) {
    val fireflyCount = 14
    for (i in 0 until fireflyCount) {
        val seed = i * 113.1f
        val fx = width * (0.15f + 0.70f * abs(sin(phase * 0.6f + seed)))
        val fy = height * (0.55f + 0.35f * abs(cos(phase * 0.8f + seed * 1.5f)))
        val fGlow = 4.5f + 2.5f * sin(phase * 3f + seed)
        val fAlpha = (0.4f + 0.5f * abs(sin(phase * 2.5f + seed))).coerceIn(0f, 0.95f)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFE2F97D).copy(alpha = fAlpha),
                    Color(0xFF76FF03).copy(alpha = fAlpha * 0.3f),
                    Color.Transparent
                ),
                center = Offset(fx, fy),
                radius = fGlow * 3f
            ),
            center = Offset(fx, fy),
            radius = fGlow * 3f
        )
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

    // Blob 3: Violeta Místico (#7C4DFF)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF7C4DFF).copy(alpha = if (isPlaying) 0.22f else 0.10f),
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
    val barCount = 28
    val barWidth = (width / barCount) * 0.65f
    val spacing = (width / barCount) * 0.35f
    val maxBarHeight = height * 0.32f

    val brush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF00E5FF),
            MonkifyPrimary,
            MonkifyPrimary.copy(alpha = 0.2f)
        )
    )

    for (i in 0 until barCount) {
        val harmonicPhase = phase + (i.toFloat() / barCount) * 6.2831853f
        val wave1 = sin(harmonicPhase)
        val wave2 = cos(harmonicPhase * 2f)
        val combinedWave = ((wave1 + wave2 * 0.5f) / 1.5f).coerceIn(-1f, 1f)
        val normalizedHeight = (abs(combinedWave) * 0.85f + 0.15f) * (if (isPlaying) pulse else 0.6f)
        val currentBarHeight = maxBarHeight * normalizedHeight

        val x = i * (barWidth + spacing) + spacing / 2
        val y = height * 0.65f - (currentBarHeight / 2)

        drawRoundRect(
            brush = brush,
            topLeft = Offset(x, y),
            size = Size(barWidth, currentBarHeight),
            cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
        )
    }
}

private fun DrawScope.drawCosmosBackground(
    width: Float,
    height: Float,
    phase: Float,
    pulse: Float,
    isPlaying: Boolean
) {
    // 1. Fondo estelar base
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF04060C),
                Color(0xFF090D1A),
                Color(0xFF070B16),
                Color(0xFF020408)
            )
        )
    )

    // 2. Galaxia Espiral Inclinada (-26°) con brillo central
    val galaxyX = width * (0.52f + 0.05f * sin(phase * 0.7f))
    val galaxyY = height * (0.36f + 0.03f * cos(phase * 0.5f))
    val coreRadius = width * 0.55f * pulse

    rotate(degrees = -26f, pivot = Offset(galaxyX, galaxyY)) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF80D8FF).copy(alpha = if (isPlaying) 0.40f else 0.22f),
                    Color(0xFF7C4DFF).copy(alpha = if (isPlaying) 0.28f else 0.14f),
                    Color(0xFF536DFE).copy(alpha = if (isPlaying) 0.15f else 0.08f),
                    Color.Transparent
                ),
                center = Offset(galaxyX, galaxyY),
                radius = coreRadius
            ),
            radius = coreRadius,
            center = Offset(galaxyX, galaxyY)
        )

        val arm1Radius = width * 0.75f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFF4081).copy(alpha = if (isPlaying) 0.18f else 0.08f),
                    Color(0xFF7C4DFF).copy(alpha = 0.08f),
                    Color.Transparent
                ),
                center = Offset(galaxyX + width * 0.18f, galaxyY - height * 0.06f),
                radius = arm1Radius
            ),
            radius = arm1Radius,
            center = Offset(galaxyX + width * 0.18f, galaxyY - height * 0.06f)
        )
    }

    // 3. 72 estrellas con picos de difracción
    val starCount = 72
    for (i in 0 until starCount) {
        val seed = i * 137.5f
        val starX = (seed * 11.3f) % width
        val starY = (seed * 7.1f) % height
        val isSpikeStar = i % 9 == 0

        val starPhase = phase * (1.2f + (i % 5) * 0.4f) + (i.toFloat())
        val twinkle = (0.25f + 0.75f * abs(sin(starPhase))).coerceIn(0.1f, 1.0f)

        val starColor = when (i % 4) {
            0 -> Color(0xFFE0F7FA)
            1 -> Color(0xFFFFF9C4)
            2 -> Color(0xFFF8BBD0)
            else -> Color.White
        }

        val baseRadius = if (isSpikeStar) 2.2f else (1.0f + (i % 3) * 0.4f)
        val finalRadius = baseRadius * twinkle

        drawCircle(
            color = starColor.copy(alpha = twinkle * 0.9f),
            center = Offset(starX, starY),
            radius = finalRadius
        )

        if (isSpikeStar && twinkle > 0.45f) {
            val spikeLen = 8.5f * twinkle
            val spikeColor = starColor.copy(alpha = twinkle * 0.40f)
            drawLine(
                color = spikeColor,
                start = Offset(starX - spikeLen, starY),
                end = Offset(starX + spikeLen, starY),
                strokeWidth = 1.0f
            )
            drawLine(
                color = spikeColor,
                start = Offset(starX, starY - spikeLen),
                end = Offset(starX, starY + spikeLen),
                strokeWidth = 1.0f
            )
        }
    }

    // 4. Meteoro continuo suave
    val meteorProgress = abs(sin(phase * 1.5f))
    if (meteorProgress in 0.05f..0.65f) {
        val normalized = (meteorProgress - 0.05f) / 0.60f
        val startX = width * (0.85f - normalized * 0.65f)
        val startY = height * (0.12f + normalized * 0.35f)
        val length = 65f * (1f - normalized * 0.3f)

        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.85f * (1f - normalized)),
                    Color(0xFF80D8FF).copy(alpha = 0.45f * (1f - normalized)),
                    Color.Transparent
                ),
                start = Offset(startX, startY),
                end = Offset(startX + length * 0.8f, startY - length * 0.5f)
            ),
            start = Offset(startX, startY),
            end = Offset(startX + length * 0.8f, startY - length * 0.5f),
            strokeWidth = 1.8f
        )
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
    // Gradiente de cielo según Day/Night cycle
    val skyColors = when (timeOfDay) {
        ZenTimeOfDay.DAWN -> listOf(
            Color(0xFFF8BBD0), // Rosa suave del amanecer
            Color(0xFFFFE0B2), // Ámbar dorado matutino
            Color(0xFFE0F7FA)  // Cyan fresco en el horizonte
        )
        ZenTimeOfDay.DAY -> listOf(
            Color(0xFF4FC3F7), // Azul cielo vibrante
            Color(0xFF81D4FA), // Celeste luminoso
            Color(0xFFE1F5FE)  // Blanco suave en el horizonte
        )
        ZenTimeOfDay.SUNSET -> listOf(
            Color(0xFF311B92), // Violeta crepuscular superior
            Color(0xFFE65100), // Carmesí anaranjado
            Color(0xFFFFB74D)  // Dorado cálido en el horizonte
        )
        ZenTimeOfDay.NIGHT -> listOf(
            Color(0xFF0A1128), // Azul noche profundo
            Color(0xFF101F42), // Azul zafiro nocturno
            Color(0xFF1C2D5A)  // Horizonte oscuro suave
        )
    }

    drawRect(brush = Brush.verticalGradient(colors = skyColors))

    // Astro celestial (Sol / Luna) con pulso suave
    val celestialY = height * when (timeOfDay) {
        ZenTimeOfDay.DAWN -> 0.32f
        ZenTimeOfDay.DAY -> 0.18f
        ZenTimeOfDay.SUNSET -> 0.38f
        ZenTimeOfDay.NIGHT -> 0.22f
    }
    val celestialX = width * when (timeOfDay) {
        ZenTimeOfDay.DAWN -> 0.30f
        ZenTimeOfDay.DAY -> 0.75f
        ZenTimeOfDay.SUNSET -> 0.70f
        ZenTimeOfDay.NIGHT -> 0.80f
    }
    val celestialRadius = width * 0.12f * pulse

    val celestialColors = when (timeOfDay) {
        ZenTimeOfDay.DAWN -> listOf(Color(0xFFFFCC80), Color(0xFFFFE082), Color.Transparent)
        ZenTimeOfDay.DAY -> listOf(Color(0xFFFFF59D), Color(0xFFFFF176), Color.Transparent)
        ZenTimeOfDay.SUNSET -> listOf(Color(0xFFFF7043), Color(0xFFFF8A65), Color.Transparent)
        ZenTimeOfDay.NIGHT -> listOf(Color(0xFFE0F7FA), Color(0xFFB2EBF2), Color.Transparent)
    }

    drawCircle(
        brush = Brush.radialGradient(
            colors = celestialColors,
            center = Offset(celestialX, celestialY),
            radius = celestialRadius * 1.5f
        ),
        radius = celestialRadius * 1.5f,
        center = Offset(celestialX, celestialY)
    )

    // Colinas y estanque
    val hillColor = when (timeOfDay) {
        ZenTimeOfDay.DAWN -> Color(0xFF81C784)
        ZenTimeOfDay.DAY -> Color(0xFF66BB6A)
        ZenTimeOfDay.SUNSET -> Color(0xFF5D4037)
        ZenTimeOfDay.NIGHT -> Color(0xFF1B3B2B)
    }
    val pondColor = when (timeOfDay) {
        ZenTimeOfDay.DAWN -> Color(0xFF4DD0E1)
        ZenTimeOfDay.DAY -> Color(0xFF29B6F6)
        ZenTimeOfDay.SUNSET -> Color(0xFF7E57C2)
        ZenTimeOfDay.NIGHT -> Color(0xFF0D253A)
    }

    drawCircle(
        color = hillColor,
        radius = width * 0.85f,
        center = Offset(width * 0.25f, height * 0.82f)
    )
    drawCircle(
        color = hillColor.copy(alpha = 0.9f),
        radius = width * 0.95f,
        center = Offset(width * 0.80f, height * 0.86f)
    )

    // Estanque
    drawOval(
        color = pondColor,
        topLeft = Offset(width * 0.05f, height * 0.76f),
        size = Size(width * 0.90f, height * 0.22f)
    )

    // Nenúfar
    val lilyPadX = width * 0.50f
    val lilyPadY = height * 0.83f
    drawOval(
        color = Color(0xFF2E7D32),
        topLeft = Offset(lilyPadX - 65f, lilyPadY - 22f),
        size = Size(130f, 44f)
    )

    // Froggy Zen en el centro
    val froggyX = lilyPadX
    val froggyY = lilyPadY - 25f
    val froggyBreath = (pulse - 1.0f) * 6f

    // Cuerpo
    drawCircle(
        color = Color(0xFF4CAF50),
        radius = 26f + froggyBreath,
        center = Offset(froggyX, froggyY)
    )
    // Panza
    drawCircle(
        color = Color(0xFFC8E6C9),
        radius = 16f + froggyBreath * 0.7f,
        center = Offset(froggyX, froggyY + 4f)
    )
    // Ojos
    drawCircle(
        color = Color(0xFF4CAF50),
        radius = 11f,
        center = Offset(froggyX - 16f, froggyY - 20f)
    )
    drawCircle(
        color = Color(0xFF4CAF50),
        radius = 11f,
        center = Offset(froggyX + 16f, froggyY - 20f)
    )
    drawCircle(
        color = Color.Black,
        radius = 5.5f,
        center = Offset(froggyX - 16f, froggyY - 20f)
    )
    drawCircle(
        color = Color.Black,
        radius = 5.5f,
        center = Offset(froggyX + 16f, froggyY - 20f)
    )
    // Destello en ojos
    drawCircle(
        color = Color.White,
        radius = 2.0f,
        center = Offset(froggyX - 18f, froggyY - 22f)
    )
    drawCircle(
        color = Color.White,
        radius = 2.0f,
        center = Offset(froggyX + 14f, froggyY - 22f)
    )
    // Mejillas sonrojadas
    drawCircle(
        color = Color(0xFFFF8A80).copy(alpha = 0.65f),
        radius = 5f,
        center = Offset(froggyX - 18f, froggyY - 5f)
    )
    drawCircle(
        color = Color(0xFFFF8A80).copy(alpha = 0.65f),
        radius = 5f,
        center = Offset(froggyX + 18f, froggyY - 5f)
    )
}

// ==========================================
// PREVIEWS COMPOSE PARA VALIDACIÓN VISUAL
// ==========================================

@Preview(name = "Froggy Weather - Sunny", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun ZenFroggySunnyPreview() {
    ZenBackground(
        isPlaying = true,
        engine = "FROGGY",
        timeOfDay = ZenTimeOfDay.DAY,
        weatherCondition = WeatherCondition.SUNNY,
        scenery = FroggyScenery.FIELDS
    )
}

@Preview(name = "Froggy Weather - Rainy", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun ZenFroggyRainyPreview() {
    ZenBackground(
        isPlaying = true,
        engine = "FROGGY",
        timeOfDay = ZenTimeOfDay.DAY,
        weatherCondition = WeatherCondition.RAINY,
        scenery = FroggyScenery.FIELDS
    )
}

@Preview(name = "Froggy Weather - Snowy Night", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun ZenFroggySnowyNightPreview() {
    ZenBackground(
        isPlaying = true,
        engine = "FROGGY",
        timeOfDay = ZenTimeOfDay.NIGHT,
        weatherCondition = WeatherCondition.SNOWY,
        scenery = FroggyScenery.HILL
    )
}

@Preview(name = "Froggy Weather - Hazy Sunset", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun ZenFroggyHazySunsetPreview() {
    ZenBackground(
        isPlaying = true,
        engine = "FROGGY",
        timeOfDay = ZenTimeOfDay.SUNSET,
        weatherCondition = WeatherCondition.HAZY,
        scenery = FroggyScenery.MUSHROOM
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
