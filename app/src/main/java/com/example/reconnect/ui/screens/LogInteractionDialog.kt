package com.example.reconnect.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue                // ← added: needed for 'by' delegation
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue                // ← added: needed for 'by' delegation
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LogInteractionDialog(
    onDismiss: () -> Unit,
    onConfirm: (notes: String) -> Unit
) {
    var notesField by remember { mutableStateOf("") }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull() ?: ""
            notesField = spokenText
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Interaction") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Add an optional note about this interaction.",
                    style = MaterialTheme.typography.bodySmall
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),             // ← removed .Companion
                    verticalAlignment = Alignment.CenterVertically  // ← removed .Companion
                ) {
                    OutlinedTextField(
                        value = notesField,
                        onValueChange = { notesField = it },
                        label = { Text("Notes (optional)") },
                        modifier = Modifier.weight(1f),             // ← removed .Companion
                        minLines = 2
                    )
                    Spacer(Modifier.width(8.dp))                    // ← removed .Companion

                    IconButton(
                        onClick = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(
                                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                )
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Say your note…")
                            }
                            speechLauncher.launch(intent)
                        }
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = "Dictate note")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(notesField.trim()) }) { Text("Log") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}