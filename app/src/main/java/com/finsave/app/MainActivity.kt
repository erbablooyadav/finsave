package com.finsave.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import com.finsave.app.biometric.BiometricGate
import com.finsave.app.biometric.BiometricGateScreen
import com.finsave.app.navigation.FinSaveNavHost
import com.finsave.core.common.Constants
import com.finsave.core.common.prefs.PreferencesManager
import com.finsave.core.common.startup.AppLaunchCoordinator
import com.finsave.core.ui.theme.FinSaveTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var biometricGate: BiometricGate

    @Inject
    lateinit var appLaunchCoordinator: AppLaunchCoordinator

    @Inject
    lateinit var preferencesManager: PreferencesManager

    @Inject
    lateinit var analyticsRepository: com.finsave.domain.repository.AnalyticsRepository
    
    private var isAuthenticated by mutableStateOf(true)
    private var showWhatsNew by mutableStateOf(false)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        
        // Edge to edge for immersive UI (Android 15+ requirement/best practice)
        enableEdgeToEdge()
        
        // Observe lifecycle events to trigger biometric gate on resume
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // When app resumes from background, check if gate should be shown
                if (biometricGate.shouldShowGate()) {
                    isAuthenticated = false
                }
                // Clear the pause timestamp because we are now in foreground.
                // This prevents re-triggering the gate until the next background session.
                preferencesManager.setLong(Constants.PREFS_LAST_PAUSED_AT, 0L)
            } else if (event == Lifecycle.Event.ON_CREATE) {
                // Track cold start / app launch
                lifecycleScope.launch {
                    analyticsRepository.trackEvent("APP_OPEN")
                }
                
                // Check for "What's New" display
                val currentVersion = try {
                    packageManager.getPackageInfo(packageName, 0).versionCode
                } catch (e: Exception) { 0 }
                
                val lastVersion = preferencesManager.getInt(Constants.PREFS_LAST_VERSION_CODE, 0)
                if (currentVersion > lastVersion) {
                    showWhatsNew = true
                }
            }
        })
        
        setContent {
            FinSaveTheme {
                // Show biometric gate if not authenticated and gate is enabled
                if (!isAuthenticated && (preferencesManager.getInt(Constants.PREFS_AUTO_LOCK_TIMEOUT, 300) != 0)) {
                    BiometricGateScreen(
                        biometricGate = biometricGate,
                        onAuthenticated = { 
                            isAuthenticated = true 
                            // Ensure timestamp is cleared on successful auth
                            preferencesManager.setLong(Constants.PREFS_LAST_PAUSED_AT, 0L)
                        }
                    )
                } else {
                    val navController = androidx.navigation.compose.rememberNavController()
                    val startDestination = remember { appLaunchCoordinator.resolveStartDestination() }

                    // E1.7: Reactive unseen SMS count for bottom nav badge
                    var unseenSmsCount by remember { mutableIntStateOf(preferencesManager.getUnseenSmsCount()) }
                    
                    // Poll unseen count periodically to react to worker updates
                    LaunchedEffect(Unit) {
                        while (true) {
                            unseenSmsCount = preferencesManager.getUnseenSmsCount()
                            delay(2000) // Check every 2 seconds
                        }
                    }

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            com.finsave.app.navigation.FinSaveBottomNavBar(
                                navController = navController,
                                unseenSmsCount = unseenSmsCount,
                                onTransactionsTabSelected = {
                                    // Clear badge when Transactions tab is opened
                                    preferencesManager.setUnseenSmsCount(0)
                                    unseenSmsCount = 0
                                }
                            )
                        }
                    ) { innerPadding ->
                        FinSaveNavHost(
                            modifier = Modifier.padding(innerPadding),
                            navController = navController,
                            startDestination = startDestination
                        )
                    }
                }

                if (showWhatsNew) {
                    WhatsNewBottomSheet(
                        onDismiss = {
                            showWhatsNew = false
                            val currentVersion = try {
                                packageManager.getPackageInfo(packageName, 0).versionCode
                            } catch (e: Exception) { 0 }
                            preferencesManager.setInt(Constants.PREFS_LAST_VERSION_CODE, currentVersion)
                        }
                    )
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        preferencesManager.setLong(Constants.PREFS_LAST_PAUSED_AT, System.currentTimeMillis())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsNewBottomSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "What's New in FinSave",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            
            item {
                ChangelogItem(
                    title = "🚀 Performance & Privacy",
                    description = "Cold starts under 1.2s and 100% offline verification in Settings."
                )
            }
            
            item {
                ChangelogItem(
                    title = "🔥 Habit streaks",
                    description = "Keep your budget streak alive with Duolingo-style fire animations!"
                )
            }
            
            item {
                ChangelogItem(
                    title = "📸 UPI QR Scanning",
                    description = "Scan any UPI QR code to instantly log a transaction."
                )
            }
            
            item {
                ChangelogItem(
                    title = "🔍 Smart Search",
                    description = "Blazing fast full-text search for all your transactions."
                )
            }
            
            item {
                androidx.compose.material3.Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                ) {
                    Text("Got it!")
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun ChangelogItem(title: String, description: String) {
    Column {
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(text = description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
