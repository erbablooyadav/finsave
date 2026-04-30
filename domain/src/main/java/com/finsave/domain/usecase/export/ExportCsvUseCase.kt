package com.finsave.domain.usecase.export

import android.content.Context
import com.finsave.core.common.export.CsvExporter
import com.finsave.core.common.export.TransactionCsvRow
import com.finsave.domain.repository.AccountRepository
import com.finsave.domain.repository.CategoryRepository
import com.finsave.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Use case: Export all transactions to CSV.
 * Wraps CsvExporter.export.
 * Requirements: 11.5, 11.7
 */
class ExportCsvUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val csvExporter: CsvExporter
) {
    suspend operator fun invoke(context: Context): Result<Unit> {
        return try {
            // Fetch all data
            val transactions = transactionRepository.getAllTransactions().first()
                .sortedByDescending { it.date }
            val categories = categoryRepository.getAllCategories().first()
                .associateBy({ it.id }, { it.name })
            val accounts = accountRepository.getAllAccounts().first()
                .associateBy({ it.id }, { it.name })

            // Convert to CSV rows
            val csvRows = transactions.map { tx ->
                TransactionCsvRow(
                    date = tx.date,
                    type = tx.type.name,
                    categoryId = tx.categoryId,
                    accountId = tx.accountId,
                    merchantName = tx.merchantName,
                    amountPaise = tx.amountPaise,
                    note = tx.note
                )
            }

            // Export
            csvExporter.export(context, csvRows, categories, accounts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
