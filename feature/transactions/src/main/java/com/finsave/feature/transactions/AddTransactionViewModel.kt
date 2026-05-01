package com.finsave.feature.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finsave.core.common.voice.VoiceInputParser
import com.finsave.core.common.work.BudgetCheckScheduler
import com.finsave.domain.model.Transaction
import com.finsave.domain.model.TransactionType
import com.finsave.domain.repository.AccountRepository
import com.finsave.domain.repository.CategoryRepository
import com.finsave.domain.usecase.transaction.AddTransactionUseCase
import com.finsave.domain.usecase.transaction.UpdateTransactionUseCase
import com.finsave.domain.usecase.transaction.DeleteTransactionUseCase
import com.finsave.domain.usecase.transaction.GetMerchantSuggestionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import javax.inject.Inject

@HiltViewModel
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class AddTransactionViewModel @Inject constructor(
    private val addTransactionUseCase: AddTransactionUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val getMerchantSuggestionsUseCase: GetMerchantSuggestionsUseCase,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val budgetCheckScheduler: BudgetCheckScheduler
) : ViewModel() {

    private val _transactionId = MutableStateFlow<Long?>(null)
    private val _isEditMode = MutableStateFlow(false)
    val isEditMode = _isEditMode.asStateFlow()

    private val _amountPaise = MutableStateFlow(0L)
    val amountPaise = _amountPaise.asStateFlow()

    private val _transactionType = MutableStateFlow(TransactionType.DEBIT)
    val transactionType = _transactionType.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId = _selectedCategoryId.asStateFlow()

    private val _merchantName = MutableStateFlow("")
    val merchantName = _merchantName.asStateFlow()
    private val hiddenMerchantSuggestion = MutableStateFlow<String?>(null)

    val merchantSuggestions = _merchantName
        .debounce(200)
        .flatMapLatest { query ->
            if (hiddenMerchantSuggestion.value == query) {
                flowOf(emptyList())
            } else {
                getMerchantSuggestionsUseCase(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate = _selectedDate.asStateFlow()

    private val _selectedAccountId = MutableStateFlow<Long?>(null)
    val selectedAccountId = _selectedAccountId.asStateFlow()

    val categories = categoryRepository.getAllCategories().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val accounts = accountRepository.getAllAccounts().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            accounts.collect { accList ->
                // Only auto-select a default account when creating a NEW transaction.
                // In edit mode, the account is set explicitly by initializeWithTransaction().
                if (accList.isNotEmpty() && _selectedAccountId.value == null && !_isEditMode.value) {
                    _selectedAccountId.value = accList.firstOrNull { it.isDefault }?.id ?: accList.first().id
                }
            }
        }
    }

    fun onAmountChange(amountString: String) {
        if (amountString.isBlank() || amountString == ".") {
            _amountPaise.value = 0L
            return
        }

        try {
            val cleaned = amountString.trimEnd('.')
            val bd = java.math.BigDecimal(cleaned)
            _amountPaise.value = bd
                .multiply(java.math.BigDecimal("100"))
                .setScale(0, java.math.RoundingMode.HALF_UP)
                .toLong()
        } catch (e: NumberFormatException) {
            _amountPaise.value = 0L
        }
    }

    fun onTypeChange(type: TransactionType) {
        _transactionType.value = type
    }

    fun onCategorySelect(categoryId: Long) {
        _selectedCategoryId.value = categoryId
    }

    fun onMerchantChange(name: String) {
        if (hiddenMerchantSuggestion.value != null && hiddenMerchantSuggestion.value != name) {
            hiddenMerchantSuggestion.value = null
        }
        _merchantName.value = name
    }

    fun onMerchantSuggestionSelect(name: String) {
        hiddenMerchantSuggestion.value = name
        _merchantName.value = name
    }

    fun processVoiceInput(text: String) {
        val result = VoiceInputParser.parse(text)
        result.amountPaise?.let { _amountPaise.value = it }
        result.merchant?.let { onMerchantChange(it) }
    }

    fun onDateSelect(date: LocalDate) {
        _selectedDate.value = date
    }

    fun onAccountSelect(accountId: Long) {
        _selectedAccountId.value = accountId
    }

    fun initializeWithTransaction(transaction: Transaction) {
        // Reset all state first to prevent stale values from a previous session
        // bleeding into the new edit (e.g. if Hilt reuses the VM instance).
        resetState()
        _transactionId.value = transaction.id
        _isEditMode.value = true
        _amountPaise.value = transaction.amountPaise
        _transactionType.value = transaction.type
        _selectedCategoryId.value = transaction.categoryId
        _merchantName.value = transaction.merchantName
        _selectedDate.value = transaction.date
        _selectedAccountId.value = transaction.accountId
    }

    /**
     * Resets all form state to defaults.
     * Called before editing a transaction to ensure no stale data leaks in.
     */
    private fun resetState() {
        _transactionId.value = null
        _isEditMode.value = false
        _amountPaise.value = 0L
        _transactionType.value = TransactionType.DEBIT
        _selectedCategoryId.value = null
        _merchantName.value = ""
        _selectedDate.value = java.time.LocalDate.now()
        _selectedAccountId.value = null
        hiddenMerchantSuggestion.value = null
    }

    fun saveTransaction() {
        val amount = _amountPaise.value
        if (amount <= 0) {
            viewModelScope.launch { _uiEvent.emit(UiEvent.ShowError("Amount must be greater than 0")) }
            return
        }
        val categoryId = _selectedCategoryId.value
        if (categoryId == null) {
            viewModelScope.launch { _uiEvent.emit(UiEvent.ShowError("Please select a category")) }
            return
        }
        val accountId = _selectedAccountId.value ?: return

        viewModelScope.launch {
            val transaction = Transaction(
                id = _transactionId.value ?: 0,
                accountId = accountId,
                categoryId = categoryId,
                amountPaise = amount,
                type = _transactionType.value,
                merchantName = _merchantName.value.ifBlank { "Unknown Merchant" },
                date = _selectedDate.value,
                note = "",
                smsHash = null
            )
            
            if (_isEditMode.value) {
                updateTransactionUseCase(transaction).fold(
                    onSuccess = {
                        _uiEvent.emit(UiEvent.Success)
                        budgetCheckScheduler.scheduleBudgetCheck()
                    },
                    onFailure = { e ->
                        _uiEvent.emit(UiEvent.ShowError(e.message ?: "Failed to update transaction"))
                    }
                )
            } else {
                addTransactionUseCase(transaction).fold(
                    onSuccess = {
                        _uiEvent.emit(UiEvent.Success)
                        budgetCheckScheduler.scheduleBudgetCheck()
                    },
                    onFailure = { e ->
                        _uiEvent.emit(UiEvent.ShowError(e.message ?: "Failed to save transaction"))
                    }
                )
            }
        }
    }

    fun deleteTransaction() {
        val transactionId = _transactionId.value
        if (transactionId == null || transactionId == 0L) {
            viewModelScope.launch { _uiEvent.emit(UiEvent.ShowError("Invalid transaction")) }
            return
        }

        viewModelScope.launch {
            deleteTransactionUseCase(transactionId).fold(
                onSuccess = {
                    _uiEvent.emit(UiEvent.Success)
                },
                onFailure = { e ->
                    _uiEvent.emit(UiEvent.ShowError(e.message ?: "Failed to delete transaction"))
                }
            )
        }
    }

    sealed class UiEvent {
        object Success : UiEvent()
        data class ShowError(val message: String) : UiEvent()
    }
}
