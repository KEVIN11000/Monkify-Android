package com.k11000.monkify.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.min

/**
 * Representa el estado atómico del vinilo en reproducción.
 */
data class VinylDiscState(
    val trackKey: String,
    val coverUrl: String?
)

/**
 * Componente que representa un disco de vinilo girando sobre el plato de una vitrola,
 * con física de inercia, animación cinemática de cambio de disco (deslizamiento físico
 * del disco saliente hacia la izquierda y entrada del nuevo disco por la derecha) y
 * el fondo reactivo Zen translúcido incrustado en los surcos del acetato.
 */
@Composable
fun VinylTurntable(
    coverUrl: String?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    trackKey: String = coverUrl ?: ""
) {
    // Físicas de rotación continua y desaceleración con inercia al pausar
    val rotationAnim = remember { Animatable(0f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            // Rotación continua a velocidad 33 RPM (~1800ms por vuelta)
            while (isActive) {
                rotationAnim.animateTo(
                    targetValue = rotationAnim.value + 360f,
                    animationSpec = tween(durationMillis = 1800, easing = LinearEasing)
                )
            }
        } else {
            // Desaceleración suave con inercia mecánica al pausar
            rotationAnim.animateTo(
                targetValue = rotationAnim.value + 50f,
                animationSpec = tween(durationMillis = 850, easing = LinearOutSlowInEasing)
            )
        }
    }

    // Estado para levantar el brazo fonocaptor mientras se produce el cambio físico de disco
    var isSwappingDisc by remember { mutableStateOf(false) }

    LaunchedEffect(trackKey) {
        if (trackKey.isNotBlank()) {
            isSwappingDisc = true
            // Espera el tiempo de la animación de deslizamiento antes de posar de nuevo la aguja
            delay(580)
            isSwappingDisc = false
        }
    }

    // Ángulo del brazo fonocaptor (tonearm): ~26° sobre el disco al reproducir, ~8° en reposo/cambio
    val tonearmAngle by animateFloatAsState(
        targetValue = if (isPlaying && !isSwappingDisc) 26f else 8f,
        animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
        label = "TonearmAngle"
    )

    val currentDisc = remember(trackKey, coverUrl) {
        VinylDiscState(trackKey = trackKey, coverUrl = coverUrl)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        // 1. Plato y chasis base de la vitrola
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = min(size.width, size.height) / 2f
            val platterRadius = maxRadius * 0.95f

            // Sombra ambiental del plato
            drawCircle(
                color = Color.Black.copy(alpha = 0.55f),
                radius = platterRadius + 6f,
                center = center
            )

            // Anillo exterior de aluminio/metal cepillado del plato
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF38383A),
                        Color(0xFF242426),
                        Color(0xFF4A4A4D),
                        Color(0xFF1E1E20)
                    ),
                    center = center,
                    radius = platterRadius
                ),
                radius = platterRadius,
                center = center
            )

            // Borde metálico estriado de precisión
            drawCircle(
                color = Color(0xFF6B6B70),
                radius = platterRadius,
                center = center,
                style = Stroke(width = 2.5f)
            )

            // Cama interior de goma antideslizante (slipmat)
            drawCircle(
                color = Color(0xFF161618),
                radius = platterRadius * 0.94f,
                center = center
            )
        }

        // 2. Disco de Vinilo Giratorio con transición física:
        // El disco saliente se desliza completamente hacia la izquierda y el nuevo ingresa desde la derecha
        AnimatedContent(
            targetState = currentDisc,
            transitionSpec = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> (fullWidth * 1.15f).toInt() },
                    animationSpec = tween(durationMillis = 560, easing = FastOutSlowInEasing)
                ).togetherWith(
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> (-fullWidth * 1.15f).toInt() },
                        animationSpec = tween(durationMillis = 560, easing = FastOutSlowInEasing)
                    )
                )
            },
            label = "VinylDiscSwap"
        ) { disc ->
            Box(
                modifier = Modifier
                    .fillMaxSize(0.88f)
                    .graphicsLayer {
                        rotationZ = rotationAnim.value % 360f
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val vinylRadius = min(size.width, size.height) / 2f

                    // Cuerpo de acetato con tinte translúcido:
                    // Permite que el fondo ambiental/Zen (Aurora u Ondas) resplandezca a través de los surcos
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xCC0D0D10),
                                Color(0xDD121216),
                                Color(0xEE09090B),
                                Color(0xF5040405)
                            ),
                            center = center,
                            radius = vinylRadius
                        ),
                        radius = vinylRadius,
                        center = center
                    )

                    // Borde exterior elevado del vinilo
                    drawCircle(
                        color = Color(0xFF28282D),
                        radius = vinylRadius,
                        center = center,
                        style = Stroke(width = 3.5f)
                    )

                    // Surcos concéntricos realistas de audio
                    val grooveStart = vinylRadius * 0.44f
                    val grooveEnd = vinylRadius * 0.96f
                    val numGrooves = 16
                    val step = (grooveEnd - grooveStart) / numGrooves

                    for (i in 0..numGrooves) {
                        val r = grooveStart + i * step
                        val alpha = if (i % 3 == 0) 0.28f else 0.14f
                        drawCircle(
                            color = Color.White.copy(alpha = alpha),
                            radius = r,
                            center = center,
                            style = Stroke(width = if (i % 4 == 0) 1.5f else 0.8f)
                        )
                    }

                    // Reflejos dinámicos de luz en cuña angular (especular de acetato)
                    rotate(degrees = 35f, pivot = center) {
                        drawOval(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.White.copy(alpha = 0.08f), Color.Transparent),
                                center = center,
                                radius = vinylRadius
                            ),
                            topLeft = Offset(center.x - vinylRadius * 0.25f, center.y - vinylRadius),
                            size = androidx.compose.ui.geometry.Size(vinylRadius * 0.5f, vinylRadius * 2f)
                        )
                    }
                    rotate(degrees = 125f, pivot = center) {
                        drawOval(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent),
                                center = center,
                                radius = vinylRadius
                            ),
                            topLeft = Offset(center.x - vinylRadius * 0.2f, center.y - vinylRadius),
                            size = androidx.compose.ui.geometry.Size(vinylRadius * 0.4f, vinylRadius * 2f)
                        )
                    }
                }

                // 3. Etiqueta central (Center Label) con la carátula circular
                Box(
                    modifier = Modifier
                        .fillMaxSize(0.38f)
                        .clip(CircleShape)
                        .shadow(6.dp, CircleShape)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    TrackArtwork(
                        coverUrl = disc.coverUrl,
                        contentDescription = "Carátula en disco de vinilo",
                        shape = CircleShape,
                        iconSize = 36.dp,
                        isAlbum = true,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Anillo de protección y orificio central (spindle)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        // Borde de la etiqueta
                        drawCircle(
                            color = Color(0xAAFFFFFF),
                            radius = size.width / 2f,
                            center = center,
                            style = Stroke(width = 2f)
                        )
                        // Orificio central
                        drawCircle(
                            color = Color(0xFF1E1E22),
                            radius = 12f,
                            center = center
                        )
                        drawCircle(
                            color = Color(0xFFB5B5BA),
                            radius = 12f,
                            center = center,
                            style = Stroke(width = 2f)
                        )
                    }
                }
            }
        }

        // 4. Brazo Fonocaptor (Tonearm) de la Vitrola
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val pivot = Offset(size.width * 0.90f, size.height * 0.12f)
            val armLength = size.width * 0.52f

            rotate(degrees = tonearmAngle, pivot = pivot) {
                // Base del pivote metálico del brazo
                drawCircle(
                    color = Color.Black.copy(alpha = 0.4f),
                    radius = 24f,
                    center = pivot + Offset(2f, 4f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF71717A), Color(0xFF27272A)),
                        center = pivot,
                        radius = 22f
                    ),
                    radius = 22f,
                    center = pivot
                )
                drawCircle(
                    color = Color(0xFFA1A1AA),
                    radius = 22f,
                    center = pivot,
                    style = Stroke(width = 2f)
                )

                // Contrapeso trasero cilíndrico
                val counterWeightStart = pivot - Offset(0f, 26f)
                drawLine(
                    color = Color(0xFF3F3F46),
                    start = pivot,
                    end = counterWeightStart,
                    strokeWidth = 14f,
                    cap = StrokeCap.Round
                )

                // Brazo cromado curvado hacia el disco
                val armPath = Path().apply {
                    moveTo(pivot.x, pivot.y)
                    val mid = Offset(pivot.x - armLength * 0.45f, pivot.y + armLength * 0.45f)
                    val end = Offset(pivot.x - armLength * 0.72f, pivot.y + armLength * 0.88f)
                    quadraticTo(mid.x, mid.y, end.x, end.y)
                }

                // Sombra del brazo
                drawPath(
                    path = armPath,
                    color = Color.Black.copy(alpha = 0.35f),
                    style = Stroke(width = 7f, cap = StrokeCap.Round)
                )

                // Barra metálica plateada
                drawPath(
                    path = armPath,
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFE4E4E7), Color(0xFFA1A1AA), Color(0xFFD4D4D8))
                    ),
                    style = Stroke(width = 5.5f, cap = StrokeCap.Round)
                )

                // Portacápsula / Headshell con aguja al final del brazo
                val headStart = Offset(pivot.x - armLength * 0.72f, pivot.y + armLength * 0.88f)
                val headEnd = headStart + Offset(-16f, 18f)
                drawLine(
                    color = Color(0xFF18181B),
                    start = headStart,
                    end = headEnd,
                    strokeWidth = 10f,
                    cap = StrokeCap.Square
                )

                // Cartucho dorado / aguja fonocaptora
                drawCircle(
                    color = Color(0xFFFFD700),
                    radius = 4f,
                    center = headEnd
                )
            }
        }
    }
}
