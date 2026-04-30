package com.finsave.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * Domain model for a financial transaction.
 *
 * Amounts are stored as Long in paise (1 INR = 100 paise)
 * to avoid floating-point precision issues. This is the
 * standard approach for financial calculations.
 */
data class Transaction(
    val id: Long = 0,
    val amountPaise: Long,
    val type: TransactionType,
    val categoryId: Long,
    val accountId: Long,
    val merchantName: String = "",
    val note: String = "",
    val date: LocalDate,
    val upiId: String? = null,
    val isAutoImported: Boolean = false,
    val smsHash: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now()
)

/**
 * Transaction type classification.
 * TRANSFER is net-neutral on budgets (e.g., credit card payment).
 */
enum class TransactionType {
    DEBIT,
    CREDIT,
    TRANSFER
}

/**
 * Expense category with visual metadata.
 */
data class Category(
    val id: Long = 0,
    val name: String,
    val emoji: String,
    val colorHex: String,
    val budgetLimitPaise: Long? = null,
    val sortOrder: Int = 0,
    val isDefault: Boolean = true,
    val isCustom: Boolean = false
)

/**
 * Financial account — mirrors real bank/wallet accounts.
 * Balance is manually maintained; FinSave never connects to banks.
 */
data class Account(
    val id: Long = 0,
    val name: String,
    val type: AccountType,
    val balancePaise: Long = 0,
    val colorHex: String = "#4F46E5",
    val isDefault: Boolean = false,
    val sortOrder: Int = 0
)

enum class AccountType {
    SAVINGS,
    CURRENT,
    CREDIT_CARD,
    WALLET,
    CASH,
    INVESTMENT
}

/**
 * Budget configuration for a category or overall spending.
 */
data class Budget(
    val id: Long = 0,
    val categoryId: Long? = null, // null = total budget
    val limitAmountPaise: Long,
    val periodType: BudgetPeriod,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val rollover: Boolean = false,
    val isActive: Boolean = true
)

enum class BudgetPeriod {
    MONTHLY,
    WEEKLY,
    CUSTOM
}

/**
 * Budget summary with spend tracking.
 */
data class BudgetSummary(
    val budget: Budget,
    val category: Category?,
    val spentPaise: Long,
    val remainingPaise: Long,
    val percentUsed: Float,
    val daysRemaining: Int,
    val dailyBudgetPaise: Long
)

/**
 * Summary of spending per category for insights.
 */
data class CategorySpendSummary(
    val categoryId: Long?,
    val totalSpentPaise: Long
)

/**
 * Returns the [LocalDate] range (start inclusive, end inclusive) for the current
 * budget period, computed in IST (Asia/Kolkata).
 *
 * - [BudgetPeriod.MONTHLY]: first day of the current calendar month → last day of the
 *   current calendar month.
 * - [BudgetPeriod.WEEKLY]: Monday of the current ISO week → Sunday of the current ISO
 *   week.
 * - [BudgetPeriod.CUSTOM]: [Budget.startDate] → [Budget.endDate]. If [Budget.endDate]
 *   is null the start date is used as both bounds (single-day range).
 */
fun Budget.currentPeriodRange(): Pair<LocalDate, LocalDate> {
    val today = LocalDate.now(ZoneId.of("Asia/Kolkata"))
    return when (periodType) {
        BudgetPeriod.MONTHLY -> {
            val start = today.with(TemporalAdjusters.firstDayOfMonth())
            val end = today.with(TemporalAdjusters.lastDayOfMonth())
            start to end
        }
        BudgetPeriod.WEEKLY -> {
            val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val end = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            start to end
        }
        BudgetPeriod.CUSTOM -> {
            startDate to (endDate ?: startDate)
        }
    }
}
