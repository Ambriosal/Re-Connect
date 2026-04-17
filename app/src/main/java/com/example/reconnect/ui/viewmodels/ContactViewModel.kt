package com.example.reconnect.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.reconnect.data.local.ContactEntity
import com.example.reconnect.data.local.REConnectRepository
import com.example.reconnect.data.model.toUiModel
import android.content.Context
import android.net.Uri
import com.example.reconnect.util.readContactFromUri

import com.example.reconnect.data.model.ContactUiModel
import com.example.reconnect.util.ImportedContact
import com.example.reconnect.util.readAllPhoneContacts
import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ── UI State ──────────────────────────────────────────────────────────────────
// This is what the screen observes. One clean object representing everything
// the Contacts screen needs to display.
data class ContactsUiState(
    val contacts: List<ContactUiModel> = emptyList(),
    val isLoading: Boolean = true,
    val contactCount: Int = 0,
    val phoneContacts: List<ImportedContact> = emptyList(),   // ← all phone contacts
    val isLoadingPhoneContacts: Boolean = false
)

// ── ViewModel ─────────────────────────────────────────────────────────────────
class ContactsViewModel(private val repository: REConnectRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactsUiState())
    val uiState: StateFlow<com.example.reconnect.ui.viewmodel.ContactsUiState> = _uiState.asStateFlow()

    data class ContactsUiState(
        val contacts: List<ContactUiModel> = emptyList(),
        val isLoading: Boolean = true,
        val contactCount: Int = 0,           // ← add this
        val isLoadingPhoneContacts: Boolean
    )

    init {
        viewModelScope.launch {
            repository.getAllContacts()
                .collect { contactList ->
                    val uiModels = contactList.map { entity ->
                        val lastInteraction = repository.getLastInteraction(entity.id)
                        entity.toUiModel(lastInteraction)
                    }
                    _uiState.update {
                        it.copy(
                            contacts = uiModels,
                            isLoading = false,
                            contactCount = uiModels.size)
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

    fun importContactFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            val imported = readContactFromUri(context, uri) ?: return@launch
            repository.importContact(imported)
        }
    }

    fun loadPhoneContacts(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoadingPhoneContacts = true) }
            val phoneContacts = readAllPhoneContacts(context)
            _uiState.update {
                it.copy(
                    phoneContacts = phoneContacts,
                    isLoadingPhoneContacts = false
                )
            }
        }
    }

    fun importMultipleContacts(selected: List<ImportedContact>) {
        viewModelScope.launch {
            selected.forEach { imported ->
                repository.importContact(imported)
            }
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