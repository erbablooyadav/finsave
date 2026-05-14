package com.finsave.data.repository

import androidx.room.withTransaction
import com.finsave.data.local.FinSaveDatabase
import com.finsave.data.local.dao.AccountDao
import com.finsave.data.local.dao.TransactionDao
import com.finsave.data.mapper.toDomain
import com.finsave.data.mapper.toEntity
import com.finsave.domain.model.Transaction
import com.finsave.domain.model.TransactionType
import com.finsave.domain.repository.TransactionRepository
import com.finsave.core.common.extensions.toEpochMillis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val db: FinSaveDatabase,
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao
) : TransactionRepository {

    override fun getAllTransactions(): Flow<List<Transaction>> =
        transactionDao.getAllTransactions().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getTransactionCount(): Flow<Int> = transactionDao.getTransactionCount()

    override fun getTransactionsByDateRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<Transaction>> =
        transactionDao.getTransactionsByDateRange(
            startDate.toEpochMillis(),
            endDate.toEpochMillis()
        ).map { entities -> entities.map { it.toDomain() } }

    override fun getTransactionsByCategory(categoryId: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsByCategory(categoryId)
            .map { entities -> entities.map { it.toDomain() } }

    override fun getTransactionsByAccount(accountId: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsByAccount(accountId)
            .map { entities -> entities.map { it.toDomain() } }

    override fun getTransactionById(id: Long): Flow<Transaction?> =
        transactionDao.getTransactionById(id).map { it?.toDomain() }

    override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> =
        transactionDao.getRecentTransactions(limit)
            .map { entities -> entities.map { it.toDomain() } }

    override fun searchTransactions(query: String): Flow<List<Transaction>> =
        if (query.length >= 2) {
            // Use FTS for better performance on longer queries
            transactionDao.searchTransactionsFts("$query*")
                .map { entities -> entities.map { it.toDomain() } }
        } else {
            // Fallback to LIKE for single character or empty queries
            transactionDao.searchTransactions(query)
                .map { entities -> entities.map { it.toDomain() } }
        }

    override fun getTotalByTypeAndDateRange(
        type: TransactionType,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<Long> =
        transactionDao.getTotalByTypeAndDateRange(
            type.name,
            startDate.toEpochMillis(),
            endDate.toEpochMillis()
        )

    override fun getSpendByCategory(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<com.finsave.domain.model.CategorySpendSummary>> =
        transactionDao.getSpendByCategory(
            startDate.toEpochMillis(),
            endDate.toEpochMillis()
        ).map { list ->
            list.map {
                com.finsave.domain.model.CategorySpendSummary(
                    categoryId = it.category_id,
                    totalSpentPaise = it.total
                )
            }
        }

    override suspend fun insertTransaction(transaction: Transaction): Long =
        db.withTransaction {
            val id = transactionDao.insertTransaction(transaction.toEntity())
            // id == -1L means IGNORE fired (duplicate sms_hash). Skip balance update to
            // avoid double-counting money when the same SMS is processed more than once.
            if (id != -1L) {
                val delta = if (transaction.type == TransactionType.DEBIT)
                    -transaction.amountPaise else transaction.amountPaise
                accountDao.updateBalance(transaction.accountId, delta)
            }
            id
        }

    override suspend fun updateTransaction(transaction: Transaction) =
        db.withTransaction {
            val old = transactionDao.getTransactionByIdOnce(transaction.id)
                ?: return@withTransaction
            // Reverse old balance effect
            val oldDelta = if (old.type == "DEBIT") old.amountPaise else -old.amountPaise
            accountDao.updateBalance(old.accountId, oldDelta)
            // Apply new balance effect
            val newDelta = if (transaction.type == TransactionType.DEBIT)
                -transaction.amountPaise else transaction.amountPaise
            accountDao.updateBalance(transaction.accountId, newDelta)
            transactionDao.updateTransaction(transaction.toEntity())
        }

    override suspend fun deleteTransaction(id: Long) =
        db.withTransaction {
            val entity = transactionDao.getTransactionByIdOnce(id)
                ?: return@withTransaction
            val reversal = if (entity.type == "DEBIT") entity.amountPaise else -entity.amountPaise
            accountDao.updateBalance(entity.accountId, reversal)
            transactionDao.deleteTransaction(id)
        }

    override suspend fun isSmsDuplicate(smsHash: String): Boolean =
        transactionDao.isSmsDuplicate(smsHash)
}
