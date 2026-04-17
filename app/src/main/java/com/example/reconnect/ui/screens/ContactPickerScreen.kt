package com.example.reconnect.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.reconnect.data.model.ContactUiModel
import com.example.reconnect.util.ImportedContact
import com.example.reconnect.ui.viewmodel.ContactsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactPickerScreen(
    viewModel: ContactsViewModel,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // ── Track which contacts are selected by their nativeId
    val selected = remember { mutableStateListOf<String>() }

    // ── Filter out contacts already in RE:Connect
    val existingNativeIds = uiState.contacts
        .mapNotNull { it.nativeContactId }
        .toSet()

    val availableContacts = uiState.phoneContacts
        .filter { it.nativeId !in existingNativeIds }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (selected.isEmpty()) "Import Contacts"
                        else "${selected.size} selected"
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            // Import only selected contacts
                            val toImport = uiState.phoneContacts
                                .filter { it.nativeId in selected }
                            viewModel.importMultipleContacts(toImport)
                            onDismiss()
                        },
                        enabled = selected.isNotEmpty()
                    ) {
                        Text("Import (${selected.size})")
                    }
                }
            )
        }
    ) { paddingValues ->

        when {
            uiState.isLoadingPhoneContacts -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Reading contacts...")
                    }
                }
            }

            availableContacts.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "All your phone contacts are already in RE:Connect",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // ── Select All / Deselect All
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (selected.size == availableContacts.size) {
                                        selected.clear()
                                    } else {
                                        selected.clear()
                                        selected.addAll(availableContacts.map { it.nativeId })
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selected.size == availableContacts.size,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        selected.clear()
                                        selected.addAll(availableContacts.map { it.nativeId })
                                    } else {
                                        selected.clear()
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Select All (${availableContacts.size})",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        HorizontalDivider()
                    }

                    // ── Individual contact rows
                    items(availableContacts, key = { it.nativeId }) { contact ->
                        ContactPickerRow(
                            contact = contact,
                            isSelected = contact.nativeId in selected,
                            onToggle = {
                                if (contact.nativeId in selected) {
                                    selected.remove(contact.nativeId)
                                } else {
                                    selected.add(contact.nativeId)
                                }
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ContactPickerRow(
    contact: ImportedContact,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(36.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = contact.name, fontWeight = FontWeight.Medium)
            contact.phoneNumber?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}