package com.finsave.domain.usecase.export

import android.content.Context
import com.finsave.core.common.export.CategoryPdfRow
import com.finsave.core.common.export.MerchantPdfRow
import com.finsave.core.common.export.PdfExporter
import com.finsave.core.common.export.PdfReportData
import com.finsave.domain.model.TransactionType
import com.finsave.domain.repository.CategoryRepository
import com.finsave.domain.repository.TransactionRepository
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ExportPdfUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val pdfExporter: PdfExporter
) {
    suspend operator fun invoke(context: Context): Result<Unit> {
        return try {
            val istZone = ZoneId.of("Asia/Kolkata")
            val month = YearMonth.now(istZone)
            val startDate = month.atDay(1)
            val endDate = month.atEndOfMonth()

            val transactions = transactionRepository.getTransactionsByDateRange(startDate, endDate).first()
            val categories = categoryRepository.getAllCategories().first().associateBy { it.id }

            val debitTransactions = transactions.filter { it.type == TransactionType.DEBIT }
            val creditTransactions = transactions.filter { it.type == TransactionType.CREDIT }
            val totalDebit = debitTransactions.sumOf { it.amountPaise }
            val totalCredit = creditTransactions.sumOf { it.amountPaise }

            val categoryRows = debitTransactions
                .groupBy { it.categoryId }
                .map { (categoryId, items) ->
                    val category = categories[categoryId]
                    val amount = items.sumOf { it.amountPaise }
                    CategoryPdfRow(
                        emoji = category?.emoji ?: "",
                        name = category?.name ?: "Uncategorized",
                        amountPaise = amount,
                        percent = if (totalDebit > 0) (amount.toFloat() / totalDebit) * 100f else 0f
                    )
                }
                .sortedByDescending { it.amountPaise }
                .take(10)

            val merchantRows = debitTransactions
                .filter { it.merchantName.isNotBlank() }
                .groupBy { it.merchantName.trim() }
                .map { (merchantName, items) ->
                    MerchantPdfRow(
                        name = merchantName,
                        amountPaise = items.sumOf { it.amountPaise },
                        count = items.size
                    )
                }
                .sortedByDescending { it.amountPaise }
                .take(5)

            val reportData = PdfReportData(
                monthYear = month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                totalDebitPaise = totalDebit,
                totalCreditPaise = totalCredit,
                categoryBreakdown = categoryRows,
                topMerchants = merchantRows
            )

            pdfExporter.export(context, reportData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
