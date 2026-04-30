package com.finsave.core.common.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.finsave.core.common.Constants
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

/**
 * CsvExporter — generates and shares a CSV export of all transactions.
 *
 * Format:
 * - UTF-8 with BOM for Excel compatibility
 * - Header row: Date,Type,Category,Account,Merchant,Amount (₹),Note
 * - Data rows sorted by date descending
 * - RFC 4180 compliant (fields with commas/quotes/newlines are wrapped and escaped)
 *
 * Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 11.6, 11.7
 */
class CsvExporter @Inject constructor() {

    /**
     * Generates a CSV file and launches the Android share sheet.
     *
     * @param context Application or activity context
     * @param transactions List of transactions to export (should be pre-sorted by date descending)
     * @param categories Map of category ID to category name
     * @param accounts Map of account ID to account name
     * @return Result.success(Unit) if successful, Result.failure(IOException) on error
     */
    fun export(
        context: Context,
        transactions: List<TransactionCsvRow>,
        categories: Map<Long, String>,
        accounts: Map<Long, String>
    ): Result<Unit> {
        return try {
            val file = File(context.cacheDir, "finsave_export_${System.currentTimeMillis()}.csv")
            file.bufferedWriter(Charsets.UTF_8).use { writer ->
                // Write UTF-8 BOM for Excel compatibility
                writer.write(Constants.CSV_BOM)
                
                // Write header row
                writer.write("Date,Type,Category,Account,Merchant,Amount (₹),Note\n")
                
                // Write data rows
                transactions.forEach { tx ->
                    writer.write(buildRow(tx, categories, accounts))
                }
            }

            // Share via Android share sheet
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = Constants.CSV_MIME_TYPE
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Export Transactions"))
            
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(e)
        }
    }

    /**
     * Builds a single CSV row for a transaction.
     */
    private fun buildRow(
        tx: TransactionCsvRow,
        categories: Map<Long, String>,
        accounts: Map<Long, String>
    ): String {
        val date = tx.date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val type = tx.type
        val category = categories[tx.categoryId] ?: "Uncategorized"
        val account = accounts[tx.accountId] ?: "Unknown"
        val amount = String.format(Locale.ROOT, "%.2f", tx.amountPaise / 100.0)
        
        return "${escapeCsv(date)},${escapeCsv(type)},${escapeCsv(category)}," +
               "${escapeCsv(account)},${escapeCsv(tx.merchantName)},${amount},${escapeCsv(tx.note)}\n"
    }

    /**
     * Escapes a CSV field per RFC 4180.
     * - If the field contains comma, quote, or newline, wrap it in double-quotes
     * - Escape internal double-quotes as ""
     */
    private fun escapeCsv(field: String): String {
        return if (field.contains(',') || field.contains('"') || field.contains('\n')) {
            "\"" + field.replace("\"", "\"\"") + "\""
        } else {
            field
        }
    }
}

/**
 * Simplified transaction row for CSV export.
 * Avoids coupling CsvExporter to the full domain Transaction model.
 */
data class TransactionCsvRow(
    val date: LocalDate,
    val type: String,
    val categoryId: Long,
    val accountId: Long,
    val merchantName: String,
    val amountPaise: Long,
    val note: String
)
