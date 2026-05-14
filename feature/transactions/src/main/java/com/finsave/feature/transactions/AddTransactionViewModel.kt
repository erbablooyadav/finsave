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
import com.finsave.domain.repository.TransactionRepository
import com.finsave.domain.repository.MerchantCatalogRepository
import com.finsave.domain.usecase.category.SuggestCategoryForMerchantUseCase
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
    private val transactionRepository: TransactionRepository,
    private val merchantCatalogRepository: MerchantCatalogRepository,
    private val suggestCategoryForMerchantUseCase: SuggestCategoryForMerchantUseCase,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val budgetCheckScheduler: BudgetCheckScheduler,
    private val analyticsRepository: com.finsave.domain.repository.AnalyticsRepository
) : ViewModel() {

    private val _transactionId = MutableStateFlow<Long?>(null)
    private val _isEditMode = MutableStateFlow(false)
    val isEditMode = _isEditMode.asStateFlow()

    private val _amountPaise = MutableStateFlow(0L)
    val amountPaise = _amountPaise.asStateFlow()

    // Raw string input from numpad (e.g. "1234.50")
    private val _rawAmountInput = MutableStateFlow("")
    val rawAmountInput = _rawAmountInput.asStateFlow()

    // Formatted display string for the amount (e.g. "₹1,234.50")
    val amountDisplay: StateFlow<String> = _rawAmountInput.map { raw ->
        if (raw.isBlank()) "₹0" else "₹${formatAmountForDisplay(raw)}"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "₹0")

    private val _transactionType = MutableStateFlow(TransactionType.DEBIT)
    val transactionType = _transactionType.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId = _selectedCategoryId.asStateFlow()
    private val hasManuallySelectedCategory = MutableStateFlow(false)

    private val _merchantName = MutableStateFlow("")
    val merchantName = _merchantName.asStateFlow()
    private val hiddenMerchantSuggestion = MutableStateFlow<String?>(null)

    private val _note = MutableStateFlow("")
    val note = _note.asStateFlow()

    data class MerchantSuggestion(val name: String, val isRecent: Boolean)

    val merchantSuggestions = _merchantName
        .debounce(200)
        .flatMapLatest { query ->
            val normalizedQuery = query.trim()
            if (hiddenMerchantSuggestion.value == query || normalizedQuery.length < 2) {
                flowOf(emptyList())
            } else {
                flow {
                    val today = LocalDate.now(java.time.ZoneId.of("Asia/Kolkata"))
                    val startDate = today.minusDays(30)
                    val bundledMerchants = merchantCatalogRepository.getBundledMerchantNames()

                    emitAll(
                        transactionRepository.getTransactionsByDateRange(startDate, today)
                            .map { transactions ->
                                val recentMatches = transactions
                                    .asSequence()
                                    .filter { it.type == TransactionType.DEBIT }
                                    .map { it.merchantName.trim() }
                                    .filter { it.isNotBlank() }
                                    .distinctBy { it.lowercase() }
                                    .filter { 
                                        it.startsWith(normalizedQuery, ignoreCase = true) || 
                                        it.contains(normalizedQuery, ignoreCase = true) 
                                    }
                                    .map { MerchantSuggestion(name = it, isRecent = true) }
                                    .toList()

                                val recentKeys = recentMatches.mapTo(mutableSetOf()) { it.name.lowercase() }
                                val bundledMatches = bundledMerchants
                                    .asSequence()
                                    .map { it.trim() }
                                    .filter { it.isNotBlank() }
                                    .distinctBy { it.lowercase() }
                                    .filter { 
                                        it.startsWith(normalizedQuery, ignoreCase = true) || 
                                        it.contains(normalizedQuery, ignoreCase = true) 
                                    }
                                    .filter { it.lowercase() !in recentKeys }
                                    .sortedWith(String.CASE_INSENSITIVE_ORDER)
                                    .map { MerchantSuggestion(name = it, isRecent = false) }
                                    .toList()

                                val combined = (recentMatches + bundledMatches).distinctBy { it.name.lowercase() }
                                // Sort so that startsWith matches come before contains matches
                                combined.sortedBy { 
                                    if (it.name.startsWith(normalizedQuery, ignoreCase = true)) 0 else 1 
                                }.take(5)
                            }
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate = _selectedDate.asStateFlow()

    private val _selectedAccountId = MutableStateFlow<Long?>(null)
    val selectedAccountId = _selectedAccountId.asStateFlow()

    // Preserved across edits so that updating a transaction (e.g. changing its
    // category) does not wipe the sms_hash and break deduplication.
    private val _smsHash = MutableStateFlow<String?>(null)
    private val _isAutoImported = MutableStateFlow(false)

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
        
        // E1.6: Smart Category Auto-Select from Merchant Name
        viewModelScope.launch {
            _merchantName
                .debounce(300)
                .distinctUntilChanged()
                .filter { it.isNotBlank() }
                .collect { merchantName ->
                    if (!hasManuallySelectedCategory.value && !_isEditMode.value) {
                        val suggestedCategoryId = suggestCategoryForMerchantUseCase(merchantName)
                        if (suggestedCategoryId != null) {
                            _selectedCategoryId.value = suggestedCategoryId
                        }
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
        hasManuallySelectedCategory.value = true
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
        result.amountPaise?.let { paise ->
            _amountPaise.value = paise
            // Sync raw input for numpad display
            val rupees = paise / 100
            val remainder = paise % 100
            _rawAmountInput.value = if (remainder == 0L) rupees.toString()
                else "$rupees.${remainder.toString().padStart(2, '0')}"
        }
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
        _note.value = transaction.note
        _selectedDate.value = transaction.date
        _selectedAccountId.value = transaction.accountId
        _smsHash.value = transaction.smsHash
        _isAutoImported.value = transaction.isAutoImported
        // Sync raw amount input for numpad display
        val rupees = transaction.amountPaise / 100
        val paise = transaction.amountPaise % 100
        _rawAmountInput.value = if (paise == 0L) rupees.toString()
            else "$rupees.${paise.toString().padStart(2, '0')}"
    }

    /**
     * Resets all form state to defaults.
     * Called before editing a transaction to ensure no stale data leaks in.
     */
    private fun resetState() {
        _transactionId.value = null
        _isEditMode.value = false
        _amountPaise.value = 0L
        _rawAmountInput.value = ""
        _transactionType.value = TransactionType.DEBIT
        _selectedCategoryId.value = null
        hasManuallySelectedCategory.value = false
        _merchantName.value = ""
        _note.value = ""
        _selectedDate.value = java.time.LocalDate.now()
        _selectedAccountId.value = null
        _smsHash.value = null
        _isAutoImported.value = false
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
                note = _note.value,
                isAutoImported = _isAutoImported.value,
                smsHash = _smsHash.value
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
                        analyticsRepository.trackEvent(
                            "TRANSACTION_CREATED",
                            mapOf(
                                "type" to transaction.type.name,
                                "is_auto_imported" to transaction.isAutoImported
                            )
                        )
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

    // ── Numpad ──

    /**
     * Handles numpad key presses. All validation happens here (OWASP A03).
     * Max 8 digits before decimal, max 2 after, max value 9_999_999 rupees.
     */
    fun onNumpadKey(key: NumpadKey) {
        val current = _rawAmountInput.value
        val newValue = when (key) {
            is NumpadKey.Digit -> {
                val candidate = current + key.value.toString()
                if (isValidAmountInput(candidate)) candidate else current
            }
            is NumpadKey.Dot -> {
                if (current.contains('.')) current // reject multiple dots
                else if (current.isBlank()) "0." // "" → "0."
                else current + "."
            }
            is NumpadKey.Backspace -> {
                if (current.isNotEmpty()) current.dropLast(1) else ""
            }
        }
        _rawAmountInput.value = newValue
        syncAmountPaise(newValue)
    }

    private fun syncAmountPaise(raw: String) {
        if (raw.isBlank() || raw == ".") {
            _amountPaise.value = 0L
            return
        }
        try {
            val cleaned = raw.trimEnd('.')
            val bd = java.math.BigDecimal(cleaned)
            _amountPaise.value = bd
                .multiply(java.math.BigDecimal("100"))
                .setScale(0, java.math.RoundingMode.HALF_UP)
                .toLong()
        } catch (_: NumberFormatException) {
            _amountPaise.value = 0L
        }
    }

    private fun isValidAmountInput(candidate: String): Boolean {
        val parts = candidate.split('.')
        // Integer part: max 8 digits, but leading zeros collapse ("0" is ok as prefix to dot)
        val intPart = parts[0]
        if (intPart.length > 8) return false
        // Decimal part: max 2 digits
        if (parts.size == 2 && parts[1].length > 2) return false
        // Value cap: 99,99,999 rupees = 9_999_999
        try {
            val value = java.math.BigDecimal(candidate.trimEnd('.'))
            if (value > java.math.BigDecimal("9999999")) return false
        } catch (_: NumberFormatException) {
            return false
        }
        return true
    }

    private fun formatAmountForDisplay(raw: String): String {
        if (raw.isBlank()) return "0"
        val parts = raw.split('.')
        val intPart = parts[0].ifBlank { "0" }
        // Indian number formatting for the integer part
        val formatted = formatIndianInteger(intPart)
        return if (parts.size == 2) "$formatted.${parts[1]}" else formatted
    }

    private fun formatIndianInteger(intStr: String): String {
        val n = intStr.toLongOrNull() ?: return intStr
        if (n < 1000) return n.toString()
        val last3 = (n % 1000).toString().padStart(3, '0')
        var rest = n / 1000
        val groups = mutableListOf(last3)
        while (rest > 0) {
            groups.add(0, (rest % 100).toString().let { if (rest / 100 > 0) it.padStart(2, '0') else it })
            rest /= 100
        }
        return groups.joinToString(",")
    }

    sealed class UiEvent {
        object Success : UiEvent()
        data class ShowError(val message: String) : UiEvent()
    }

    // ── E2.4: UPI QR Scanned ──
    fun onUpiQrScanned(upiUri: String) {
        if (!upiUri.startsWith("upi://pay?") && !upiUri.startsWith("upi://")) {
            viewModelScope.launch { _uiEvent.emit(UiEvent.ShowError("Not a valid UPI QR code")) }
            return
        }

        try {
            val uri = android.net.Uri.parse(upiUri)
            val pa = uri.getQueryParameter("pa")
            val pn = uri.getQueryParameter("pn")
            val am = uri.getQueryParameter("am")
            val tn = uri.getQueryParameter("tn")

            // Security OWASP A03: Length validation
            if ((pa?.length ?: 0) > 256 || (pn?.length ?: 0) > 256 || (am?.length ?: 0) > 256 || (tn?.length ?: 0) > 256) {
                viewModelScope.launch { _uiEvent.emit(UiEvent.ShowError("Invalid UPI QR code")) }
                return
            }

            // Pre-fill fields
            if (!pn.isNullOrBlank()) {
                val decodedPn = android.net.Uri.decode(pn)
                onMerchantChange(decodedPn)
            }

            if (!am.isNullOrBlank()) {
                if (isValidAmountInput(am)) {
                    _rawAmountInput.value = am
                    syncAmountPaise(am)
                }
            }

            if (!tn.isNullOrBlank()) {
                val decodedTn = android.net.Uri.decode(tn)
                _note.value = decodedTn
            }
            
            if (!pa.isNullOrBlank()) {
                if (_note.value.isBlank()) {
                    _note.value = "UPI ID: $pa"
                } else {
                    _note.value += " (UPI ID: $pa)"
                }
            }

        } catch (e: Exception) {
            viewModelScope.launch { _uiEvent.emit(UiEvent.ShowError("Failed to parse UPI QR code")) }
        }
    }
}

/**
 * Numpad key sealed class.
 */
sealed class NumpadKey {
    data class Digit(val value: Int) : NumpadKey()
    object Dot : NumpadKey()
    object Backspace : NumpadKey()
}
