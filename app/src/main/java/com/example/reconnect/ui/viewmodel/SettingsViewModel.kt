package com.example.reconnect.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.reconnect.REConnectApplication
import com.example.reconnect.data.local.AppPreferences
import com.example.reconnect.data.local.REConnectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isClearing: Boolean = false,   // true while the delete is in progress
    val clearSuccess: Boolean = false, // briefly true after clear completes
    val reminderHour: Int = AppPreferences.DEFAULT_HOUR,
    val reminderMinute: Int = AppPreferences.DEFAULT_MINUTE
)

class SettingsViewModel(
    private val repository: REConnectRepository,
    private val application: REConnectApplication
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            application.appPreferences.reminderTime.collect { (hour, minute) ->
                _uiState.update { it.copy(reminderHour = hour, reminderMinute = minute) }
            }
        }
    }

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

    // Persists the new daily check time and reschedules the reminder worker to match
    fun setReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            application.appPreferences.setReminderTime(hour, minute)
            application.scheduleReminderCheck(hour, minute)
        }
    }
}

class SettingsViewModelFactory(
    private val repository: REConnectRepository,
    private val application: REConnectApplication
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(repository, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
