package com.finsave.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

import androidx.work.Configuration
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.finsave.app.workers.DailyDigestWorker
import com.finsave.app.workers.SplitterReminderWorker
import com.finsave.core.common.Constants
import com.finsave.core.common.notifications.NotificationHelper
import com.finsave.data.local.sms.BankPatternConfigProvider
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull

/**
 * Main Application class for FinSave.
 *
 * Annotating with @HiltAndroidApp triggers Hilt's code generation,
 * including a base class for your application that serves as the
 * application-level dependency container.
 */
@HiltAndroidApp
class FinSaveApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    @Inject
    lateinit var syncManager: com.finsave.domain.usecase.sync.SyncManager

    @Inject
    lateinit var accountRepository: com.finsave.domain.repository.AccountRepository

    @Inject
    lateinit var categoryRepository: com.finsave.domain.repository.CategoryRepository

    @Inject
    lateinit var bankPatternConfigProvider: BankPatternConfigProvider

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
        
        // Initialize default database records if missing
        CoroutineScope(Dispatchers.IO).launch {
            val accounts = accountRepository.getAllAccounts().firstOrNull()
            if (accounts.isNullOrEmpty()) {
                accountRepository.insertDefaultAccounts()
            }
            
            val categories = categoryRepository.getAllCategories().firstOrNull()
            if (categories.isNullOrEmpty()) {
                categoryRepository.insertDefaultCategories()
            }
        }
        
        // Start SMS sync on app launch
        syncManager.startPeriodicSync()
        
        scheduleDailyDigest()
        scheduleSplitterReminder()

        // Warm BankPatternConfig cache in background to avoid first worker runBlocking asset load.
        CoroutineScope(Dispatchers.IO).launch {
            bankPatternConfigProvider.warmCache()
        }
    }

    /**
     * Schedules the DailyDigestWorker to run at 22:00 IST every day.
     * Uses ExistingPeriodicWorkPolicy.KEEP to avoid rescheduling on app restart.
     */
    private fun scheduleDailyDigest() {
        val initialDelay = DailyDigestWorker.computeDelayUntil2200Ist()
        
        val dailyDigestRequest = PeriodicWorkRequestBuilder<DailyDigestWorker>(
            24, TimeUnit.HOURS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .addTag(Constants.WORK_DAILY_DIGEST)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            Constants.WORK_DAILY_DIGEST,
            ExistingPeriodicWorkPolicy.KEEP,
            dailyDigestRequest
        )
    }

    /**
     * Schedules the SplitterReminderWorker to run every 24 hours.
     * Uses ExistingPeriodicWorkPolicy.KEEP to avoid rescheduling on app restart.
     */
    private fun scheduleSplitterReminder() {
        val splitterReminderRequest = PeriodicWorkRequestBuilder<SplitterReminderWorker>(
            24, TimeUnit.HOURS
        )
            .addTag(Constants.WORK_SPLITTER_REMINDER)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            Constants.WORK_SPLITTER_REMINDER,
            ExistingPeriodicWorkPolicy.KEEP,
            splitterReminderRequest
        )
    }
}
