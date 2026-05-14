package com.finsave.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finsave.core.common.Constants
import com.finsave.core.common.prefs.PreferencesManager
import com.finsave.domain.usecase.export.ExportCsvUseCase
import com.finsave.domain.usecase.export.ExportPdfUseCase
import com.finsave.domain.usecase.settings.ClearAllDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val syncManager: com.finsave.domain.usecase.sync.SyncManager,
    private val exportCsvUseCase: ExportCsvUseCase,
    private val exportPdfUseCase: ExportPdfUseCase,
    private val clearAllDataUseCase: ClearAllDataUseCase,
    private val preferencesManager: PreferencesManager,
    private val analyticsRepository: com.finsave.domain.repository.AnalyticsRepository
) : ViewModel() {

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()
    
    private val _isSyncEnabled = MutableStateFlow(true)
    val isSyncEnabled: StateFlow<Boolean> = _isSyncEnabled.asStateFlow()
    
    private val _isAppLockEnabled = MutableStateFlow(
        preferencesManager.getBoolean(Constants.PREFS_APP_LOCK_ENABLED, false)
    )
    val isAppLockEnabled: StateFlow<Boolean> = _isAppLockEnabled.asStateFlow()

    private val _autoLockTimeoutSeconds = MutableStateFlow(
        preferencesManager.getInt(Constants.PREFS_AUTO_LOCK_TIMEOUT, 300)
    )
    val autoLockTimeoutSeconds: StateFlow<Int> = _autoLockTimeoutSeconds.asStateFlow()
    
    private val _exportErrorMessage = MutableStateFlow<String?>(null)
    val exportErrorMessage: StateFlow<String?> = _exportErrorMessage.asStateFlow()

    private val _settingsMessage = MutableStateFlow<String?>(null)
    val settingsMessage: StateFlow<String?> = _settingsMessage.asStateFlow()

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

    fun setAutoLockTimeout(seconds: Int) {
        _autoLockTimeoutSeconds.value = seconds
        preferencesManager.setInt(Constants.PREFS_AUTO_LOCK_TIMEOUT, seconds)
    }

    fun clearAllData() {
        viewModelScope.launch {
            val result = clearAllDataUseCase()
            _settingsMessage.value = if (result.isSuccess) {
                "All data has been cleared"
            } else {
                "Failed to clear data. Please try again."
            }
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

    fun exportPdf(context: Context) {
        viewModelScope.launch {
            val result = exportPdfUseCase(context)
            if (result.isFailure) {
                _exportErrorMessage.value = "PDF export failed. Please try again."
            }
        }
    }
    
    /**
     * Clear the export error message after showing snackbar.
     */
    fun clearExportError() {
        _exportErrorMessage.value = null
    }

    fun clearSettingsMessage() {
        _settingsMessage.value = null
    }

    /**
     * Generates a diagnostic report and opens email app to send it.
     * Includes device info and recent local analytics events.
     * Requirements: E5.3
     */
    fun generateIssueReport(context: Context) {
        viewModelScope.launch {
            val events = analyticsRepository.getRecentEvents(50).first()
            val deviceInfo = """
                Model: ${android.os.Build.MODEL}
                Manufacturer: ${android.os.Build.MANUFACTURER}
                Android Version: ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})
                App Version: ${context.packageManager.getPackageInfo(context.packageName, 0).versionName}
            """.trimIndent()
            
            val eventLog = events.joinToString("\n") { 
                val time = java.time.Instant.ofEpochMilli(it.timestamp)
                    .atZone(java.time.ZoneId.of("Asia/Kolkata"))
                    .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
                "$time: ${it.name} ${it.propertiesJson ?: ""}" 
            }
            
            val body = "Please describe the issue below:\n\n\n\n--- Diagnostic Info ---\n$deviceInfo\n\n--- Recent Events ---\n$eventLog"
            
            val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                data = android.net.Uri.parse("mailto:")
                putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf("support@finsave.app"))
                putExtra(android.content.Intent.EXTRA_SUBJECT, "FinSave Issue Report")
                putExtra(android.content.Intent.EXTRA_TEXT, body)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            try {
                context.startActivity(android.content.Intent.createChooser(intent, "Send Report via Email"))
            } catch (e: Exception) {
                _settingsMessage.value = "No email app found to send report"
            }
        }
    }
}
