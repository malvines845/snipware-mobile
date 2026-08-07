package com.snipware.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.snipware.app.ui.theme.SnipAccent
import com.snipware.app.ui.theme.SnipBorder
import com.snipware.app.ui.theme.SnipSurface2
import com.snipware.app.ui.theme.SnipText

/**
 * "Smart Copy" dialog: prompts for a value per {{placeholder}} found in the
 * snippet, then hands the resolved text back via [onConfirm]. Mirrors the
 * placeholder-fill flow driven by extractPH()/resolvePH() in the original app.
 */
@Composable
fun PlaceholderFillDialog(
    placeholderNames: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, String>) -> Unit
) {
    val values = remember { mutableStateMapOf<String, String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Fill placeholders") },
        text = {
            Column {
                placeholderNames.forEach { name ->
                    OutlinedTextField(
                        value = values[name] ?: "",
                        onValueChange = { values[name] = it },
                        label = { Text(name) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SnipSurface2,
                            unfocusedContainerColor = SnipSurface2,
                            focusedBorderColor = SnipAccent,
                            unfocusedBorderColor = SnipBorder,
                            focusedTextColor = SnipText,
                            unfocusedTextColor = SnipText
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(values.toMap()) }) {
                Text("Copy", color = SnipAccent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
