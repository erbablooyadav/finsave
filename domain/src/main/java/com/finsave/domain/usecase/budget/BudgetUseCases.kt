package com.finsave.domain.usecase.budget

import com.finsave.domain.model.Budget
import com.finsave.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Use case: Add a new budget.
 * Validates: limitAmount > 0, no duplicate category+period.
 * Requirements: 6.5, 6.6
 */
class AddBudgetUseCase @Inject constructor(
    private val repository: BudgetRepository
) {
    suspend operator fun invoke(budget: Budget): Result<Long> {
        // Validation: limit amount > 0
        if (budget.limitAmountPaise <= 0) {
            return Result.failure(IllegalArgumentException("Budget amount must be greater than zero"))
        }

        // Validation: no duplicate category+period
        // Check if a budget already exists for this category
        if (budget.categoryId != null) {
            val existingBudget = repository.getBudgetForCategory(budget.categoryId).first()
            if (existingBudget != null && existingBudget.isActive) {
                return Result.failure(IllegalStateException("A budget for this category already exists"))
            }
        } else {
            // Check for duplicate total budget
            val allBudgets = repository.getActiveBudgets().first()
            val totalBudgetExists = allBudgets.any { it.categoryId == null }
            if (totalBudgetExists) {
                return Result.failure(IllegalStateException("A budget for this category already exists"))
            }
        }

        return try {
            val id = repository.insertBudget(budget)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Use case: Update an existing budget.
 * Requirements: 6.8
 */
class UpdateBudgetUseCase @Inject constructor(
    private val repository: BudgetRepository
) {
    suspend operator fun invoke(budget: Budget): Result<Unit> {
        // Validation: limit amount > 0
        if (budget.limitAmountPaise <= 0) {
            return Result.failure(IllegalArgumentException("Budget amount must be greater than zero"))
        }

        return try {
            repository.updateBudget(budget)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Use case: Delete a budget.
 * Requirements: 6.9
 */
class DeleteBudgetUseCase @Inject constructor(
    private val repository: BudgetRepository
) {
    suspend operator fun invoke(id: Long): Result<Unit> {
        return try {
            repository.deleteBudget(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
