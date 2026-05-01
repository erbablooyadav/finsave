package com.finsave.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.finsave.app.workers.DailyDigestWorker
import com.finsave.app.workers.SplitterReminderWorker
import com.finsave.core.common.Constants
import com.finsave.domain.usecase.sync.SyncManager
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var syncManager: SyncManager

    override fun onReceive(context: Context, intent: Intent) {
        if (
            intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == ACTION_QUICKBOOT_POWERON
        ) {
            syncManager.startPeriodicSync()
            scheduleDailyDigest(context)
            scheduleSplitterReminder(context)
        }
    }

    private fun scheduleDailyDigest(context: Context) {
        val initialDelay = DailyDigestWorker.computeDelayUntil2200Ist()
        val dailyDigestRequest = PeriodicWorkRequestBuilder<DailyDigestWorker>(
            24, TimeUnit.HOURS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .addTag(Constants.WORK_DAILY_DIGEST)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            Constants.WORK_DAILY_DIGEST,
            ExistingPeriodicWorkPolicy.KEEP,
            dailyDigestRequest
        )
    }

    private fun scheduleSplitterReminder(context: Context) {
        val splitterReminderRequest = PeriodicWorkRequestBuilder<SplitterReminderWorker>(
            24, TimeUnit.HOURS
        )
            .addTag(Constants.WORK_SPLITTER_REMINDER)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            Constants.WORK_SPLITTER_REMINDER,
            ExistingPeriodicWorkPolicy.KEEP,
            splitterReminderRequest
        )
    }

    private companion object {
        const val ACTION_QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON"
    }
}
