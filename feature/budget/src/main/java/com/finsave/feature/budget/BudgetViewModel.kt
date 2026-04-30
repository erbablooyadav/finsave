package com.finsave.feature.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finsave.domain.model.Budget
import com.finsave.domain.model.BudgetPeriod
import com.finsave.domain.model.currentPeriodRange
import com.finsave.domain.repository.BudgetRepository
import com.finsave.domain.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val preferencesManager: com.finsave.core.common.prefs.PreferencesManager
) : ViewModel() {

    private val currentMonth = YearMonth.now()
    private val startOfMonth = currentMonth.atDay(1)
    private val endOfMonth = currentMonth.atEndOfMonth()

    val budgetSummaries = budgetRepository.getBudgetSummaries(startOfMonth, endOfMonth)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _dialogBudget = MutableStateFlow<Budget?>(null)
    val dialogBudget: StateFlow<Budget?> = _dialogBudget.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    val useIndianNumberSystem = MutableStateFlow(
        preferencesManager.getBoolean(com.finsave.core.common.Constants.PREFS_USE_INDIAN_NUMBER_SYSTEM, true)
    ).asStateFlow()

    fun createDefaultBudget() {
        viewModelScope.launch {
            val totalBudget = Budget(
                categoryId = null,
                limitAmountPaise = 20000_00L, // 20,000 INR
                periodType = BudgetPeriod.MONTHLY,
                startDate = startOfMonth,
                endDate = endOfMonth,
                isActive = true
            )
            budgetRepository.insertBudget(totalBudget)
        }
    }

    /**
     * Opens the dialog to create a new budget.
     * Requirement: 6.2
     */
    fun onAddBudgetClick() {
        _dialogBudget.value = Budget(
            categoryId = null,
            limitAmountPaise = 0L,
            periodType = BudgetPeriod.MONTHLY,
            startDate = startOfMonth,
            endDate = endOfMonth,
            isActive = true
        )
        _errorMessage.value = null
    }

    /**
     * Opens the dialog to edit an existing budget.
     * Requirement: 6.7
     */
    fun onEditBudget(budget: Budget) {
        _dialogBudget.value = budget
        _errorMessage.value = null
    }

    /**
     * Closes the budget dialog.
     */
    fun onDismissDialog() {
        _dialogBudget.value = null
        _errorMessage.value = null
    }

    /**
     * Adds a new budget with validation.
     * Requirements: 6.4, 6.5, 6.6
     */
    fun onAddBudget(budget: Budget) {
        viewModelScope.launch {
            try {
                // Validate: limit amount > 0 (Requirement 6.5)
                if (budget.limitAmountPaise <= 0) {
                    _errorMessage.value = "Budget amount must be greater than zero"
                    return@launch
                }

                // Validate: no duplicate category+period (Requirement 6.6)
                val existingBudgets = budgetRepository.getActiveBudgets().first()
                val hasDuplicate = existingBudgets.any { existing ->
                    existing.categoryId == budget.categoryId &&
                    existing.periodType == budget.periodType &&
                    periodsOverlap(existing, budget)
                }

                if (hasDuplicate) {
                    val scopeName = if (budget.categoryId == null) {
                        "this scope"
                    } else {
                        "this category"
                    }
                    _errorMessage.value = "A budget for $scopeName already exists"
                    return@launch
                }

                // Insert budget
                budgetRepository.insertBudget(budget)
                _dialogBudget.value = null
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to create budget"
            }
        }
    }

    /**
     * Updates an existing budget.
     * Requirement: 6.8
     */
    fun onUpdateBudget(budget: Budget) {
        viewModelScope.launch {
            try {
                // Validate: limit amount > 0
                if (budget.limitAmountPaise <= 0) {
                    _errorMessage.value = "Budget amount must be greater than zero"
                    return@launch
                }

                // Validate: no duplicate category+period (excluding self)
                val existingBudgets = budgetRepository.getActiveBudgets().first()
                val hasDuplicate = existingBudgets.any { existing ->
                    existing.id != budget.id &&
                    existing.categoryId == budget.categoryId &&
                    existing.periodType == budget.periodType &&
                    periodsOverlap(existing, budget)
                }

                if (hasDuplicate) {
                    val scopeName = if (budget.categoryId == null) {
                        "this scope"
                    } else {
                        "this category"
                    }
                    _errorMessage.value = "A budget for $scopeName already exists"
                    return@launch
                }

                // Update budget
                budgetRepository.updateBudget(budget)
                _dialogBudget.value = null
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to update budget"
            }
        }
    }

    /**
     * Deletes a budget.
     * Requirement: 6.9
     */
    fun onDeleteBudget(budget: Budget) {
        viewModelScope.launch {
            try {
                budgetRepository.deleteBudget(budget.id)
                _dialogBudget.value = null
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to delete budget"
            }
        }
    }

    fun onClearError() {
        _errorMessage.value = null
    }

    /**
     * Checks if two budgets have overlapping periods.
     */
    private fun periodsOverlap(budget1: Budget, budget2: Budget): Boolean {
        val (start1, end1) = budget1.currentPeriodRange()
        val (start2, end2) = budget2.currentPeriodRange()
        
        return !(end1.isBefore(start2) || end2.isBefore(start1))
    }
}
