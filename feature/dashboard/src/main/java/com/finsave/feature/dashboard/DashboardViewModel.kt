package com.finsave.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.finsave.core.common.Constants
import com.finsave.core.common.prefs.PreferencesManager
import com.finsave.domain.model.Transaction
import com.finsave.domain.repository.AccountRepository
import com.finsave.domain.usecase.transaction.GetTransactionsUseCase
import com.finsave.domain.usecase.transaction.GetSpendingSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import android.content.Context
import java.time.LocalDate
import java.time.YearMonth
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
    private val preferencesManager: PreferencesManager,
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
        
    val daysRemaining = endOfMonth.dayOfMonth - LocalDate.now().dayOfMonth
    
    val useIndianNumberSystem = MutableStateFlow(
        preferencesManager.getBoolean(com.finsave.core.common.Constants.PREFS_USE_INDIAN_NUMBER_SYSTEM, true)
    ).asStateFlow()
    
    // Check if total budget exists (categoryId == null)
    val hasTotalBudget: StateFlow<Boolean> = budgetRepository.getActiveBudgets()
        .map { budgets -> budgets.any { it.categoryId == null } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    // FinSave Score and Budget Streak
    val finSaveScore: StateFlow<Int> = MutableStateFlow(
        preferencesManager.getInt(com.finsave.core.common.Constants.PREFS_FINSAVE_SCORE, 0)
    ).asStateFlow()
    
    val budgetStreakCount: StateFlow<Int> = MutableStateFlow(
        preferencesManager.getInt(com.finsave.core.common.Constants.PREFS_BUDGET_STREAK_COUNT, 0)
    ).asStateFlow()

    val smsImportProgress: StateFlow<SmsImportProgressUi?> =
        WorkManager.getInstance(appContext)
            .getWorkInfosByTagFlow(Constants.WORK_SMS_MANUAL_IMPORT)
            .map { infos ->
                val running = infos.firstOrNull { it.state == WorkInfo.State.RUNNING }
                if (running != null) {
                    running.toSmsImportProgress()
                } else {
                    null
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val smsImportCompletion: StateFlow<SmsImportCompletionUi?> =
        WorkManager.getInstance(appContext)
            .getWorkInfosByTagFlow(Constants.WORK_SMS_MANUAL_IMPORT)
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
}
