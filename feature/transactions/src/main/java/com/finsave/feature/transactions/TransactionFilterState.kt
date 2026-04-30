package com.finsave.feature.transactions

import java.time.LocalDate

/**
 * Represents the current filter state for the transactions list.
 *
 * All filters are applied with AND logic: a transaction must match
 * the search query AND the type filter AND the date range to be visible.
 */
data class TransactionFilterState(
    val query: String = "",
    val typeFilter: TransactionTypeFilter = TransactionTypeFilter.ALL,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null
)

/**
 * Type filter for transactions.
 */
enum class TransactionTypeFilter {
    ALL,
    DEBIT,
    CREDIT
}
