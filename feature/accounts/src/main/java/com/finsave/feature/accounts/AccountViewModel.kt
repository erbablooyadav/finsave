package com.finsave.feature.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finsave.domain.model.Account
import com.finsave.domain.model.AccountType
import com.finsave.domain.usecase.account.AddAccountUseCase
import com.finsave.domain.usecase.account.DeleteAccountUseCase
import com.finsave.domain.usecase.account.GetAccountsUseCase
import com.finsave.domain.usecase.account.UpdateAccountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for AccountScreen.
 * Manages account list, total balance, and CRUD operations.
 *
 * Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9, 4.10
 */
@HiltViewModel
class AccountViewModel @Inject constructor(
    private val getAccounts: GetAccountsUseCase,
    private val addAccount: AddAccountUseCase,
    private val updateAccount: UpdateAccountUseCase,
    private val deleteAccount: DeleteAccountUseCase
) : ViewModel() {

    // Account list from repository
    val accounts: StateFlow<List<Account>> = getAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Total balance across all accounts
    val totalBalance: StateFlow<Long> = accounts
        .map { accountList -> accountList.sumOf { it.balancePaise } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    // UI state
    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    // Dialog state
    private val _dialogAccount = MutableStateFlow<Account?>(null)
    val dialogAccount: StateFlow<Account?> = _dialogAccount.asStateFlow()

    // Delete confirmation state
    private val _deleteConfirmation = MutableStateFlow<Account?>(null)
    val deleteConfirmation: StateFlow<Account?> = _deleteConfirmation.asStateFlow()

    /**
     * Opens the add account dialog.
     */
    fun onAddAccountClick() {
        _dialogAccount.value = Account(
            name = "",
            type = AccountType.SAVINGS,
            balancePaise = 0L,
            colorHex = "#4F46E5",
            isDefault = false
        )
    }

    /**
     * Opens the edit account dialog.
     */
    fun onEditAccount(account: Account) {
        _dialogAccount.value = account
    }

    /**
     * Closes the account dialog.
     */
    fun onDismissDialog() {
        _dialogAccount.value = null
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Saves an account (add or update).
     * Validates: name non-empty, opening balance ≥ 0.
     * Requirements: 4.4, 4.5
     */
    fun onSaveAccount(
        id: Long,
        name: String,
        type: AccountType,
        balancePaise: Long,
        colorHex: String,
        isDefault: Boolean
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val account = Account(
                id = id,
                name = name.trim(),
                type = type,
                balancePaise = balancePaise,
                colorHex = colorHex,
                isDefault = isDefault
            )

            val result = if (id == 0L) {
                addAccount(account)
            } else {
                updateAccount(account)
            }

            result.fold(
                onSuccess = {
                    _dialogAccount.value = null
                    _uiState.update { it.copy(isLoading = false) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to save account"
                        )
                    }
                }
            )
        }
    }

    /**
     * Shows delete confirmation dialog.
     */
    fun onDeleteAccountClick(account: Account) {
        _deleteConfirmation.value = account
    }

    /**
     * Dismisses delete confirmation dialog.
     */
    fun onDismissDeleteConfirmation() {
        _deleteConfirmation.value = null
    }

    /**
     * Confirms account deletion.
     * Validates: no linked transactions, account count > 1.
     * Requirements: 4.7, 4.8, 4.10
     */
    fun onConfirmDelete() {
        val account = _deleteConfirmation.value ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            deleteAccount(account.id).fold(
                onSuccess = {
                    _deleteConfirmation.value = null
                    _uiState.update { it.copy(isLoading = false) }
                },
                onFailure = { error ->
                    _deleteConfirmation.value = null
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to delete account"
                        )
                    }
                }
            )
        }
    }

    /**
     * Sets an account as default.
     * Unmarks previous default, marks new one.
     * Requirement: 4.9
     */
    fun onSetDefault(account: Account) {
        viewModelScope.launch {
            val currentAccounts = accounts.value

            // Unmark all accounts as default
            currentAccounts.forEach { acc ->
                if (acc.isDefault && acc.id != account.id) {
                    updateAccount(acc.copy(isDefault = false))
                }
            }

            // Mark the selected account as default
            updateAccount(account.copy(isDefault = true))
        }
    }

    /**
     * Clears error message.
     */
    fun onClearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

/**
 * UI state for AccountScreen.
 */
data class AccountUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
