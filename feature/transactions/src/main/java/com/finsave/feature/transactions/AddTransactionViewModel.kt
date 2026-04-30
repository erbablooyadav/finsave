package com.finsave.feature.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finsave.domain.model.Transaction
import com.finsave.domain.model.TransactionType
import com.finsave.domain.repository.AccountRepository
import com.finsave.domain.repository.CategoryRepository
import com.finsave.domain.usecase.transaction.AddTransactionUseCase
import com.finsave.domain.usecase.transaction.UpdateTransactionUseCase
import com.finsave.domain.usecase.transaction.DeleteTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import javax.inject.Inject

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val addTransactionUseCase: AddTransactionUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository
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
                if (accList.isNotEmpty() && _selectedAccountId.value == null) {
                    _selectedAccountId.value = accList.firstOrNull { it.isDefault }?.id ?: accList.first().id
                }
            }
        }
    }

    fun onAmountChange(amountString: String) {
        val parsed = amountString.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
        _amountPaise.value = (parsed * 100).toLong()
    }

    fun onTypeChange(type: TransactionType) {
        _transactionType.value = type
    }

    fun onCategorySelect(categoryId: Long) {
        _selectedCategoryId.value = categoryId
    }

    fun onMerchantChange(name: String) {
        _merchantName.value = name
    }

    fun onDateSelect(date: LocalDate) {
        _selectedDate.value = date
    }

    fun onAccountSelect(accountId: Long) {
        _selectedAccountId.value = accountId
    }

    fun initializeWithTransaction(transaction: Transaction) {
        _transactionId.value = transaction.id
        _isEditMode.value = true
        _amountPaise.value = transaction.amountPaise
        _transactionType.value = transaction.type
        _selectedCategoryId.value = transaction.categoryId
        _merchantName.value = transaction.merchantName
        _selectedDate.value = transaction.date
        _selectedAccountId.value = transaction.accountId
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
                    },
                    onFailure = { e ->
                        _uiEvent.emit(UiEvent.ShowError(e.message ?: "Failed to update transaction"))
                    }
                )
            } else {
                addTransactionUseCase(transaction).fold(
                    onSuccess = {
                        _uiEvent.emit(UiEvent.Success)
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
