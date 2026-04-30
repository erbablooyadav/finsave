package com.finsave.data.repository

import com.finsave.data.local.dao.BudgetDao
import com.finsave.data.local.dao.CategoryDao
import com.finsave.data.local.dao.TransactionDao
import com.finsave.data.mapper.toDomain
import com.finsave.data.mapper.toEntity
import com.finsave.domain.model.Budget
import com.finsave.domain.model.BudgetSummary
import com.finsave.domain.repository.BudgetRepository
import com.finsave.core.common.extensions.toEpochMillis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao
) : BudgetRepository {

    override fun getAllBudgets(): Flow<List<Budget>> =
        budgetDao.getAllBudgets().map { list -> list.map { it.toDomain() } }

    override fun getActiveBudgets(): Flow<List<Budget>> =
        budgetDao.getActiveBudgets().map { list -> list.map { it.toDomain() } }

    override fun getBudgetForCategory(categoryId: Long): Flow<Budget?> =
        budgetDao.getBudgetForCategory(categoryId).map { it?.toDomain() }

    override fun getBudgetSummaries(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<BudgetSummary>> {
        val activeBudgetsFlow = getActiveBudgets()
        val categoriesFlow = categoryDao.getAllCategories().map { list -> list.map { it.toDomain() } }
        val spendFlow = transactionDao.getSpendByCategory(
            startDate.toEpochMillis(),
            endDate.toEpochMillis()
        )

        return combine(activeBudgetsFlow, categoriesFlow, spendFlow) { budgets, categories, spends ->
            val spendMap = spends.associateBy { it.category_id }
            val categoryMap = categories.associateBy { it.id }
            
            val daysInPeriod = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
            val daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), endDate).toInt().coerceAtLeast(0)

            budgets.map { budget ->
                val spent = if (budget.categoryId == null) {
                    // Total budget
                    spends.sumOf { it.total }
                } else {
                    spendMap[budget.categoryId]?.total ?: 0L
                }

                val category = budget.categoryId?.let { categoryMap[it] }
                val remaining = (budget.limitAmountPaise - spent).coerceAtLeast(0)
                val percentUsed = if (budget.limitAmountPaise > 0) (spent.toFloat() / budget.limitAmountPaise) else 0f
                val dailyBudget = if (daysRemaining > 0) remaining / daysRemaining else 0L

                BudgetSummary(
                    budget = budget,
                    category = category,
                    spentPaise = spent,
                    remainingPaise = remaining,
                    percentUsed = percentUsed,
                    daysRemaining = daysRemaining,
                    dailyBudgetPaise = dailyBudget
                )
            }
        }
    }

    override suspend fun insertBudget(budget: Budget): Long =
        budgetDao.insertBudget(budget.toEntity())

    override suspend fun updateBudget(budget: Budget) =
        budgetDao.updateBudget(budget.toEntity())

    override suspend fun deleteBudget(id: Long) =
        budgetDao.deleteBudget(id)
}
