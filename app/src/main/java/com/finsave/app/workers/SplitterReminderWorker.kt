package com.finsave.app.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.finsave.core.common.Constants
import com.finsave.core.common.notifications.NotificationHelper
import com.finsave.core.common.prefs.PreferencesManager
import com.finsave.domain.repository.SplitterRepository
import com.finsave.domain.usecase.splitter.SimplifyDebtsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * SplitterReminderWorker — posts reminders for unsettled group debts above ₹500
 * that are older than 7 days.
 *
 * Scheduled as a [androidx.work.PeriodicWorkRequest] with a 24-hour interval,
 * using tag [Constants.WORK_SPLITTER_REMINDER].
 *
 * Responsibilities:
 * 1. Query all active (non-settled, non-archived) groups
 * 2. For each group older than 7 days, compute simplified debts
 * 3. Filter debts by amount ≥ ₹500 (50,000 paise)
 * 4. Post at most one notification per group per day
 *
 * Notification throttling:
 * - Uses PreferencesManager keyed by "splitter_reminder_${groupId}_${today}"
 * - Prevents duplicate notifications for the same group on the same day
 */
@HiltWorker
class SplitterReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val splitterRepository: SplitterRepository,
    private val simplifyDebtsUseCase: SimplifyDebtsUseCase,
    private val preferencesManager: PreferencesManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Use IST timezone for all date calculations
            val istZone = ZoneId.of("Asia/Kolkata")
            val today = LocalDate.now(istZone)

            // Query all active groups (not settled, not archived)
            val activeGroups = splitterRepository.getActiveGroups().first()

            activeGroups.forEach { group ->
                // Check if group is older than SPLITTER_REMINDER_DAYS (7 days)
                val groupCreatedDate = group.createdAt.toLocalDate()
                val groupAgeDays = ChronoUnit.DAYS.between(groupCreatedDate, today)

                if (groupAgeDays < Constants.SPLITTER_REMINDER_DAYS) {
                    return@forEach // Skip groups younger than 7 days
                }

                // Check if we've already posted a notification for this group today
                val notificationKey = "splitter_reminder_${group.id}_$today"
                if (preferencesManager.getBoolean(notificationKey, false)) {
                    return@forEach // Already notified for this group today
                }

                // Compute simplified debts for this group
                val members = splitterRepository.getMembersByGroup(group.id).first()
                val expenses = splitterRepository.getExpensesByGroup(group.id).first()

                // Build the data structure for SimplifyDebtsUseCase.calculateNetBalances
                val expensePairs = expenses.map { expense ->
                    expense.paidByMemberId to expense.amountPaise
                }

                val splitsMap = mutableMapOf<Long, List<Pair<Long, Long>>>()
                expenses.forEachIndexed { index, expense ->
                    val splits = splitterRepository.getSplitsByExpense(expense.id).first()
                    splitsMap[index.toLong()] = splits.map { split ->
                        split.memberId to split.amountPaise
                    }
                }

                // Calculate net balances and simplify debts
                val memberBalances = simplifyDebtsUseCase.calculateNetBalances(
                    members = members,
                    expenses = expensePairs,
                    splits = splitsMap
                )

                val simplifiedDebts = simplifyDebtsUseCase(memberBalances)

                // Filter debts by threshold (₹500 = 50,000 paise)
                val significantDebts = simplifiedDebts.filter { debt ->
                    debt.amountPaise >= Constants.SPLITTER_REMINDER_THRESHOLD_PAISE
                }

                // Post notifications for significant debts
                if (significantDebts.isNotEmpty()) {
                    // Post one notification per debt (but we'll mark the group as notified)
                    significantDebts.forEach { debt ->
                        NotificationHelper.postSplitterReminder(
                            context = context,
                            fromMemberName = debt.fromMember.name,
                            toMemberName = debt.toMember.name,
                            amountPaise = debt.amountPaise,
                            groupName = group.name,
                            notificationId = debt.hashCode()
                        )
                    }

                    // Mark this group as notified for today
                    preferencesManager.setBoolean(notificationKey, true)
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            // Don't retry on failure — reminders are best-effort
            Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "SplitterReminderWorker"
    }
}
