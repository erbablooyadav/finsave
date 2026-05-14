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
    lateinit var syncManager: dagger.Lazy<com.finsave.domain.usecase.sync.SyncManager>

    @Inject
    lateinit var accountRepository: dagger.Lazy<com.finsave.domain.repository.AccountRepository>

    @Inject
    lateinit var categoryRepository: dagger.Lazy<com.finsave.domain.repository.CategoryRepository>

    @Inject
    lateinit var bankPatternConfigProvider: dagger.Lazy<BankPatternConfigProvider>

    private val applicationScope = CoroutineScope(Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        val startTime = System.currentTimeMillis()
        super.onCreate()
        
        // 1. Mandatory Main Thread Work
        NotificationHelper.createChannels(this)
        
        // 2. Deferred Background Work
        applicationScope.launch(Dispatchers.IO) {
            val ioStartTime = System.currentTimeMillis()
            
            // Initialize default database records if missing (Lazy access triggers DB init on IO thread)
            val accounts = accountRepository.get().getAllAccounts().firstOrNull()
            if (accounts.isNullOrEmpty()) {
                accountRepository.get().insertDefaultAccounts()
            }
            
            val categories = categoryRepository.get().getAllCategories().firstOrNull()
            if (categories.isNullOrEmpty()) {
                categoryRepository.get().insertDefaultCategories()
            }
            
            // Warm BankPatternConfig cache
            bankPatternConfigProvider.get().warmCache()
            
            if (BuildConfig.DEBUG) {
                android.util.Log.d("FinSaveStartup", "Background IO init completed in ${System.currentTimeMillis() - ioStartTime}ms")
            }
        }
        
        applicationScope.launch(Dispatchers.Default) {
            val workStartTime = System.currentTimeMillis()
            
            // Start SMS sync on app launch
            syncManager.get().startPeriodicSync()
            
            scheduleDailyDigest()
            scheduleSplitterReminder()
            
            if (BuildConfig.DEBUG) {
                android.util.Log.d("FinSaveStartup", "WorkManager scheduling completed in ${System.currentTimeMillis() - workStartTime}ms")
            }
        }

        if (BuildConfig.DEBUG) {
            android.util.Log.d("FinSaveStartup", "onCreate completed in ${System.currentTimeMillis() - startTime}ms")
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
