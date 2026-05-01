package com.finsave.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finsave.core.ui.theme.LocalSpacing

@Composable
fun SettingsScreen(
    onNavigateToAccounts: () -> Unit = {},
    onNavigateToCategories: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isSyncEnabled by viewModel.isSyncEnabled.collectAsState()
    val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsState()
    val autoLockTimeoutSeconds by viewModel.autoLockTimeoutSeconds.collectAsState()
    val exportErrorMessage by viewModel.exportErrorMessage.collectAsState()
    val settingsMessage by viewModel.settingsMessage.collectAsState()
    val spacing = LocalSpacing.current
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showResyncDialog by remember { mutableStateOf(false) }
    var showBiometricErrorDialog by remember { mutableStateOf(false) }
    var biometricErrorMessage by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Show snackbar when export error occurs
    androidx.compose.runtime.LaunchedEffect(exportErrorMessage) {
        exportErrorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearExportError()
        }
    }

    androidx.compose.runtime.LaunchedEffect(settingsMessage) {
        settingsMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSettingsMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = spacing.large),
            contentPadding = PaddingValues(vertical = spacing.large),
            verticalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            item {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(spacing.large))
            }

            item {
                SettingsSectionTitle("Preferences")
                
                SettingsToggleRow(
                    title = "Dark Mode",
                    subtitle = "Toggle dark theme manually",
                    checked = isDarkMode,
                    onCheckedChange = { viewModel.toggleDarkMode(it) }
                )
                
                SettingsToggleRow(
                    title = "Auto-Sync SMS",
                    subtitle = "Automatically scan SMS for transactions",
                    checked = isSyncEnabled,
                    onCheckedChange = { viewModel.toggleSync(it) }
                )
                
                SettingsToggleRow(
                    title = "App Lock",
                    subtitle = "Require biometrics or PIN to unlock app",
                    checked = isAppLockEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            // Check if device has biometric capability before enabling
                            val biometricManager = androidx.biometric.BiometricManager.from(context)
                            val canAuthenticate = biometricManager.canAuthenticate(
                                androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or
                                androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
                            )
                            
                            when (canAuthenticate) {
                                androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS -> {
                                    // Device has biometric or device credential available
                                    viewModel.toggleAppLock(true)
                                }
                                androidx.biometric.BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                                    biometricErrorMessage = "App Lock requires a device PIN, pattern, or password to be set"
                                    showBiometricErrorDialog = true
                                }
                                androidx.biometric.BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                                    biometricErrorMessage = "App Lock requires a device PIN, pattern, or password to be set"
                                    showBiometricErrorDialog = true
                                }
                                else -> {
                                    biometricErrorMessage = "App Lock is not available on this device"
                                    showBiometricErrorDialog = true
                                }
                            }
                        } else {
                            // Disabling app lock - no validation needed
                            viewModel.toggleAppLock(false)
                        }
                    }
                )

                if (isAppLockEnabled) {
                    Spacer(modifier = Modifier.height(spacing.small))
                    AutoLockTimeoutSelector(
                        selectedSeconds = autoLockTimeoutSeconds,
                        onTimeoutSelected = viewModel::setAutoLockTimeout
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(spacing.medium))
                SettingsSectionTitle("Manage")
                
                SettingsButtonRow(
                    title = "Accounts",
                    subtitle = "Manage your bank accounts and wallets",
                    buttonText = "Open",
                    onClick = onNavigateToAccounts
                )
                
                SettingsButtonRow(
                    title = "Categories",
                    subtitle = "Customize expense categories",
                    buttonText = "Open",
                    onClick = onNavigateToCategories
                )
            }

            item {
                Spacer(modifier = Modifier.height(spacing.medium))
                SettingsSectionTitle("Data Management")
                
                SettingsButtonRow(
                    title = "Export to CSV",
                    subtitle = "Download all your transactions",
                    buttonText = "Export",
                    onClick = { viewModel.exportCsv(context) }
                )

                SettingsButtonRow(
                    title = "Export PDF Summary",
                    subtitle = "Share a monthly report from this device",
                    buttonText = "Export",
                    onClick = { viewModel.exportPdf(context) }
                )
                
                SettingsButtonRow(
                    title = "Re-sync SMS messages",
                    subtitle = "Manually scan SMS for missing transactions",
                    buttonText = "Sync",
                    onClick = { showResyncDialog = true }
                )
                
                SettingsButtonRow(
                    title = "Clear All Data",
                    subtitle = "Permanently delete all transactions and budgets",
                    buttonText = "Delete",
                    isDestructive = true,
                    onClick = { showClearDataDialog = true }
                )
            }
        }

        if (showClearDataDialog) {
            AlertDialog(
                onDismissRequest = { showClearDataDialog = false },
                icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red) },
                title = { Text("Clear All Data") },
                text = { Text("Are you sure you want to delete all transactions, budgets, and splitter groups? This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.clearAllData()
                            showClearDataDialog = false
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                    ) {
                        Text("Delete Everything")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDataDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
        
        if (showResyncDialog) {
            AlertDialog(
                onDismissRequest = { showResyncDialog = false },
                title = { Text("Re-sync SMS messages") },
                text = {
                    Column {
                        Text("Choose how far back you want to scan for missing transactions. Duplicates will be safely ignored.")
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        val options = listOf(
                            "Last 7 Days" to 7,
                            "This Month (30 Days)" to 30,
                            "Last 3 Months (90 Days)" to 90,
                            "All Time" to 0
                        )
                        
                        options.forEach { (label, days) ->
                            TextButton(
                                onClick = {
                                    viewModel.reSyncSms(context, days)
                                    showResyncDialog = false
                                    // Optionally show a toast here
                                    android.widget.Toast.makeText(context, "Sync started in background...", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(label)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showResyncDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
        
        if (showBiometricErrorDialog) {
            AlertDialog(
                onDismissRequest = { showBiometricErrorDialog = false },
                icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                title = { Text("App Lock Unavailable") },
                text = { Text(biometricErrorMessage) },
                confirmButton = {
                    TextButton(onClick = { showBiometricErrorDialog = false }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val spacing = LocalSpacing.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun AutoLockTimeoutSelector(
    selectedSeconds: Int,
    onTimeoutSelected: (Int) -> Unit
) {
    val spacing = LocalSpacing.current
    val options = listOf(
        -1 to "Immediately",
        60 to "1 minute",
        300 to "5 minutes",
        1800 to "30 minutes",
        0 to "Never"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(spacing.medium)
    ) {
        Text(
            text = "Auto-lock timeout",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Choose when FinSave asks to unlock after you leave the app",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(spacing.small))

        options.forEach { (seconds, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedSeconds == seconds,
                    onClick = { onTimeoutSelected(seconds) }
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun SettingsButtonRow(
    title: String,
    subtitle: String,
    buttonText: String = "Action",
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    val spacing = LocalSpacing.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title, 
                style = MaterialTheme.typography.bodyLarge, 
                fontWeight = FontWeight.SemiBold,
                color = if (isDestructive) Color.Red else MaterialTheme.colorScheme.onSurface
            )
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Button(
            onClick = onClick,
            colors = if (isDestructive) ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.1f), contentColor = Color.Red) 
                     else ButtonDefaults.buttonColors()
        ) {
            Text(buttonText)
        }
    }
}
