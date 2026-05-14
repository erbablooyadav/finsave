package com.finsave.feature.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finsave.domain.model.Transaction
import com.finsave.domain.model.TransactionType
import com.finsave.domain.model.Category
import com.finsave.domain.repository.CategoryRepository
import com.finsave.domain.usecase.transaction.AddTransactionUseCase
import com.finsave.domain.usecase.transaction.DeleteTransactionUseCase
import com.finsave.domain.usecase.transaction.GetTransactionsUseCase
import com.finsave.domain.usecase.transaction.UpdateTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val getTransactions: GetTransactionsUseCase,
    private val updateTransaction: UpdateTransactionUseCase,
    private val deleteTransaction: DeleteTransactionUseCase,
    private val addTransaction: AddTransactionUseCase,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _filterState = MutableStateFlow(TransactionFilterState())
    val filterState: StateFlow<TransactionFilterState> = _filterState.asStateFlow()

    private val _selectedTransaction = MutableStateFlow<Transaction?>(null)
    val selectedTransaction: StateFlow<Transaction?> = _selectedTransaction.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _deleteConfirmation = MutableStateFlow<Transaction?>(null)
    val deleteConfirmation: StateFlow<Transaction?> = _deleteConfirmation.asStateFlow()

    // Category map for UI display
    val categories: StateFlow<Map<Long, Category>> = categoryRepository.getAllCategories()
        .map { list -> list.associateBy { it.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Snackbar events for undo support
    private val _snackbarMessage = MutableSharedFlow<SnackbarEvent>()
    val snackbarMessage: SharedFlow<SnackbarEvent> = _snackbarMessage.asSharedFlow()

    // Last deleted transaction for undo
    private var lastDeletedTransaction: Transaction? = null

    private val debouncedFilterState = _filterState
        .debounce(300)
        .distinctUntilChanged()

    // Combine all transactions with filters
    val transactions: StateFlow<List<Transaction>> = combine(
        getTransactions(),
        debouncedFilterState
    ) { allTransactions, filterState ->
        applyFilters(
            transactions = allTransactions,
            query = filterState.query,
            typeFilter = filterState.typeFilter,
            datePreset = filterState.datePreset,
            minAmountPaise = filterState.minAmountPaise,
            maxAmountPaise = filterState.maxAmountPaise,
            sortOption = filterState.sortOption,
            startDate = filterState.startDate,
            endDate = filterState.endDate
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Applies all filters to the transaction list with AND logic.
     */
    private fun applyFilters(
        transactions: List<Transaction>,
        query: String,
        typeFilter: TransactionTypeFilter,
        datePreset: TransactionDatePreset,
        minAmountPaise: Long?,
        maxAmountPaise: Long?,
        sortOption: TransactionSortOption,
        startDate: LocalDate?,
        endDate: LocalDate?
    ): List<Transaction> {
        val now = LocalDate.now()
        val presetStartDate = when (datePreset) {
            TransactionDatePreset.ALL_TIME -> null
            TransactionDatePreset.THIS_MONTH -> YearMonth.now().atDay(1)
            TransactionDatePreset.LAST_3_MONTHS -> now.minusMonths(3)
            TransactionDatePreset.THIS_YEAR -> LocalDate.of(now.year, 1, 1)
            TransactionDatePreset.CUSTOM -> null
        }

        val filtered = transactions
            .filter { transaction ->
                // Search query filter (merchant name or note)
                val matchesQuery = if (query.isBlank()) {
                    true
                } else {
                    when (val amountQueryPaise = parseAmountQueryToPaise(query)) {
                        null -> {
                            transaction.merchantName.contains(query, ignoreCase = true) ||
                                transaction.note.contains(query, ignoreCase = true) ||
                                matchesAmountDisplay(transaction, query)
                        }
                        else -> {
                            // Numeric query means user is searching by amount,
                            // so match exact value (with 1-paise tolerance).
                            abs(transaction.amountPaise - amountQueryPaise) <= 1L
                        }
                    }
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

                val matchesPreset = presetStartDate?.let { !transaction.date.isBefore(it) } ?: true
                val matchesAmountRange = (minAmountPaise == null || transaction.amountPaise >= minAmountPaise) &&
                    (maxAmountPaise == null || transaction.amountPaise <= maxAmountPaise)

                matchesQuery && matchesType && matchesDateRange && matchesPreset && matchesAmountRange
            }
        return when (sortOption) {
            TransactionSortOption.DATE_DESC -> filtered.sortedByDescending { it.date }
            TransactionSortOption.DATE_ASC -> filtered.sortedBy { it.date }
            TransactionSortOption.AMOUNT_DESC -> filtered.sortedByDescending { it.amountPaise }
            TransactionSortOption.AMOUNT_ASC -> filtered.sortedBy { it.amountPaise }
        }
    }

    private fun matchesAmountDisplay(transaction: Transaction, query: String): Boolean {
        val normalizedQuery = query
            .replace("₹", "")
            .replace(",", "")
            .trim()

        if (normalizedQuery.isBlank()) return false

        val rupeesFormatted = String.format(Locale.ROOT, "%.2f", transaction.amountPaise / 100.0)
        val rupeesWhole = (transaction.amountPaise / 100L).toString()
        val paiseRaw = transaction.amountPaise.toString()

        return rupeesFormatted.contains(normalizedQuery) ||
            rupeesWhole.contains(normalizedQuery) ||
            paiseRaw.contains(normalizedQuery)
    }

    private fun parseAmountQueryToPaise(query: String): Long? {
        val cleaned = query
            .replace("₹", "")
            .replace(",", "")
            .trim()

        if (cleaned.isBlank()) return null
        if (!cleaned.matches(Regex("^\\d+(\\.\\d{1,2})?$"))) return null

        val rupees = cleaned.toDoubleOrNull() ?: return null
        return (rupees * 100.0).roundToLong()
    }

    fun onSearchQueryChange(query: String) {
        _filterState.update { it.copy(query = query) }
    }

    fun onTypeFilterChange(typeFilter: TransactionTypeFilter) {
        _filterState.update { it.copy(typeFilter = typeFilter) }
    }

    fun onDatePresetChange(datePreset: TransactionDatePreset) {
        _filterState.update {
            if (datePreset == TransactionDatePreset.CUSTOM) {
                it.copy(datePreset = datePreset)
            } else {
                it.copy(datePreset = datePreset, startDate = null, endDate = null)
            }
        }
    }

    fun onAmountRangeChange(minAmountPaise: Long?, maxAmountPaise: Long?) {
        _filterState.update { it.copy(minAmountPaise = minAmountPaise, maxAmountPaise = maxAmountPaise) }
    }

    fun onSortOptionChange(sortOption: TransactionSortOption) {
        _filterState.update { it.copy(sortOption = sortOption) }
    }

    fun resetFilters() {
        _filterState.value = TransactionFilterState()
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
                    lastDeletedTransaction = transaction
                    _deleteConfirmation.value = null
                    _snackbarMessage.emit(
                        SnackbarEvent(
                            message = "Transaction deleted",
                            actionLabel = "Undo",
                            onAction = { undoDelete() }
                        )
                    )
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "Failed to delete transaction"
                    _deleteConfirmation.value = null
                }
            )
        }
    }

    /**
     * Swipe-to-delete: deletes immediately and shows undo snackbar.
     */
    fun onSwipeDelete(transaction: Transaction) {
        viewModelScope.launch {
            deleteTransaction(transaction.id).fold(
                onSuccess = {
                    lastDeletedTransaction = transaction
                    _snackbarMessage.emit(
                        SnackbarEvent(
                            message = "Transaction deleted",
                            actionLabel = "Undo",
                            onAction = { undoDelete() }
                        )
                    )
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "Failed to delete transaction"
                }
            )
        }
    }

    /**
     * Re-inserts the last deleted transaction.
     */
    fun undoDelete() {
        val transaction = lastDeletedTransaction ?: return
        viewModelScope.launch {
            addTransaction(transaction)
            lastDeletedTransaction = null
        }
    }

    fun onClearError() {
        _errorMessage.value = null
    }
}

/**
 * Snackbar event with optional undo action.
 */
data class SnackbarEvent(
    val message: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null
)
