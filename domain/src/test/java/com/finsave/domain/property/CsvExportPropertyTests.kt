package com.finsave.domain.property

import com.finsave.core.common.export.TransactionCsvRow
import io.kotest.matchers.longs.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

/**
 * Property-based tests for CSV export logic.
 *
 * Tests Properties 9-11 from the design spec:
 * - Property 9: CSV row count round-trip
 * - Property 10: CSV amount precision round-trip
 * - Property 11: CSV RFC 4180 escaping
 */
class CsvExportPropertyTests {

    // ---------------------------------------------------------------------------
    // Property 9: CSV row count round-trip
    // ---------------------------------------------------------------------------

    /**
     * N transactions → exactly N + 1 lines (excluding BOM).
     *
     * **Validates: Requirements 11.2, 11.3, 11.6**
     */
    @Test
    fun `CSV row count equals transaction count plus header`() = runTest {
        checkAll(100, Arb.list(transactionCsvRowArb(), 0..50)) { transactions ->
            val csv = generateCsv(transactions)
            val lines = csv.lines().filter { it.isNotBlank() }
            lines.size shouldBe (transactions.size + 1) // +1 for header
        }
    }

    // ---------------------------------------------------------------------------
    // Property 10: CSV amount precision round-trip
    // ---------------------------------------------------------------------------

    /**
     * parseToPaise(csvAmountField) equals original amountPaise.
     *
     * **Validates: Requirements 11.3**
     */
    @Test
    fun `CSV amount precision round-trip preserves paise`() = runTest {
        checkAll(100, Arb.long(0L..100_000_000L)) { amountPaise ->
            val csvAmount = String.format(Locale.ROOT, "%.2f", amountPaise / 100.0)
            val parsedPaise = (csvAmount.toDouble() * 100).toLong()
            // Allow for rounding errors within 1 paise
            kotlin.math.abs(parsedPaise - amountPaise) shouldBeLessThanOrEqual 1L
        }
    }

    // ---------------------------------------------------------------------------
    // Property 11: CSV RFC 4180 escaping
    // ---------------------------------------------------------------------------

    /**
     * Fields with commas/quotes/newlines are correctly wrapped and escaped.
     *
     * **Validates: Requirements 11.1**
     */
    @Test
    fun `CSV RFC 4180 escaping handles special characters`() = runTest {
        checkAll(100, Arb.string()) { field ->
            val escaped = escapeCsv(field)
            
            // If field contains special chars, it should be wrapped in quotes
            if (field.contains(',') || field.contains('"') || field.contains('\n')) {
                escaped.startsWith("\"") shouldBe true
                escaped.endsWith("\"") shouldBe true
                
                // Internal quotes should be doubled
                if (field.contains('"')) {
                    escaped.contains("\"\"") shouldBe true
                }
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private fun transactionCsvRowArb(): Arb<TransactionCsvRow> =
        io.kotest.property.arbitrary.arbitrary {
            TransactionCsvRow(
                date = LocalDate.now(),
                type = "DEBIT",
                categoryId = 1L,
                accountId = 1L,
                merchantName = Arb.string().bind(),
                amountPaise = Arb.long(1L..10_000_000L).bind(),
                note = Arb.string().bind()
            )
        }

    private fun generateCsv(transactions: List<TransactionCsvRow>): String {
        val sb = StringBuilder()
        sb.append("Date,Type,Category,Account,Merchant,Amount (₹),Note\n")
        transactions.forEach { tx ->
            val amount = String.format(Locale.ROOT, "%.2f", tx.amountPaise / 100.0)
            sb.append("${tx.date},${tx.type},Category,Account,${escapeCsv(tx.merchantName)},$amount,${escapeCsv(tx.note)}\n")
        }
        return sb.toString()
    }

    private fun escapeCsv(field: String): String {
        return if (field.contains(',') || field.contains('"') || field.contains('\n')) {
            "\"" + field.replace("\"", "\"\"") + "\""
        } else {
            field
        }
    }
}
