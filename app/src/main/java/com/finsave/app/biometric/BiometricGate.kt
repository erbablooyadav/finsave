package com.finsave.app.biometric

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.core.content.ContextCompat
import com.finsave.core.common.Constants
import com.finsave.core.common.prefs.PreferencesManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BiometricGate manages biometric authentication for app lock functionality.
 * 
 * Requirements: 12.1, 12.2, 12.3, 12.4, 12.5, 12.6, 12.7, 12.8
 */
@Singleton
class BiometricGate @Inject constructor(
    private val preferencesManager: PreferencesManager
) {
    /**
     * Checks if the biometric gate should be shown based on user preferences.
     * 
     * @return true if app lock is enabled and the configured timeout has elapsed
     */
    fun shouldShowGate(): Boolean {
        if (!preferencesManager.getBoolean(Constants.PREFS_APP_LOCK_ENABLED, false)) return false

        val timeoutSeconds = preferencesManager.getInt(Constants.PREFS_AUTO_LOCK_TIMEOUT, 300)
        if (timeoutSeconds == 0) return false
        if (timeoutSeconds == -1) return true

        val lastPausedAt = preferencesManager.getLong(Constants.PREFS_LAST_PAUSED_AT, 0L)
        if (lastPausedAt == 0L) return true

        val elapsedSeconds = (System.currentTimeMillis() - lastPausedAt) / 1000
        return elapsedSeconds >= timeoutSeconds
    }

    /**
     * Initiates biometric authentication with fallback to device credentials.
     * 
     * Uses BIOMETRIC_STRONG OR DEVICE_CREDENTIAL to allow PIN/pattern fallback
     * for devices without enrolled biometrics.
     * 
     * @param activity The FragmentActivity context for showing the prompt
     * @param onSuccess Callback invoked when authentication succeeds
     * @param onFailure Callback invoked when authentication fails or is cancelled
     */
    fun authenticate(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                // User cancelled or authentication failed - keep gate visible
                onFailure()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // Single authentication attempt failed - prompt remains visible
                // User can retry
            }
        }

        val biometricPrompt = BiometricPrompt(activity, executor, callback)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock FinSave")
            .setSubtitle("Authenticate to access your financial data")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
