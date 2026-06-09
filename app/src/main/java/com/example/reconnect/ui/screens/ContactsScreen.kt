package com.example.reconnect.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.runtime.*
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.example.reconnect.data.model.ContactUiModel
import com.example.reconnect.data.model.RelationshipType
import com.example.reconnect.ui.viewmodel.ContactsViewModel



sealed class ContactSortOrderRich(val label: String) {
    object AtoZ             : ContactSortOrderRich("A → Z")
    object ZtoA             : ContactSortOrderRich("Z → A")
    object RecentFirst      : ContactSortOrderRich("Recent First")
    object OverdueFirst     : ContactSortOrderRich("Overdue First")
}
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ContactsScreen(
    viewModel: ContactsViewModel,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToSettings: () -> Unit
) {

    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var showImportOptions by remember { mutableStateOf(false) }
    var pendingLogContactId by remember { mutableStateOf<Long?>(null) }

    var showLogDialog by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var sortOrder by remember { mutableStateOf<ContactSortOrderRich>(ContactSortOrderRich.AtoZ) }
    var showSortMenu by remember { mutableStateOf(false) }
//    var displayedContacts by remember { mutableStateOf(uiState.contacts) }

    val displayedContacts by remember(uiState.contacts, searchQuery, sortOrder) {
        derivedStateOf {
            uiState.contacts
                .filter { contact ->
                    searchQuery.isBlank() || contact.name.contains(searchQuery, ignoreCase = true)
                }
                .let { filtered ->
                    when (sortOrder) {
                        ContactSortOrderRich.AtoZ         -> filtered.sortedBy { it.name.lowercase() }
                        ContactSortOrderRich.ZtoA         -> filtered.sortedByDescending { it.name.lowercase() }
                        ContactSortOrderRich.RecentFirst  -> filtered.sortedByDescending { it.lastContactedAt ?: 0L }
                        ContactSortOrderRich.OverdueFirst -> filtered.sortedBy { it.lastContactedAt ?: Long.MAX_VALUE }
                    }
                }
        }
    }


    // -- Permission State
    val contactsPermission = rememberPermissionState(Manifest.permission.READ_CONTACTS)

    // ── Add this state variable near the top
    var showContactPicker by remember { mutableStateOf(false) }

    // only fires when intended
    var pendingContactImport by remember { mutableStateOf(false) }
    // ── Contact picker launcher
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri ->
        uri?.let { viewModel.importContactFromUri(context, it) }
        pendingContactImport = false
    }


    Scaffold(
        topBar = {
            // Update the TopAppBar to include a settings icon action
            TopAppBar(
                title = { Text("RE:Connect") },
                actions = {
                    // ── Sort dropdown
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,  // up/down arrows icon
                                contentDescription = "Sort"
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            listOf(
                                ContactSortOrderRich.AtoZ,
                                ContactSortOrderRich.ZtoA,
                                ContactSortOrderRich.RecentFirst,
                                ContactSortOrderRich.OverdueFirst
                            ).forEach { order ->
                                DropdownMenuItem(
                                    text = { Text(order.label) },
                                    onClick = {
                                        sortOrder = order
                                        showSortMenu = false
                                    },
                                    trailingIcon = {
                                        if (sortOrder == order) {
                                            Icon(Icons.Default.Check, contentDescription = null)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // ── Settings icon (unchanged)
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showImportOptions = true }) {
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

                        // ── Search bar — pinned at top
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

                        // ── Result count — updates live as user types
                        item {
                            Text(
                                text = when {
                                    searchQuery.isBlank() ->
                                        "${uiState.contactCount} contact${if (uiState.contactCount == 1) "" else "s"}"
                                    displayedContacts.isEmpty() ->
                                        "No contacts match \"$searchQuery\""
                                    else ->
                                        "${displayedContacts.size} result${if (displayedContacts.size == 1) "" else "s"}"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }

                        // ── Contact rows — now from displayedContacts, not uiState.contacts
                        items(displayedContacts, key = { it.id }) { contact ->
                            ContactCard(
                                contact = contact,
                                onQuickLog = {
                                    pendingLogContactId = contact.id
                                    showLogDialog = true
                                },
                                onDelete = { viewModel.deleteContact(contact.id) },
                                onClick = { onNavigateToDetail(contact.id) }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    // ── Add options dialog: Import from phone OR add manually
    if (showImportOptions) {
        AlertDialog(
            onDismissRequest = { showImportOptions = false },
            title = { Text("Add Contact") },
            text = { Text("How would you like to add a contact?") },
            confirmButton = {
                TextButton(onClick = {
                    showImportOptions = false
                    when {
                        contactsPermission.status.isGranted -> {
                            viewModel.loadPhoneContacts(context)   // ← load all contacts
                            showContactPicker = true               // ← show picker
                        }
                        else -> {
                            pendingContactImport = true
                            contactsPermission.launchPermissionRequest()
                        }
                    }
                }) { Text("Import from Phone") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImportOptions = false
                    showAddDialog = true
                }) { Text("Add Manually") }
            }
        )
    }

    if (showContactPicker) {
        ContactPickerScreen(
            viewModel = viewModel,
            onDismiss = { showContactPicker = false }
        )
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

    if (showLogDialog) {
        LogInteractionDialog(
            onDismiss = {
                showLogDialog = false
                pendingLogContactId = null
            },
            onConfirm = { type, notes ->
                pendingLogContactId?.let { contactId ->
                    viewModel.quickLogContact(
                        contactId = contactId,
                        platform = type.key,       // ← was hardcoded "manual" before
                        notes = notes
                    )
                }
                showLogDialog = false
                pendingLogContactId = null
            }
        )
    }

// ── Track whether user just requested import so LaunchedEffect





// ── Only opens picker when user actually requested it
    LaunchedEffect(contactsPermission.status.isGranted, pendingContactImport) {
        if (pendingContactImport && contactsPermission.status.isGranted) {
            viewModel.loadPhoneContacts(context)
            showContactPicker = true
            pendingContactImport = false
        }
    }
}




// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ContactCard(
    contact: ContactUiModel,
    onQuickLog: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }   // ← whole row is now tappable
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
            // In ContactCard, replace the relationshipLabel Text:

            if (contact.relationshipLabel.isNotBlank()) {
                Text(
                    // Before: contact.relationshipLabel
                    // After: mapped through RelationshipType for emoji + clean label
                    text = RelationshipType.displayFrom(contact.relationshipLabel),
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
        var selectedRelationship by remember { mutableStateOf<RelationshipType?>(null) }  // ← replaces label string
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

                    // ── Relationship picker replaces the OutlinedTextField
                    Text(
                        "Relationship Type",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    RelationshipTypePicker(
                        selectedKey = selectedRelationship?.key ?: "",
                        onTypeSelected = { selectedRelationship = it }
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
                                selectedRelationship?.key ?: "",  // ← store the key
                                reminderDays.toIntOrNull() ?: 14
                            )
                        }
                    },
                    // Optionally require a relationship type too
                    enabled = name.isNotBlank()
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        )
    }