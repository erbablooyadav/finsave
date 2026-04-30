package com.finsave.domain.usecase.transaction

import com.finsave.domain.model.Transaction
import com.finsave.domain.model.TransactionType
import com.finsave.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

/**
 * Use case: Get all transactions with optional filtering.
 */
class GetTransactionsUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(): Flow<List<Transaction>> = repository.getAllTransactions()

    fun byDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<Transaction>> =
        repository.getTransactionsByDateRange(startDate, endDate)

    fun byCategory(categoryId: Long): Flow<List<Transaction>> =
        repository.getTransactionsByCategory(categoryId)

    fun byAccount(accountId: Long): Flow<List<Transaction>> =
        repository.getTransactionsByAccount(accountId)

    fun recent(limit: Int = 5): Flow<List<Transaction>> =
        repository.getRecentTransactions(limit)

    fun search(query: String): Flow<List<Transaction>> =
        repository.searchTransactions(query)
}

/**
 * Use case: Add a new transaction.
 * Validates input before insertion.
 */
class AddTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(transaction: Transaction): Result<Long> {
        // Validation
        if (transaction.amountPaise <= 0) {
            return Result.failure(IllegalArgumentException("Amount must be positive"))
        }

        if (transaction.categoryId <= 0) {
            return Result.failure(IllegalArgumentException("Category is required"))
        }

        if (transaction.accountId <= 0) {
            return Result.failure(IllegalArgumentException("Account is required"))
        }

        return try {
            val id = repository.insertTransaction(transaction)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Use case: Update an existing transaction.
 */
class UpdateTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(transaction: Transaction): Result<Unit> {
        if (transaction.id <= 0) {
            return Result.failure(IllegalArgumentException("Transaction ID is required for update"))
        }

        return try {
            repository.updateTransaction(transaction)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Use case: Delete a transaction.
 */
class DeleteTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(id: Long): Result<Unit> {
        return try {
            repository.deleteTransaction(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Use case: Get spending total for a date range.
 */
class GetSpendingSummaryUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    fun getTotalDebit(startDate: LocalDate, endDate: LocalDate): Flow<Long> =
        repository.getTotalByTypeAndDateRange(TransactionType.DEBIT, startDate, endDate)

    fun getTotalCredit(startDate: LocalDate, endDate: LocalDate): Flow<Long> =
        repository.getTotalByTypeAndDateRange(TransactionType.CREDIT, startDate, endDate)

    fun getCategorySpendSummary(startDate: LocalDate, endDate: LocalDate): Flow<List<com.finsave.domain.model.CategorySpendSummary>> =
        repository.getSpendByCategory(startDate, endDate)
}
