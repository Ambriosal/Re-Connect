package com.example.reconnect.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.reconnect.data.local.ContactEntity
import com.example.reconnect.data.local.InteractionEntity
import com.example.reconnect.data.model.InteractionType
import com.example.reconnect.ui.viewmodel.ContactDetailViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailsScreen(
    viewModel: ContactDetailViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Controls whether the "Log Interaction" dialog is visible
    var showLogDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.contact?.name ?: "Contact") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize().padding(paddingValues)) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
            }
            uiState.contact == null -> {
                Text("Contact not found", modifier = Modifier.padding(paddingValues))
            }
            else -> {
                // LazyColumn lets the whole screen scroll as one unit —
                // header + history list together, not nested scrollers
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    // ── Header section
                    item {
                        ContactHeader(
                            contact = uiState.contact!!,
                            lastContactedAt = uiState.lastContactedAt,
                            onContactUpdated = { viewModel.updateContact(it) }
                        )
                    }

                    // ── Log button
                    item {
                        Button(
                            onClick = { showLogDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("+ Log Interaction")
                        }
                    }

                    // ── Section title for history
                    item {
                        Text(
                            text = "Interaction History",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // ── History entries — 'items' maps each InteractionEntity to a row
                    if (uiState.interactions.isEmpty()) {
                        item {
                            Text(
                                text = "No interactions logged yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(uiState.interactions, key = { it.id }) { interaction ->
                            InteractionRow(interaction = interaction)
                            HorizontalDivider()
                        }
                    }

                    // Bottom breathing room
                    item { Spacer(Modifier.height(32.dp)) }
                }
            }
        }
    }

    // ── Dialog rendered outside the Scaffold — overlays on top
    if (showLogDialog) {
        LogInteractionDialog(
            onDismiss = { showLogDialog = false },
            onConfirm = { type, notes ->           // ← destructure both parameters
                viewModel.logInteraction(
                    type = type.key,               // pass the stable key, not the label
                    notes = notes
                )
                showLogDialog = false
            }
        )
    }
}

//
// ── Contact Header ────────────────────────────────────────────────────────────
@Composable
fun ContactHeader(
    contact: ContactEntity,
    lastContactedAt: Long?,                  // ← new parameter
    onContactUpdated: (ContactEntity) -> Unit
) {
    var nameField by remember(contact.id) { mutableStateOf(contact.name) }
    var labelField by remember(contact.id) { mutableStateOf(contact.relationshipLabel) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Spacer(Modifier.height(8.dp))

        // ── Last contacted display ─────────────────────────────────────────
        // Show before the editable fields — it's read info, not edit info
        val lastContactedText = lastContactedAt?.let { timestamp ->
            val daysAgo = ((System.currentTimeMillis() - timestamp) / 86_400_000).toInt()
            when (daysAgo) {
                0 -> "Last contacted: today"
                1 -> "Last contacted: yesterday"
                else -> "Last contacted: $daysAgo days ago"
            }
        } ?: "No interactions logged yet"

        Text(
            text = lastContactedText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // ── Editable fields (unchanged)
        OutlinedTextField(
            value = nameField,
            onValueChange = { nameField = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = labelField,
            onValueChange = { labelField = it },
            label = { Text("Relationship") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                onContactUpdated(contact.copy(name = nameField, relationshipLabel = labelField))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Changes")
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
    }
}

// ── Single history row ────────────────────────────────────────────────────────
@Composable
fun InteractionRow(interaction: InteractionEntity) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val dateString = dateFormatter.format(Date(interaction.occurredAt))

    // Map the stored key back to a display label
    // Falls back to the raw key for old "manual" entries so nothing breaks
    val typeDisplay = InteractionType.fromKey(interaction.type)
        ?.let { "${it.emoji} ${it.label}" }
        ?: interaction.type                          // ← graceful fallback

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Text(
            text = "$dateString · $typeDisplay",     // ← was "via ${interaction.type}"
            style = MaterialTheme.typography.bodyMedium
        )
        val notes = interaction.notes.orEmpty()
        if (notes.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = notes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
