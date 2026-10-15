package com.k11000.monkify.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.k11000.monkify.ui.theme.MonkifyGray
import com.k11000.monkify.ui.theme.MonkifyOnBackground
import com.k11000.monkify.ui.theme.MonkifyPrimary
import com.k11000.monkify.ui.theme.MonkifySurface

@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String) -> Unit
) {
    var playlistName by remember { mutableStateOf("") }
    val isValid = playlistName.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MonkifySurface,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = "Nueva Playlist",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MonkifyOnBackground
            )
        },
        text = {
            Column {
                Text(
                    text = "Asigna un nombre a tu lista de reproducción:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MonkifyGray
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { if (it.length <= 100) playlistName = it },
                    placeholder = { Text("Ej. Favoritas del Invierno", color = MonkifyGray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MonkifyOnBackground,
                        unfocusedTextColor = MonkifyOnBackground,
                        focusedBorderColor = MonkifyPrimary,
                        unfocusedBorderColor = MonkifyGray,
                        cursorColor = MonkifyPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid) {
                        onConfirm(playlistName.trim())
                    }
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MonkifyPrimary,
                    contentColor = Color.Black,
                    disabledContainerColor = MonkifyGray.copy(alpha = 0.3f),
                    disabledContentColor = MonkifyGray
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Crear", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = MonkifyGray)
            }
        }
    )
}
