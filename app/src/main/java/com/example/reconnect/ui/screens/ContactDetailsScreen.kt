package com.example.reconnect.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.reconnect.data.local.ContactEntity
import com.example.reconnect.data.local.InteractionEntity
import com.example.reconnect.data.model.InteractionType
import com.example.reconnect.data.model.RelationshipType
import com.example.reconnect.ui.components.ContactAvatar
import com.example.reconnect.ui.viewmodel.ContactDetailViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailsScreen(
    viewModel: ContactDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToContactSettings: () -> Unit = {},
    promptInteractionId: Long? = null,
    onPromptConsumed: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    // Controls whether the "Log Interaction" dialog is visible
    var showLogDialog by remember { mutableStateOf(false) }

    // Auto-opens when we arrived here via a reminder/follow-up notification for an
    // unanswered "how did it go?" prompt
    var activeFollowUpId by remember { mutableStateOf(promptInteractionId) }
    LaunchedEffect(promptInteractionId) {
        activeFollowUpId = promptInteractionId
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.contact?.name ?: "Contact") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToContactSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Contact settings")
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

    // ── Follow-up prompt: "how was your call/text? want to write it down?"
    activeFollowUpId?.let { interactionId ->
        FollowUpDialog(
            contactName = uiState.contact?.name ?: "them",
            onSave = { notes ->
                viewModel.answerFollowUp(interactionId, notes)
                activeFollowUpId = null
                onPromptConsumed()
            },
            onSkip = {
                viewModel.dismissFollowUp(interactionId)
                activeFollowUpId = null
                onPromptConsumed()
            }
        )
    }
}

@Composable
fun FollowUpDialog(
    contactName: String,
    onSave: (notes: String) -> Unit,
    onSkip: () -> Unit
) {
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onSkip,
        title = { Text("How was your call/text with $contactName?") },
        text = {
            Column {
                Text(
                    "What did you talk about? Want to write it down?",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(notes.trim()) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onSkip) { Text("Skip") }
        }
    )
}

//
// ── Contact Header ────────────────────────────────────────────────────────────
@Composable
fun ContactHeader(
    contact: ContactEntity,
    lastContactedAt: Long?,
    onContactUpdated: (ContactEntity) -> Unit
) {
    var nameField by remember(contact.id) { mutableStateOf(contact.name) }

    // Pre-populate from whatever is already stored on the contact
    // remember(contact.id) resets if you navigate to a different contact
    var selectedRelationshipKey by remember(contact.id) {
        mutableStateOf(contact.relationshipLabel)
    }
    var showRelationshipMenu by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Spacer(Modifier.height(8.dp))

        // ── Photo + relationship icon — tap the badge to change relationship type
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                ContactAvatar(photoUri = contact.photoUri, size = 64.dp)

                // Relationship badge — overlaps the bottom-right corner of the photo
                Box(modifier = Modifier.align(Alignment.BottomEnd)) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier
                            .size(28.dp)
                            .clickable { showRelationshipMenu = true }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = RelationshipType.fromKey(selectedRelationshipKey)?.emoji ?: "❔",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = showRelationshipMenu,
                        onDismissRequest = { showRelationshipMenu = false }
                    ) {
                        RelationshipType.all.forEach { type ->
                            DropdownMenuItem(
                                text = { Text("${type.emoji} ${type.label}") },
                                onClick = {
                                    selectedRelationshipKey = type.key
                                    showRelationshipMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            Column {
                Text(
                    text = RelationshipType.fromKey(selectedRelationshipKey)?.label ?: "Tap to set relationship",
                    style = MaterialTheme.typography.titleMedium
                )
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
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        OutlinedTextField(
            value = nameField,
            onValueChange = { nameField = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                onContactUpdated(
                    contact.copy(
                        name = nameField,
                        relationshipLabel = selectedRelationshipKey  // ← save the key
                    )
                )
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

    val typeDisplay = InteractionType.fromKey(interaction.type)
        ?.let { "${it.emoji} ${it.label}" }
        ?: interaction.type

    // Show call count if this is an auto-detected entry with multiple calls
    val countSuffix = if (interaction.source == "auto_detected" && interaction.callCount > 1) {
        " · ${interaction.callCount} calls"
    } else ""

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Text(
            text = "$dateString · $typeDisplay$countSuffix",  // ← appends "· 3 calls" when relevant
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
