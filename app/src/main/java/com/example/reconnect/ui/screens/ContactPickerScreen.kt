package com.example.reconnect.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.reconnect.util.ImportedContact
import com.example.reconnect.ui.components.ContactAvatar
import com.example.reconnect.ui.viewmodel.ContactsViewModel

// Defines the available sort modes — sealed class so the when() below is exhaustive
sealed class PickerSortOrder(val label: String) {
    object AtoZ        : PickerSortOrder("A → Z")
    object ZtoA        : PickerSortOrder("Z → A")
    object PhoneFirst  : PickerSortOrder("Has Phone")
    object LastCall    : PickerSortOrder("Last Call")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactPickerScreen(
    viewModel: ContactsViewModel,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val selected = remember { mutableStateListOf<String>() }

    // ── Search and sort state
    var searchQuery by remember { mutableStateOf("") }
    var sortOrder by remember { mutableStateOf<PickerSortOrder>(PickerSortOrder.AtoZ) }
    var showSortMenu by remember { mutableStateOf(false) }
//
//    // ── Filter out already-imported contacts
//    val existingNativeIds = uiState.contacts
//        .mapNotNull { it.nativeContactId }
//        .toSet()

    // ── Apply search then sort — this is a pure transformation of an in-memory list
    // Re-runs automatically whenever searchQuery, sortOrder, or phoneContacts changes
    val displayedContacts = remember(
        searchQuery,
        sortOrder,
        uiState.phoneContacts,
        uiState.contacts        // ← add this key
    ) {
        // Move inside the block — now always fresh when remember re-runs
        val existingNativeIds = uiState.contacts
            .mapNotNull { it.nativeContactId }
            .toSet()

        val available = uiState.phoneContacts
            .filter { it.nativeId !in existingNativeIds }

        val filtered = if (searchQuery.isBlank()) {
            available
        } else {
            val query = searchQuery.trim().lowercase()
            available.filter { contact ->
                contact.name.lowercase().contains(query) ||
                        contact.phoneNumber?.contains(query) == true
            }
        }

        when (sortOrder) {
            PickerSortOrder.AtoZ       -> filtered.sortedBy { it.name.lowercase() }
            PickerSortOrder.ZtoA       -> filtered.sortedByDescending { it.name.lowercase() }
            PickerSortOrder.PhoneFirst -> filtered.sortedByDescending { it.phoneNumber != null }
            // Most recently called first; contacts with no call history sink to the bottom
            PickerSortOrder.LastCall   -> filtered.sortedByDescending { it.lastTimeContacted ?: -1L }
        }
    }

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
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                },
                actions = {
                    // ── Sort button + dropdown
                    Box {
                        TextButton(onClick = { showSortMenu = true }) {
                            Text(sortOrder.label)
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            listOf(
                                PickerSortOrder.AtoZ,
                                PickerSortOrder.ZtoA,
                                PickerSortOrder.PhoneFirst,
                                PickerSortOrder.LastCall
                            ).forEach { order ->
                                DropdownMenuItem(
                                    text = { Text(order.label) },
                                    onClick = {
                                        sortOrder = order
                                        showSortMenu = false
                                    },
                                    // Checkmark on the active sort so user knows current state
                                    trailingIcon = {
                                        if (sortOrder == order) {
                                            Icon(Icons.Default.Check, contentDescription = null)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    TextButton(
                        onClick = {
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
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text("Reading contacts...")
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues)
                ) {

                    // ── Search bar — pinned at top of list
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search contacts...") },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null)
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    // ── Result count — helps user know how many match their search
                    item {
                        Text(
                            text = when {
                                searchQuery.isBlank() -> "${displayedContacts.size} contacts available"
                                displayedContacts.isEmpty() -> "No contacts match \"$searchQuery\""
                                else -> "${displayedContacts.size} result${if (displayedContacts.size == 1) "" else "s"}"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }

                    // ── Select All / Deselect All — operates on displayed list only
                    // If searching, "Select All" only selects the filtered results
                    if (displayedContacts.isNotEmpty()) {
                        item {
                            val allDisplayedSelected = displayedContacts
                                .all { it.nativeId in selected }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (allDisplayedSelected) {
                                            // Deselect only the currently displayed contacts
                                            displayedContacts.forEach {
                                                selected.remove(it.nativeId)
                                            }
                                        } else {
                                            displayedContacts
                                                .map { it.nativeId }
                                                .forEach { id ->
                                                    if (id !in selected) selected.add(id)
                                                }
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = allDisplayedSelected,
                                    onCheckedChange = null   // handled by row click above
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (searchQuery.isBlank()) "Select All"
                                    else "Select all results",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            HorizontalDivider()
                        }
                    }

                    // ── Contact rows
                    items(displayedContacts, key = { it.nativeId }) { contact ->
                        ContactPickerRow(
                            contact = contact,
                            isSelected = contact.nativeId in selected,
                            showLastContacted = sortOrder == PickerSortOrder.LastCall,
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
@Composable
fun ContactPickerRow(
    contact: ImportedContact,
    isSelected: Boolean,
    showLastContacted: Boolean = false,
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
        ContactAvatar(photoUri = contact.photoUri, size = 36.dp)
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
            if (showLastContacted) {
                Text(
                    text = contact.lastTimeContacted?.let { millis ->
                        "Last called " + java.text.SimpleDateFormat(
                            "MMM d, yyyy", java.util.Locale.getDefault()
                        ).format(java.util.Date(millis))
                    } ?: "No call history",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}