package com.finsave.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finsave.domain.model.Transaction
import com.finsave.domain.repository.AccountRepository
import com.finsave.domain.usecase.transaction.GetTransactionsUseCase
import com.finsave.domain.usecase.transaction.GetSpendingSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val getSpendingSummaryUseCase: GetSpendingSummaryUseCase,
    private val accountRepository: AccountRepository,
    private val budgetRepository: com.finsave.domain.repository.BudgetRepository,
    private val preferencesManager: com.finsave.core.common.prefs.PreferencesManager
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
}
