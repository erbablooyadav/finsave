package com.finsave.app.biometric

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity

/**
 * BiometricGateScreen is a full-screen overlay that blocks all app content
 * until the user successfully authenticates via biometrics or device credentials.
 * 
 * On authentication failure or cancellation, the prompt is re-presented automatically.
 * 
 * Requirements: 12.2, 12.4, 12.5, 12.6
 * 
 * @param biometricGate The BiometricGate instance to handle authentication
 * @param onAuthenticated Callback invoked when authentication succeeds
 */
@Composable
fun BiometricGateScreen(
    biometricGate: BiometricGate,
    onAuthenticated: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    // Automatically present the biometric prompt when this screen is shown
    LaunchedEffect(Unit) {
        activity?.let {
            biometricGate.authenticate(
                activity = it,
                onSuccess = onAuthenticated,
                onFailure = {
                    // On failure/cancel, re-present the prompt
                    // This creates a loop until authentication succeeds
                    biometricGate.authenticate(
                        activity = it,
                        onSuccess = onAuthenticated,
                        onFailure = { /* Keep gate visible */ }
                    )
                }
            )
        }
    }

    // Full-screen overlay that blocks all content
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "App Locked",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "FinSave is Locked",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Authenticate to access your financial data",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    activity?.let {
                        biometricGate.authenticate(
                            activity = it,
                            onSuccess = onAuthenticated,
                            onFailure = { /* Keep gate visible */ }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text("Unlock")
            }
        }
    }
}
