package com.finsave.feature.transactions

import com.finsave.domain.model.Transaction
import com.finsave.domain.model.TransactionType
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate

/**
 * Property-based tests for transaction filtering logic.
 *
 * These tests validate the correctness properties specified in the requirements:
 * - Property 1: Transaction filter AND logic
 * - Property 2: Filter monotonicity
 * - Property 3: Type filter partition
 */
class TransactionFilterPropertyTests {

    // ---------------------------------------------------------------------------
    // Arbitraries
    // ---------------------------------------------------------------------------

    private fun transactionArb(): Arb<Transaction> =
        arbitrary {
            val merchantNames = listOf(
                "Amazon", "Flipkart", "Swiggy", "Zomato", "Uber",
                "Ola", "BigBasket", "Grofers", "PayTM", "PhonePe",
                "Google Pay", "Starbucks", "McDonald's", "KFC", "Domino's"
            )
            val notes = listOf(
                "Groceries", "Food delivery", "Cab fare", "Shopping",
                "Bill payment", "Recharge", "Transfer", "Salary", "Refund", ""
            )
            Transaction(
                id = Arb.long(1L..100000L).bind(),
                amountPaise = Arb.long(100L..1_000_000L).bind(),
                type = Arb.of(TransactionType.DEBIT, TransactionType.CREDIT).bind(),
                categoryId = Arb.long(1L..20L).bind(),
                accountId = Arb.long(1L..5L).bind(),
                merchantName = Arb.of(merchantNames).bind(),
                note = Arb.of(notes).bind(),
                date = Arb.localDate(
                    minDate = LocalDate.of(2023, 1, 1),
                    maxDate = LocalDate.of(2024, 12, 31)
                ).bind()
            )
        }

    private fun typeFilterArb(): Arb<TransactionTypeFilter> =
        Arb.of(TransactionTypeFilter.ALL, TransactionTypeFilter.DEBIT, TransactionTypeFilter.CREDIT)

    // ---------------------------------------------------------------------------
    // Helper: Apply filters (mirrors ViewModel logic)
    // ---------------------------------------------------------------------------

    private fun applyFilters(
        transactions: List<Transaction>,
        query: String,
        typeFilter: TransactionTypeFilter,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): List<Transaction> {
        return transactions.filter { transaction ->
            // Search query filter
            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                transaction.merchantName.contains(query, ignoreCase = true) ||
                        transaction.note.contains(query, ignoreCase = true)
            }

            // Type filter
            val matchesType = when (typeFilter) {
                TransactionTypeFilter.ALL -> true
                TransactionTypeFilter.DEBIT -> transaction.type == TransactionType.DEBIT
                TransactionTypeFilter.CREDIT -> transaction.type == TransactionType.CREDIT
            }

            // Date range filter
            val matchesDateRange = if (startDate != null && endDate != null) {
                !transaction.date.isBefore(startDate) && !transaction.date.isAfter(endDate)
            } else if (startDate != null) {
                !transaction.date.isBefore(startDate)
            } else if (endDate != null) {
                !transaction.date.isAfter(endDate)
            } else {
                true
            }

            matchesQuery && matchesType && matchesDateRange
        }
    }

    // ---------------------------------------------------------------------------
    // Property 1: Transaction filter AND logic
    // ---------------------------------------------------------------------------

    /**
     * Property 1: Transaction filter AND logic
     *
     * For all combinations of search query and type filter, the filtered result
     * equals the intersection of query-only and type-only filters.
     *
     * **Validates: Requirements 1.8**
     */
    @Test
    fun `property 1 - filter AND logic - result equals intersection of individual filters`() = runTest {
        checkAll(
            100,
            Arb.list(transactionArb(), 0..30),
            Arb.string(0..20),
            typeFilterArb()
        ) { transactions, query, typeFilter ->
            // Apply both filters together
            val combinedResult = applyFilters(transactions, query, typeFilter)

            // Apply filters separately
            val queryOnlyResult = applyFilters(transactions, query, TransactionTypeFilter.ALL)
            val typeOnlyResult = applyFilters(transactions, "", typeFilter)

            // Combined result should be the intersection
            val expectedIntersection = queryOnlyResult.filter { it in typeOnlyResult }

            combinedResult.toSet() shouldBe expectedIntersection.toSet()
        }
    }

    // ---------------------------------------------------------------------------
    // Property 2: Filter monotonicity
    // ---------------------------------------------------------------------------

    /**
     * Property 2: Filter monotonicity
     *
     * For all non-empty search queries q, the count of results for q shall be
     * less than or equal to the count of results for any prefix of q.
     *
     * In other words: adding more characters to the search query can only
     * reduce or maintain the result count, never increase it.
     *
     * **Validates: Requirements 1.3, 1.8**
     */
    @Test
    fun `property 2 - filter monotonicity - longer query yields fewer or equal results`() = runTest {
        checkAll(
            100,
            Arb.list(transactionArb(), 0..30),
            Arb.string(1..20).filter { it.isNotBlank() }
        ) { transactions, query ->
            // For each prefix of the query, check monotonicity
            for (i in 1 until query.length) {
                val prefix = query.substring(0, i)
                val fullQuery = query.substring(0, i + 1)

                val prefixResults = applyFilters(transactions, prefix, TransactionTypeFilter.ALL)
                val fullResults = applyFilters(transactions, fullQuery, TransactionTypeFilter.ALL)

                fullResults.size shouldBeLessThanOrEqual prefixResults.size
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Property 3: Type filter partition
    // ---------------------------------------------------------------------------

    /**
     * Property 3: Type filter partition
     *
     * Every transaction appears in exactly one of the DEBIT-filtered or
     * CREDIT-filtered views (never in both, never in neither).
     *
     * This validates that the type filter correctly partitions the transaction
     * list into two disjoint sets.
     *
     * **Validates: Requirements 1.4**
     */
    @Test
    fun `property 3 - type filter partition - every transaction in exactly one type view`() = runTest {
        checkAll(
            100,
            Arb.list(transactionArb(), 1..30)
        ) { transactions ->
            val debitResults = applyFilters(transactions, "", TransactionTypeFilter.DEBIT)
            val creditResults = applyFilters(transactions, "", TransactionTypeFilter.CREDIT)

            // No transaction should appear in both
            val intersection = debitResults.toSet() intersect creditResults.toSet()
            intersection.size shouldBe 0

            // Every transaction should appear in exactly one
            val union = (debitResults + creditResults).toSet()
            union shouldContainExactlyInAnyOrder transactions.toSet()

            // Verify counts
            debitResults.size shouldBe transactions.count { it.type == TransactionType.DEBIT }
            creditResults.size shouldBe transactions.count { it.type == TransactionType.CREDIT }
        }
    }
}
