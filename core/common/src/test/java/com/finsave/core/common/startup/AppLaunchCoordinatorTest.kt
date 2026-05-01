package com.finsave.core.common.startup

import com.finsave.core.common.Constants
import com.finsave.core.common.prefs.PreferencesManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class AppLaunchCoordinatorTest {

    private val preferencesManager = mockk<PreferencesManager>(relaxed = true)
    private val coordinator = AppLaunchCoordinator(preferencesManager)

    @Test
    fun `resolveStartDestination returns onboarding when onboarding incomplete`() {
        every {
            preferencesManager.getBoolean(Constants.PREFS_ONBOARDING_COMPLETE, false)
        } returns false

        val destination = coordinator.resolveStartDestination()

        assert(destination == Constants.Routes.ONBOARDING) {
            "Expected onboarding destination, got: $destination"
        }
    }

    @Test
    fun `resolveStartDestination returns dashboard when onboarding complete`() {
        every {
            preferencesManager.getBoolean(Constants.PREFS_ONBOARDING_COMPLETE, false)
        } returns true

        val destination = coordinator.resolveStartDestination()

        assert(destination == Constants.Routes.DASHBOARD) {
            "Expected dashboard destination, got: $destination"
        }
    }

    @Test
    fun `markOnboardingComplete persists completion flag`() {
        coordinator.markOnboardingComplete()

        verify(exactly = 1) {
            preferencesManager.setBoolean(Constants.PREFS_ONBOARDING_COMPLETE, true)
        }
    }
}
