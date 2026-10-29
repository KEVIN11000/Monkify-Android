package com.k11000.monkify.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.k11000.monkify.ui.theme.MonkifyBackground
import com.k11000.monkify.ui.theme.MonkifyDarkGray
import com.k11000.monkify.ui.theme.MonkifyGray
import com.k11000.monkify.ui.theme.MyApplicationTheme

/**
 * Modificador Compose de Shimmer animado nativo para esqueletos (Skeletons).
 * Proporciona un barrido de luz metálico suave que comunica inmediatamente al usuario
 * qué elementos se están cargando y dónde van a ubicarse, evitando pantallas en blanco.
 */
fun Modifier.shimmerPlaceholder(
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(8.dp)
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val shimmerColors = listOf(
        MonkifyDarkGray.copy(alpha = 0.55f),
        MonkifyGray.copy(alpha = 0.30f),
        MonkifyDarkGray.copy(alpha = 0.55f)
    )

    this
        .clip(shape)
        .background(
            brush = Brush.linearGradient(
                colors = shimmerColors,
                start = Offset(x = translateAnim - 600f, y = translateAnim - 600f),
                end = Offset(x = translateAnim, y = translateAnim)
            ),
            shape = shape
        )
}

/**
 * Esqueleto que replica la estructura exacta de un elemento de la cuadrícula QuickPlay (Home).
 */
@Composable
fun SkeletonQuickPlayItem(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MonkifyDarkGray.copy(alpha = 0.45f))
            .padding(end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Carátula cuadrada
        Box(
            modifier = Modifier
                .size(56.dp)
                .shimmerPlaceholder(shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Línea de título
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(13.dp)
                    .shimmerPlaceholder(shape = RoundedCornerShape(4.dp))
            )
            // Línea de artista
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(10.dp)
                    .shimmerPlaceholder(shape = RoundedCornerShape(3.dp))
            )
        }
    }
}

/**
 * Esqueleto que replica las tarjetas del carrusel de álbumes / novedades (Home).
 */
@Composable
fun SkeletonAlbumCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.width(140.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .shimmerPlaceholder(shape = RoundedCornerShape(12.dp))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(14.dp)
                .shimmerPlaceholder(shape = RoundedCornerShape(4.dp))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(11.dp)
                .shimmerPlaceholder(shape = RoundedCornerShape(3.dp))
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun ShimmerSkeletonPreview() {
    MyApplicationTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MonkifyBackground)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SkeletonQuickPlayItem(modifier = Modifier.weight(1f))
                SkeletonQuickPlayItem(modifier = Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SkeletonAlbumCard()
                SkeletonAlbumCard()
            }
        }
    }
}
