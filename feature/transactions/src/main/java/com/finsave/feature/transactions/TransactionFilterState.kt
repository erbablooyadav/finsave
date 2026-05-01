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
    val datePreset: TransactionDatePreset = TransactionDatePreset.ALL_TIME,
    val minAmountPaise: Long? = null,
    val maxAmountPaise: Long? = null,
    val sortOption: TransactionSortOption = TransactionSortOption.DATE_DESC,
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

enum class TransactionDatePreset {
    ALL_TIME,
    THIS_MONTH,
    LAST_3_MONTHS,
    THIS_YEAR,
    CUSTOM
}

enum class TransactionSortOption {
    DATE_DESC,
    DATE_ASC,
    AMOUNT_DESC,
    AMOUNT_ASC
}
