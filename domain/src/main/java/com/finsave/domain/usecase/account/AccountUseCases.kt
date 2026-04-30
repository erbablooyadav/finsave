package com.finsave.domain.usecase.account

import com.finsave.domain.model.Account
import com.finsave.domain.repository.AccountRepository
import com.finsave.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Use case: Get all accounts.
 */
class GetAccountsUseCase @Inject constructor(
    private val repository: AccountRepository
) {
    operator fun invoke(): Flow<List<Account>> = repository.getAllAccounts()
}

/**
 * Use case: Add a new account.
 * Validates: name non-empty, openingBalance ≥ 0.
 * Requirements: 4.4, 4.5
 */
class AddAccountUseCase @Inject constructor(
    private val repository: AccountRepository
) {
    suspend operator fun invoke(account: Account): Result<Long> {
        // Validation: name non-empty
        if (account.name.isBlank()) {
            return Result.failure(IllegalArgumentException("Account name is required"))
        }

        // Validation: opening balance ≥ 0
        if (account.balancePaise < 0) {
            return Result.failure(IllegalArgumentException("Opening balance must be non-negative"))
        }

        return try {
            val id = repository.insertAccount(account)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Use case: Update an existing account.
 * Validates same rules as add: name non-empty, balance ≥ 0.
 * Requirements: 4.4, 4.5
 */
class UpdateAccountUseCase @Inject constructor(
    private val repository: AccountRepository
) {
    suspend operator fun invoke(account: Account): Result<Unit> {
        // Validation: name non-empty
        if (account.name.isBlank()) {
            return Result.failure(IllegalArgumentException("Account name is required"))
        }

        // Validation: balance ≥ 0
        if (account.balancePaise < 0) {
            return Result.failure(IllegalArgumentException("Balance must be non-negative"))
        }

        return try {
            repository.updateAccount(account)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Use case: Delete an account.
 * Validates:
 * - No linked transactions (Requirement 4.8)
 * - Account count > 1 (Requirement 4.10)
 */
class DeleteAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(id: Long): Result<Unit> {
        // Check: account count > 1
        val accountCount = accountRepository.getAllAccounts().first().size
        if (accountCount <= 1) {
            return Result.failure(IllegalStateException("At least one account is required"))
        }

        // Check: no linked transactions
        val linkedTransactions = transactionRepository.getTransactionsByAccount(id).first()
        if (linkedTransactions.isNotEmpty()) {
            return Result.failure(IllegalStateException("Cannot delete account with existing transactions"))
        }

        return try {
            accountRepository.deleteAccount(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
