package com.finsave.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.finsave.core.common.Constants
import com.finsave.core.common.prefs.DataStoreManager
import com.finsave.core.common.prefs.PreferencesManager
import com.finsave.domain.model.Transaction
import com.finsave.domain.repository.AccountRepository
import com.finsave.domain.usecase.transaction.GetTransactionsUseCase
import com.finsave.domain.usecase.transaction.GetSpendingSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import com.finsave.domain.repository.TransactionRepository
import android.content.Context
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject

data class SmsImportProgressUi(
    val total: Int,
    val parsed: Int,
    val imported: Int,
    val isRunning: Boolean
)

data class SmsImportCompletionUi(
    val workId: UUID,
    val total: Int,
    val parsed: Int,
    val imported: Int
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val getSpendingSummaryUseCase: GetSpendingSummaryUseCase,
    private val accountRepository: AccountRepository,
    private val budgetRepository: com.finsave.domain.repository.BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val preferencesManager: PreferencesManager,
    private val dataStoreManager: DataStoreManager,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val currentMonth = YearMonth.now()
    private val startOfMonth = currentMonth.atDay(1)
    private val endOfMonth = currentMonth.atEndOfMonth()

    val totalSpendPaise = getSpendingSummaryUseCase.getTotalDebit(startOfMonth, endOfMonth)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val recentTransactions = getTransactionsUseCase.recent(limit = 5)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalBalancePaise = accountRepository.getTotalBalance()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
        
    // Reactive daysRemaining — recalculates on every budget change
    val daysRemaining: StateFlow<Int> = budgetRepository.getActiveBudgets()
        .combine(flow { emit(LocalDate.now(ZoneId.of("Asia/Kolkata"))) }) { _, _ ->
            calculateDaysRemaining().toInt()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), calculateDaysRemaining().toInt())

    // Total budget amount (from the overall budget where categoryId == null)
    val totalBudgetPaise: StateFlow<Long> = budgetRepository.getActiveBudgets()
        .map { budgets ->
            budgets.firstOrNull { it.categoryId == null }?.limitAmountPaise ?: 0L
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
    
    val useIndianNumberSystem = MutableStateFlow(
        preferencesManager.getBoolean(com.finsave.core.common.Constants.PREFS_USE_INDIAN_NUMBER_SYSTEM, true)
    ).asStateFlow()
    
    // Check if total budget exists (categoryId == null)
    val hasTotalBudget: StateFlow<Boolean> = budgetRepository.getActiveBudgets()
        .map { budgets -> budgets.any { it.categoryId == null } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    // FinSave Score and Budget Streak
    val finSaveScore: StateFlow<Int> = dataStoreManager.finSaveScore
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    
    val budgetStreakCount: StateFlow<Int> = dataStoreManager.budgetStreak
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val bestStreak: StateFlow<Int> = dataStoreManager.bestStreak
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // A streak is considered broken if the last broken date was yesterday (relative to today).
    // Or if current streak is 0 and it was broken today or yesterday.
    val isStreakBroken: StateFlow<Boolean> = flow {
        val lastBrokenStr = preferencesManager.getString(Constants.PREFS_LAST_STREAK_BROKEN_DATE, "")
        val broken = if (lastBrokenStr.isEmpty()) false
        else {
            try {
                val lastBrokenDate = LocalDate.parse(lastBrokenStr)
                val today = LocalDate.now(ZoneId.of("Asia/Kolkata"))
                val diff = java.time.temporal.ChronoUnit.DAYS.between(lastBrokenDate, today)
                diff == 1L || diff == 0L
            } catch (e: Exception) {
                false
            }
        }
        emit(broken)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val showFirstTransactionConfetti: StateFlow<Boolean> = combine(
        transactionRepository.getTransactionCount(),
        preferencesManager.getBooleanFlow(Constants.PREFS_HAS_SHOWN_FIRST_TX_CONFETTI, false)
    ) { count, hasShown ->
        count == 1 && !hasShown
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun markConfettiShown() {
        preferencesManager.setBoolean(Constants.PREFS_HAS_SHOWN_FIRST_TX_CONFETTI, true)
    }

    val smsImportProgress: StateFlow<SmsImportProgressUi?> =
        WorkManager.getInstance(appContext)
            .getWorkInfosForUniqueWorkFlow(Constants.WORK_SMS_MANUAL_IMPORT)
            .map { infos ->
                val activeWork = infos.firstOrNull { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
                if (activeWork != null) {
                    activeWork.toSmsImportProgress()
                } else {
                    null
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val smsImportCompletion: StateFlow<SmsImportCompletionUi?> =
        WorkManager.getInstance(appContext)
            .getWorkInfosForUniqueWorkFlow(Constants.WORK_SMS_MANUAL_IMPORT)
            .map { infos ->
                val completion = infos.firstOrNull { it.state == WorkInfo.State.SUCCEEDED }?.toCompletionSummary()
                val lastShown = preferencesManager.getString(Constants.PREFS_LAST_SMS_IMPORT_COMPLETION_ID, "")
                if (completion != null && completion.workId.toString() != lastShown) {
                    completion
                } else {
                    null
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun markSmsImportCompletionShown(workId: UUID) {
        preferencesManager.setString(Constants.PREFS_LAST_SMS_IMPORT_COMPLETION_ID, workId.toString())
    }

    private fun WorkInfo.toSmsImportProgress(): SmsImportProgressUi {
        val total = progress.getInt(Constants.PROGRESS_SMS_TOTAL, 0)
        val parsed = progress.getInt(Constants.PROGRESS_SMS_PARSED, 0)
        val imported = progress.getInt(Constants.PROGRESS_SMS_IMPORTED, 0)
        return SmsImportProgressUi(
            total = total,
            parsed = parsed,
            imported = imported,
            isRunning = state == WorkInfo.State.RUNNING
        )
    }

    private fun WorkInfo.toCompletionSummary(): SmsImportCompletionUi {
        val total = outputData.getInt(Constants.PROGRESS_SMS_TOTAL, 0)
        val parsed = outputData.getInt(Constants.PROGRESS_SMS_PARSED, 0)
        val imported = outputData.getInt(Constants.PROGRESS_SMS_IMPORTED, 0)
        return SmsImportCompletionUi(
            workId = id,
            total = total,
            parsed = parsed,
            imported = imported
        )
    }

    private fun calculateDaysRemaining(): Long {
        val today = LocalDate.now(ZoneId.of("Asia/Kolkata"))
        val end = YearMonth.now(ZoneId.of("Asia/Kolkata")).atEndOfMonth()
        return ChronoUnit.DAYS.between(today, end).coerceAtLeast(0)
    }
}
