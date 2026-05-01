package com.finsave.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.finsave.app.biometric.BiometricGate
import com.finsave.app.biometric.BiometricGateScreen
import com.finsave.app.navigation.FinSaveNavHost
import com.finsave.core.common.startup.AppLaunchCoordinator
import com.finsave.core.ui.theme.FinSaveTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var biometricGate: BiometricGate

    @Inject
    lateinit var appLaunchCoordinator: AppLaunchCoordinator
    
    private var isAuthenticated by mutableStateOf(true)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Edge to edge for immersive UI (Android 15+ requirement/best practice)
        enableEdgeToEdge()
        
        // Observe lifecycle events to trigger biometric gate on resume
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // When app resumes from background, check if gate should be shown
                if (biometricGate.shouldShowGate()) {
                    isAuthenticated = false
                }
            }
        })
        
        setContent {
            FinSaveTheme {
                // Show biometric gate if not authenticated and gate is enabled
                if (!isAuthenticated && biometricGate.shouldShowGate()) {
                    BiometricGateScreen(
                        biometricGate = biometricGate,
                        onAuthenticated = { isAuthenticated = true }
                    )
                } else {
                    val navController = androidx.navigation.compose.rememberNavController()
                    val startDestination = remember { appLaunchCoordinator.resolveStartDestination() }
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            com.finsave.app.navigation.FinSaveBottomNavBar(navController = navController)
                        }
                    ) { innerPadding ->
                        FinSaveNavHost(
                            modifier = Modifier.padding(innerPadding),
                            navController = navController,
                            startDestination = startDestination
                        )
                    }
                }
            }
        }
    }
}
