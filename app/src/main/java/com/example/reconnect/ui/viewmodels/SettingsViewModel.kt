package com.example.reconnect.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.reconnect.data.local.REConnectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isClearing: Boolean = false,   // true while the delete is in progress
    val clearSuccess: Boolean = false  // briefly true after clear completes
)

class SettingsViewModel(
    private val repository: REConnectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun clearAllData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isClearing = true) }
            repository.clearAllData()
            _uiState.update { it.copy(isClearing = false, clearSuccess = true) }
        }
    }

    // Called after the UI has acknowledged the success state
    // Resets the flag so it doesn't re-trigger on recomposition
    fun onClearSuccessAcknowledged() {
        _uiState.update { it.copy(clearSuccess = false) }
    }
}

class SettingsViewModelFactory(
    private val repository: REConnectRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}