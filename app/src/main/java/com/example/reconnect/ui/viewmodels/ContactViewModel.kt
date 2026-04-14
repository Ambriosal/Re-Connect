package com.example.reconnect.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.reconnect.data.local.ContactEntity
import com.example.reconnect.data.local.REConnectRepository
import com.example.reconnect.data.model.toUiModel

import com.example.reconnect.data.model.ContactUiModel

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ── UI State ──────────────────────────────────────────────────────────────────
// This is what the screen observes. One clean object representing everything
// the Contacts screen needs to display.
data class ContactsUiState(
    val contacts: List<ContactUiModel> = emptyList(),
    val isLoading: Boolean = true
)

// ── ViewModel ─────────────────────────────────────────────────────────────────
class ContactsViewModel(private val repository: REConnectRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactsUiState())
    val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()


    init {
        viewModelScope.launch {
            repository.getAllContacts()
                .collect { contactList ->
                    val uiModels = contactList.map { entity ->
                        val lastInteraction = repository.getLastInteraction(entity.id)
                        entity.toUiModel(lastInteraction)
                    }
                    _uiState.update {
                        it.copy(contacts = uiModels, isLoading = false)
                    }
                }
        }
    }

    fun addContact(
        name: String,
        phoneNumber: String?,
        relationshipLabel: String,
        reminderFrequencyDays: Int
    ) {
        viewModelScope.launch {
            repository.insertContact(
                ContactEntity(
                    name = name,
                    phoneNumber = phoneNumber,
                    relationshipLabel = relationshipLabel,
                    reminderFrequencyDays = reminderFrequencyDays
                )
            )
        }
    }

    fun deleteContact(contactId: Long) {
        viewModelScope.launch {
            repository.deleteContactById(contactId)
        }
    }

    fun quickLogContact(contactId: Long, platform: String = "manual") {
        viewModelScope.launch {
            repository.logInteraction(
                contactId = contactId,
                platform = platform
            )
        }
    }
}

// ── Factory ───────────────────────────────────────────────────────────────────
// Needed because our ViewModel takes a constructor argument (the repository).
// Android can't create it automatically without this.
class ContactsViewModelFactory(private val repository: REConnectRepository) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ContactsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ContactsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}