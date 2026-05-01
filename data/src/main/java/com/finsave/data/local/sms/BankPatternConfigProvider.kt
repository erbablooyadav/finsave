package com.finsave.data.local.sms

import android.content.Context
import android.util.Log
import com.finsave.domain.model.sms.BankPatternConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides the bank pattern config by loading it from bank_patterns.json in assets.
 * Falls back to an empty config (no banks) if the file cannot be loaded, so the
 * worker fails gracefully rather than crashing.
 */
@Singleton
class BankPatternConfigProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val configLoader: BankPatternConfigLoader
) {
    @Volatile
    private var cachedConfig: BankPatternConfig? = null

    suspend fun warmCache() {
        if (cachedConfig == null) {
            cachedConfig = try {
                configLoader.loadConfig()
            } catch (e: Exception) {
                Log.e("BankPatternConfig", "Failed to warm bank_patterns.json", e)
                emptyConfig()
            }
        }
    }

    fun getConfig(): BankPatternConfig {
        cachedConfig?.let { return it }
        return runBlocking {
            try {
                configLoader.loadConfig().also { cachedConfig = it }
            } catch (e: Exception) {
                Log.e("BankPatternConfig", "Failed to load bank_patterns.json", e)
                emptyConfig().also { cachedConfig = it }
            }
        }
    }

    private fun emptyConfig(): BankPatternConfig =
        BankPatternConfig(
            version = 0,
            lastUpdated = "",
            banks = emptyList(),
            autoCategoryMappings = emptyMap()
        )
}
