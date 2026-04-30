package com.finsave.app.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.finsave.core.common.Constants
import com.finsave.core.common.notifications.NotificationHelper
import com.finsave.core.common.prefs.PreferencesManager
import com.finsave.domain.model.TransactionType
import com.finsave.domain.model.currentPeriodRange
import com.finsave.domain.repository.BudgetRepository
import com.finsave.domain.repository.TransactionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/**
 * DailyDigestWorker — posts a nightly summary of the day's spending and updates streak/score.
 *
 * Scheduled as a [androidx.work.PeriodicWorkRequest] with a 24-hour interval,
 * constrained to fire at 22:00 IST (UTC+5:30) each day, using tag [Constants.WORK_DAILY_DIGEST].
 *
 * Responsibilities:
 * 1. Query all DEBIT transactions for the current calendar day (IST)
 * 2. Compute total spend and transaction count
 * 3. Post notification via [NotificationHelper.postDailyDigest] if spend > 0
 * 4. Update budget streak and FinSave score via [updateStreakAndScore]
 *
 * Streak logic:
 * - Increment by 1 if today's spending is under the total budget limit
 * - Reset to 0 if today's spending exceeds the total budget limit
 *
 * Score calculation:
 * - score = clamp(round((streakDays × 2) + (budgetAdherencePercent × 0.5)), 0, 100)
 * - budgetAdherencePercent = (count of budgets under limit / total active budgets) × 100
 */
@HiltWorker
class DailyDigestWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val preferencesManager: PreferencesManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Use IST timezone for all date calculations
            val istZone = ZoneId.of("Asia/Kolkata")
            val today = LocalDate.now(istZone)

            // Query all DEBIT transactions for today
            val todayTransactions = transactionRepository
                .getTransactionsByDateRange(today, today)
                .first()
                .filter { it.type == TransactionType.DEBIT }

            val totalSpent = todayTransactions.sumOf { it.amountPaise }
            val txCount = todayTransactions.size

            // Post notification only if spend > 0
            if (totalSpent > 0) {
                NotificationHelper.postDailyDigest(context, totalSpent, txCount)
            }

            // Update streak and score
            updateStreakAndScore(today, totalSpent)

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            // Don't retry on failure — daily digest is best-effort
            Result.failure()
        }
    }

    /**
     * Updates the budget streak count and FinSave score based on today's spending.
     *
     * Streak logic:
     * - If no total budget exists, streak remains unchanged
     * - If today's spending <= total budget limit, increment streak by 1
     * - If today's spending > total budget limit, reset streak to 0
     *
     * Score calculation:
     * - score = clamp(round((streakDays × 2) + (budgetAdherencePercent × 0.5)), 0, 100)
     * - budgetAdherencePercent = (budgets under limit / total budgets) × 100
     */
    private suspend fun updateStreakAndScore(today: LocalDate, todaySpentPaise: Long) {
        // Get all active budgets
        val activeBudgets = budgetRepository.getActiveBudgets().first()

        // Find the total budget (categoryId == null)
        val totalBudget = activeBudgets.firstOrNull { it.categoryId == null }

        // Update streak
        val currentStreak = preferencesManager.getInt(Constants.PREFS_BUDGET_STREAK_COUNT, 0)
        val newStreak = if (totalBudget != null) {
            // Check if today's spending exceeds the total budget
            val streakBroken = todaySpentPaise > totalBudget.limitAmountPaise
            if (streakBroken) 0 else currentStreak + 1
        } else {
            // No total budget configured — keep current streak
            currentStreak
        }

        preferencesManager.setInt(Constants.PREFS_BUDGET_STREAK_COUNT, newStreak)
        preferencesManager.setString(Constants.PREFS_LAST_STREAK_DATE, today.toString())

        // Calculate budget adherence percentage
        val budgetAdherencePercent = if (activeBudgets.isEmpty()) {
            0f
        } else {
            val budgetsUnderLimit = activeBudgets.count { budget ->
                val (start, end) = budget.currentPeriodRange()
                val categoryId = budget.categoryId

                val spent = if (categoryId != null) {
                    // Category-specific budget
                    transactionRepository.getTransactionsByCategory(categoryId)
                        .first()
                        .filter { it.date in start..end && it.type == TransactionType.DEBIT }
                        .sumOf { it.amountPaise }
                } else {
                    // Total budget
                    transactionRepository.getTotalByTypeAndDateRange(
                        TransactionType.DEBIT,
                        start,
                        end
                    ).first()
                }

                spent < budget.limitAmountPaise
            }
            (budgetsUnderLimit.toFloat() / activeBudgets.size) * 100f
        }

        // Calculate FinSave Score
        val score = calculateFinSaveScore(newStreak, budgetAdherencePercent)
        preferencesManager.setInt(Constants.PREFS_FINSAVE_SCORE, score)
    }

    /**
     * Calculates the FinSave Score using the formula:
     * score = clamp(round((streakDays × 2) + (budgetAdherencePercent × 0.5)), 0, 100)
     */
    private fun calculateFinSaveScore(streakDays: Int, budgetAdherencePercent: Float): Int {
        val raw = (streakDays * 2) + (budgetAdherencePercent * 0.5f)
        return raw.roundToInt().coerceIn(0, 100)
    }

    companion object {
        const val WORK_NAME = "DailyDigestWorker"

        /**
         * Computes the delay in milliseconds from now until the next 22:00 IST.
         * Used to schedule the initial delay for the PeriodicWorkRequest.
         */
        fun computeDelayUntil2200Ist(): Long {
            val istZone = ZoneId.of("Asia/Kolkata")
            val now = LocalDateTime.now(istZone)
            var target = now.toLocalDate().atTime(22, 0)

            // If it's already past 22:00 today, schedule for 22:00 tomorrow
            if (now.isAfter(target)) {
                target = target.plusDays(1)
            }

            return ChronoUnit.MILLIS.between(now, target)
        }
    }
}
