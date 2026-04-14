package com.example.reconnect.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.reconnect.data.model.ContactUiModel
import com.example.reconnect.ui.viewmodel.ContactsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(viewModel: ContactsViewModel) {

    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("RE:Connect") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Contact")
            }
        }
    ) { paddingValues ->

        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)) {

            when {
                // ── Loading state
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                // ── Empty state
                uiState.contacts.isEmpty() -> {
                    EmptyContactsMessage(modifier = Modifier.align(Alignment.Center))
                }

                // ── Contacts list
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(uiState.contacts, key = { it.id }) { contact ->
                            ContactCard(
                                contact = contact,
                                onQuickLog = { viewModel.quickLogContact(contact.id) },
                                onDelete = { viewModel.deleteContact(contact.id) }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    // ── Add Contact Dialog
    if (showAddDialog) {
        AddContactDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name: String, phone: String?, label: String, days: Int ->
                viewModel.addContact(name, phone, label, days)
                showAddDialog = false
            }
        )
    }
}





// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ContactCard(
    contact: ContactUiModel,
    onQuickLog: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar placeholder
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .padding(end = 12.dp)
        )

        // Name + label
        Column(modifier = Modifier.weight(1f)) {
            Text(text = contact.name, fontWeight = FontWeight.SemiBold)
            if (contact.relationshipLabel.isNotBlank()) {
                Text(
                    text = contact.relationshipLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            contact.lastContactedAt?.let { timestamp ->
                val daysAgo = ((System.currentTimeMillis() - timestamp) / 86400000).toInt()
                Text(
                    text = if (daysAgo == 0) "Contacted today"
                    else "Last contact: $daysAgo day(s) ago",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Quick log button
            TextButton(onClick = onQuickLog) {
                Text("Logged")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────

    @Composable
    fun EmptyContactsMessage(modifier: Modifier = Modifier) {
        Column(
            modifier = modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No contacts yet",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tap + to add someone you want to stay in touch with",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    @Composable
    fun AddContactDialog(
        onDismiss: () -> Unit,
        onConfirm: (name: String, phone: String?, label: String, days: Int) -> Unit
    ) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var label by remember { mutableStateOf("") }
        var reminderDays by remember { mutableStateOf("14") }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Add Contact") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text("Label (e.g. Friend, Family)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = reminderDays,
                        onValueChange = { reminderDays = it.filter { c -> c.isDigit() } },
                        label = { Text("Remind every X days") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (name.isNotBlank()) {
                            onConfirm(
                                name.trim(),
                                phone.trim().ifBlank { null },
                                label.trim(),
                                reminderDays.toIntOrNull() ?: 14
                            )
                        }
                    }
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        )
    }
