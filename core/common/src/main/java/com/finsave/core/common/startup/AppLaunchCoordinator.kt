package com.finsave.core.common.startup

import android.util.Log
import com.finsave.core.common.Constants
import com.finsave.core.common.prefs.PreferencesManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLaunchCoordinator @Inject constructor(
    private val preferencesManager: PreferencesManager
) {
    fun resolveStartDestination(): String {
        val isOnboardingComplete =
            preferencesManager.getBoolean(Constants.PREFS_ONBOARDING_COMPLETE, false)
        return if (isOnboardingComplete) {
            Constants.Routes.DASHBOARD
        } else {
            Constants.Routes.ONBOARDING
        }
    }

    fun markOnboardingComplete() {
        preferencesManager.setBoolean(Constants.PREFS_ONBOARDING_COMPLETE, true)
        // Analytics hook for future telemetry integration.
        runCatching { Log.d(TAG, "onboarding_completed") }
    }

    companion object {
        private const val TAG = "AppLaunchCoordinator"
    }
}
