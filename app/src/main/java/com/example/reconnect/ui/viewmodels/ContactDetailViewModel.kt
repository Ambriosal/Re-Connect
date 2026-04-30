package com.example.reconnect.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.reconnect.data.local.ContactEntity
import com.example.reconnect.data.local.InteractionEntity
import com.example.reconnect.data.local.REConnectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ── UI State for the detail screen ────────────────────────────────────────────
// Updated UiState — add the interactions list
data class ContactDetailUiState(
    val contact: ContactEntity? = null,
    val interactions: List<InteractionEntity> = emptyList(),
    val lastContactedAt: Long? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false
)

// ── ViewModel ─────────────────────────────────────────────────────────────────
class ContactDetailViewModel(
    private val repository: REConnectRepository,
    private val contactId: Long           // ← scoped to ONE contact
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactDetailUiState())
    val uiState: StateFlow<ContactDetailUiState> = _uiState.asStateFlow()

    init {
        // Launch 1: load the contact ONCE — one-shot suspend call, not a Flow
        // This is what was missing — without it isLoading never becomes false
        viewModelScope.launch {
            val contact = repository.getContactById(contactId)
            android.util.Log.d("DetailVM", "contact loaded: $contact")  // temporary debug

            _uiState.update {
                it.copy(
                    contact = contact,
                    isLoading = false    // ← this is what unblocks the screen
                )
            }
        }

        // Launch 2: observe interactions as a live stream
        // Merged the two duplicate collectors into one — both jobs done here
        viewModelScope.launch {
            repository.getInteractionsForContact(contactId)
                .collect { interactions ->
                    _uiState.update {
                        it.copy(
                            interactions = interactions,
                            lastContactedAt = interactions.firstOrNull()?.occurredAt
                        )
                    }
                }
        }
    }

    // Called from the screen when user confirms a log entry
    fun logInteraction(type: String, notes: String) {
        viewModelScope.launch {
            repository.logInteraction(
                contactId = contactId,
                platform = type,
                notes = notes
            )
        }
    }

    // Called when user saves edits to name, label, notes, etc.
    fun updateContact(updated: ContactEntity) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            repository.updateContact(updated)
            _uiState.update { it.copy(contact = updated, isSaving = false) }
        }
    }
}

// ── Factory ───────────────────────────────────────────────────────────────────
// Same pattern as ContactsViewModelFactory, but now takes contactId too
class ContactDetailViewModelFactory(
    private val repository: REConnectRepository,
    private val contactId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ContactDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ContactDetailViewModel(repository, contactId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}