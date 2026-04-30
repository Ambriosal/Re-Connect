package com.example.reconnect.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.reconnect.data.model.InteractionType

@Composable
fun LogInteractionDialog(
    onDismiss: () -> Unit,
    onConfirm: (type: InteractionType, notes: String) -> Unit  // ← now passes type too
) {
    var notesField by remember { mutableStateOf("") }

    // No type selected until user picks one — null forces an explicit choice
    var selectedType by remember { mutableStateOf<InteractionType?>(null) }

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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // ── Type selector ─────────────────────────────────────────
                Text("How did you connect?", style = MaterialTheme.typography.bodySmall)

                // LazyVerticalGrid lays the chips in a 2-column grid automatically
                // fixedSize(2) means exactly 2 columns regardless of screen width
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)  // cap height so dialog doesn't overflow
                ) {
                    items(InteractionType.all) { type ->
                        // FilterChip is a Material3 component — selected state changes its appearance
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text("${type.emoji} ${type.label}") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // ── Notes + mic ───────────────────────────────────────────
                Text("Add a note (optional)", style = MaterialTheme.typography.bodySmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = notesField,
                        onValueChange = { notesField = it },
                        label = { Text("Notes (optional)") },
                        modifier = Modifier.weight(1f),
                        minLines = 2
                    )
                    Spacer(Modifier.width(8.dp))
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
            TextButton(
                // Disabled until a type is selected — selectedType!! is safe here
                // because the button is only enabled when selectedType is non-null
                onClick = { onConfirm(selectedType!!, notesField.trim()) },
                enabled = selectedType != null
            ) {
                Text("Log")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}