package com.finsave.app.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.finsave.core.common.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Data

@Singleton
class SyncManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : com.finsave.domain.usecase.sync.SyncManager {

    override fun startPeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED) // Works offline
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SmsSyncWorker>(
            15, TimeUnit.MINUTES, // Minimum interval allowed by Android
            5, TimeUnit.MINUTES // Flex interval
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SmsSyncWorker.WORK_NAME,
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
    
    override fun stopSync() {
        WorkManager.getInstance(context).cancelUniqueWork(SmsSyncWorker.WORK_NAME)
    }

    override fun triggerManualSync(sinceTimestamp: Long) {
        val inputData = Data.Builder()
            .putLong("sinceTimestamp", sinceTimestamp)
            .build()
            
        val workRequest = OneTimeWorkRequestBuilder<SmsSyncWorker>()
            .addTag(Constants.WORK_SMS_MANUAL_IMPORT)
            .setInputData(inputData)
            .build()
            
        WorkManager.getInstance(context).enqueueUniqueWork(
            Constants.WORK_SMS_MANUAL_IMPORT,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
}
