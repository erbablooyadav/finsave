package com.finsave.feature.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finsave.domain.model.Transaction
import com.finsave.domain.model.TransactionType
import com.finsave.domain.usecase.transaction.DeleteTransactionUseCase
import com.finsave.domain.usecase.transaction.GetTransactionsUseCase
import com.finsave.domain.usecase.transaction.UpdateTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val getTransactions: GetTransactionsUseCase,
    private val updateTransaction: UpdateTransactionUseCase,
    private val deleteTransaction: DeleteTransactionUseCase
) : ViewModel() {

    private val _filterState = MutableStateFlow(TransactionFilterState())
    val filterState: StateFlow<TransactionFilterState> = _filterState.asStateFlow()

    private val _selectedTransaction = MutableStateFlow<Transaction?>(null)
    val selectedTransaction: StateFlow<Transaction?> = _selectedTransaction.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _deleteConfirmation = MutableStateFlow<Transaction?>(null)
    val deleteConfirmation: StateFlow<Transaction?> = _deleteConfirmation.asStateFlow()

    // Debounced search query
    private val debouncedQuery = _filterState
        .map { it.query }
        .debounce(300)
        .distinctUntilChanged()

    // Combine all transactions with filters
    val transactions: StateFlow<List<Transaction>> = combine(
        getTransactions(),
        debouncedQuery,
        _filterState.map { it.typeFilter },
        _filterState.map { it.startDate },
        _filterState.map { it.endDate }
    ) { allTransactions, query, typeFilter, startDate, endDate ->
        applyFilters(allTransactions, query, typeFilter, startDate, endDate)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Applies all filters to the transaction list with AND logic.
     */
    private fun applyFilters(
        transactions: List<Transaction>,
        query: String,
        typeFilter: TransactionTypeFilter,
        startDate: LocalDate?,
        endDate: LocalDate?
    ): List<Transaction> {
        return transactions
            .filter { transaction ->
                // Search query filter (merchant name or note)
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
            .sortedByDescending { it.date }
    }

    fun onSearchQueryChange(query: String) {
        _filterState.update { it.copy(query = query) }
    }

    fun onTypeFilterChange(typeFilter: TransactionTypeFilter) {
        _filterState.update { it.copy(typeFilter = typeFilter) }
    }

    fun onDateRangeChange(startDate: LocalDate?, endDate: LocalDate?) {
        _filterState.update { it.copy(startDate = startDate, endDate = endDate) }
    }

    fun onTransactionClick(transaction: Transaction) {
        _selectedTransaction.value = transaction
    }

    fun onDismissEditSheet() {
        _selectedTransaction.value = null
    }

    fun onSwipeToDelete(transaction: Transaction) {
        _deleteConfirmation.value = transaction
    }

    fun onDismissDeleteConfirmation() {
        _deleteConfirmation.value = null
    }

    fun onConfirmDelete() {
        val transaction = _deleteConfirmation.value ?: return
        viewModelScope.launch {
            deleteTransaction(transaction.id).fold(
                onSuccess = {
                    _deleteConfirmation.value = null
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "Failed to delete transaction"
                    _deleteConfirmation.value = null
                }
            )
        }
    }

    fun onClearError() {
        _errorMessage.value = null
    }
}
