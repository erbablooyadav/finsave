package com.finsave.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finsave.core.common.Constants
import com.finsave.core.common.prefs.PreferencesManager
import com.finsave.domain.usecase.export.ExportCsvUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val syncManager: com.finsave.domain.usecase.sync.SyncManager,
    private val exportCsvUseCase: ExportCsvUseCase,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()
    
    private val _isSyncEnabled = MutableStateFlow(true)
    val isSyncEnabled: StateFlow<Boolean> = _isSyncEnabled.asStateFlow()
    
    private val _isAppLockEnabled = MutableStateFlow(
        preferencesManager.getBoolean(Constants.PREFS_APP_LOCK_ENABLED, false)
    )
    val isAppLockEnabled: StateFlow<Boolean> = _isAppLockEnabled.asStateFlow()
    
    private val _exportErrorMessage = MutableStateFlow<String?>(null)
    val exportErrorMessage: StateFlow<String?> = _exportErrorMessage.asStateFlow()

    fun toggleDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        // TODO: Save to DataStore
    }

    fun toggleSync(enabled: Boolean) {
        _isSyncEnabled.value = enabled
        if (enabled) {
            syncManager.startPeriodicSync()
        } else {
            syncManager.stopSync()
        }
        // TODO: Save to DataStore
    }

    /**
     * Toggles the app lock setting.
     * 
     * Requirements: 12.1, 12.7
     * 
     * @param enabled Whether app lock should be enabled
     */
    fun toggleAppLock(enabled: Boolean) {
        _isAppLockEnabled.value = enabled
        preferencesManager.setBoolean(Constants.PREFS_APP_LOCK_ENABLED, enabled)
    }

    fun clearAllData() {
        viewModelScope.launch {
            // TODO: Clear database
        }
    }

    /**
     * timeFrameDays = 0 means All Time
     */
    fun reSyncSms(context: android.content.Context, timeFrameDays: Int) {
        val sinceTimestamp = if (timeFrameDays > 0) {
            System.currentTimeMillis() - (timeFrameDays * 24 * 60 * 60 * 1000L)
        } else {
            0L // All time
        }
        
        syncManager.triggerManualSync(sinceTimestamp)
    }
    
    /**
     * Export all transactions to CSV.
     * On failure, sets exportErrorMessage to show snackbar.
     * Requirements: 11.5, 11.7
     */
    fun exportCsv(context: Context) {
        viewModelScope.launch {
            val result = exportCsvUseCase(context)
            if (result.isFailure) {
                _exportErrorMessage.value = "Export failed. Please try again."
            }
        }
    }
    
    /**
     * Clear the export error message after showing snackbar.
     */
    fun clearExportError() {
        _exportErrorMessage.value = null
    }
}
