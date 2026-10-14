package com.k11000.monkify.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.k11000.monkify.domain.model.AppUpdate
import com.k11000.monkify.ui.theme.MonkifyBackground
import com.k11000.monkify.ui.theme.MonkifyGray
import com.k11000.monkify.ui.theme.MonkifyOnBackground
import com.k11000.monkify.ui.theme.MonkifyPrimary
import com.k11000.monkify.ui.theme.MonkifySurface

@Composable
fun UpdateDialog(
    update: AppUpdate,
    onConfirmUpdate: (AppUpdate) -> Unit,
    onDismiss: () -> Unit
) {
    val sizeMb = if (update.apkSize > 0) {
        String.format("%.1f MB", update.apkSize / (1024.0 * 1024.0))
    } else {
        "Disponible"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MonkifySurface,
        shape = RoundedCornerShape(16.dp),
        icon = {
            Icon(
                imageVector = Icons.Default.SystemUpdate,
                contentDescription = null,
                tint = MonkifyPrimary,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Nueva actualización disponible",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MonkifyOnBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${update.currentVersionName} ➔ ${update.latestVersionName} ($sizeMb)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MonkifyPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MonkifyBackground.copy(alpha = 0.6f))
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Novedades y notas de la versión:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MonkifyOnBackground
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = update.releaseNotes.ifBlank { "Mejoras de rendimiento y corrección de errores." },
                    style = MaterialTheme.typography.bodySmall,
                    color = MonkifyGray
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmUpdate(update) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MonkifyPrimary,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Actualizar ahora", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = MonkifyGray)
            ) {
                Text("Más tarde")
            }
        }
    )
}
