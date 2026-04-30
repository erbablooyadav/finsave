package com.finsave.app.workers

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.finsave.core.common.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BudgetCheckScheduler — responsible for enqueuing [BudgetCheckWorker] after
 * transaction inserts.
 *
 * This class is injected into ViewModels that handle transaction creation/updates
 * to trigger budget threshold checks.
 */
@Singleton
class BudgetCheckScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Enqueues a one-time [BudgetCheckWorker] to evaluate budget thresholds.
     * Safe to call multiple times — WorkManager will queue the work.
     */
    fun scheduleBudgetCheck() {
        val workRequest = OneTimeWorkRequestBuilder<BudgetCheckWorker>()
            .addTag(Constants.WORK_BUDGET_CHECK)
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }
}
