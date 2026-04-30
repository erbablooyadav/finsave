package com.finsave.app.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.finsave.core.common.notifications.NotificationHelper
import com.finsave.core.common.prefs.PreferencesManager
import com.finsave.domain.model.Budget
import com.finsave.domain.model.TransactionType
import com.finsave.domain.model.currentPeriodRange
import com.finsave.domain.repository.BudgetRepository
import com.finsave.domain.repository.CategoryRepository
import com.finsave.domain.repository.TransactionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * BudgetCheckWorker — evaluates budget thresholds after each transaction insert.
 *
 * Enqueued as a [androidx.work.OneTimeWorkRequest] with tag [WORK_BUDGET_CHECK]
 * from the ViewModel after each successful transaction insert.
 *
 * For each active budget:
 * 1. Computes the current period range using [Budget.currentPeriodRange]
 * 2. Queries total DEBIT spending in that range
 * 3. Checks if spending crosses 80% or 90% thresholds
 * 4. Posts notifications via [NotificationHelper.postBudgetAlert] if threshold crossed
 * 5. Uses [PreferencesManager] to prevent duplicate notifications per period
 *
 * Notification keys are scoped by budget ID, threshold, and period start date to ensure
 * notifications fire once per threshold per period.
 */
@HiltWorker
class BudgetCheckWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val preferencesManager: PreferencesManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Get all active budgets
            val activeBudgets = budgetRepository.getActiveBudgets().first()

            // Check each budget's threshold
            activeBudgets.forEach { budget ->
                checkBudgetThreshold(budget)
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            // Don't retry on failure — budget checks are best-effort
            Result.failure()
        }
    }

    /**
     * Checks if the given budget has crossed 80% or 90% threshold and posts
     * notifications if needed.
     */
    private suspend fun checkBudgetThreshold(budget: Budget) {
        // Compute current period range
        val (start, end) = budget.currentPeriodRange()

        // Query total DEBIT spending in this period
        // If budget has a categoryId, we need to filter by category
        val categoryId = budget.categoryId
        val spent = if (categoryId != null) {
            // Get all transactions for this category in the period
            transactionRepository.getTransactionsByCategory(categoryId)
                .first()
                .filter { it.date in start..end && it.type == TransactionType.DEBIT }
                .sumOf { it.amountPaise }
        } else {
            // Total budget — sum all DEBIT transactions in period
            transactionRepository.getTotalByTypeAndDateRange(
                TransactionType.DEBIT,
                start,
                end
            ).first()
        }

        // Calculate percentage used
        val percentUsed = spent.toFloat() / budget.limitAmountPaise

        // Check thresholds and post notifications
        // Keys are scoped by budget ID, threshold, and period start to prevent duplicates
        val key80 = "budget_${budget.id}_80_${start}"
        val key90 = "budget_${budget.id}_90_${start}"

        // Get category name for notification
        val categoryName = if (categoryId != null) {
            categoryRepository.getCategoryById(categoryId).first()?.name ?: "Unknown"
        } else {
            "Total"
        }

        // Check 90% threshold first (higher priority)
        if (percentUsed >= 0.90f && !preferencesManager.getBoolean(key90, false)) {
            NotificationHelper.postBudgetAlert(
                context = context,
                budgetId = budget.id,
                categoryName = categoryName,
                threshold = 90
            )
            preferencesManager.setBoolean(key90, true)
        }
        // Check 80% threshold
        else if (percentUsed >= 0.80f && !preferencesManager.getBoolean(key80, false)) {
            NotificationHelper.postBudgetAlert(
                context = context,
                budgetId = budget.id,
                categoryName = categoryName,
                threshold = 80
            )
            preferencesManager.setBoolean(key80, true)
        }
    }

    companion object {
        const val WORK_NAME = "BudgetCheckWorker"
    }
}
