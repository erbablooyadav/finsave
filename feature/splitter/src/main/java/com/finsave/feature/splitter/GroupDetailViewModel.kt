package com.finsave.feature.splitter

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finsave.core.common.Constants
import com.finsave.core.common.upi.UpiUriBuilder
import com.finsave.domain.model.MemberBalance
import com.finsave.domain.model.SimplifiedDebt
import com.finsave.domain.model.SplitterExpense
import com.finsave.domain.model.SplitterExpenseSplit
import com.finsave.domain.model.SplitterGroup
import com.finsave.domain.model.SplitterMember
import com.finsave.domain.model.SplitType
import com.finsave.domain.repository.SplitterRepository
import com.finsave.domain.usecase.splitter.AddExpenseUseCase
import com.finsave.domain.usecase.splitter.AddMemberUseCase
import com.finsave.domain.usecase.splitter.CalculateMemberBalancesUseCase
import com.finsave.domain.usecase.splitter.RemoveMemberUseCase
import com.finsave.domain.usecase.splitter.SimplifyDebtsUseCase
import com.finsave.core.common.formatter.IndianNumberFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Group Detail Screen.
 * 
 * Manages:
 * - Group members and expenses
 * - Member balance calculations
 * - Simplified debt list
 * - UPI settle-up URI generation
 * 
 * Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7, 7.8, 7.9, 7.10, 7.11, 7.12, 7.13, 7.14, 15.1, 15.2, 15.3, 15.4, 15.5
 */
@HiltViewModel
class GroupDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val splitterRepository: SplitterRepository,
    private val addMemberUseCase: AddMemberUseCase,
    private val addExpenseUseCase: AddExpenseUseCase,
    private val removeMemberUseCase: RemoveMemberUseCase,
    private val calculateBalancesUseCase: CalculateMemberBalancesUseCase,
    private val simplifyDebtsUseCase: SimplifyDebtsUseCase
) : ViewModel() {

    val groupId: Long = savedStateHandle["groupId"] ?: 0L

    // Group data
    val group: StateFlow<SplitterGroup?> = splitterRepository.getGroupById(groupId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val members: StateFlow<List<SplitterMember>> = splitterRepository.getMembersByGroup(groupId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val expenses: StateFlow<List<SplitterExpense>> = splitterRepository.getExpensesByGroup(groupId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Calculated balances and debts
    private val _memberBalances = MutableStateFlow<List<MemberBalance>>(emptyList())
    val memberBalances: StateFlow<List<MemberBalance>> = _memberBalances.asStateFlow()

    private val _simplifiedDebts = MutableStateFlow<List<SimplifiedDebt>>(emptyList())
    val simplifiedDebts: StateFlow<List<SimplifiedDebt>> = _simplifiedDebts.asStateFlow()

    val isGroupSettleable: StateFlow<Boolean> = combine(simplifiedDebts, expenses) { debts, expenseList ->
        debts.isEmpty() && expenseList.isNotEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val isSettled: StateFlow<Boolean> = group
        .map { it?.isSettled == true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    // UI state
    private val _uiState = MutableStateFlow(GroupDetailUiState())
    val uiState: StateFlow<GroupDetailUiState> = _uiState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Dialog states
    private val _showAddMemberDialog = MutableStateFlow(false)
    val showAddMemberDialog: StateFlow<Boolean> = _showAddMemberDialog.asStateFlow()

    private val _showAddExpenseDialog = MutableStateFlow(false)
    val showAddExpenseDialog: StateFlow<Boolean> = _showAddExpenseDialog.asStateFlow()

    private val _memberToDelete = MutableStateFlow<SplitterMember?>(null)
    val memberToDelete: StateFlow<SplitterMember?> = _memberToDelete.asStateFlow()

    init {
        // Recalculate balances whenever members or expenses change
        viewModelScope.launch {
            combine(members, expenses) { _, _ -> Unit }
                .collect {
                    recalculateBalances()
                }
        }
    }

    /**
     * Recalculates member balances and simplified debts.
     * Requirement: 7.10
     */
    private suspend fun recalculateBalances() {
        val result = calculateBalancesUseCase(groupId)
        result.onSuccess { balances ->
            _memberBalances.value = balances
            // Simplify debts
            val debts = simplifyDebtsUseCase(balances)
            _simplifiedDebts.value = debts
        }.onFailure { error ->
            _errorMessage.value = error.message ?: "Failed to calculate balances"
        }
    }

    /**
     * Adds a new member to the group.
     * Requirements: 7.3, 7.4, 7.5
     */
    fun addMember(name: String, upiId: String?) {
        viewModelScope.launch {
            val member = SplitterMember(
                groupId = groupId,
                name = name.trim(),
                upiId = upiId?.trim()?.takeIf { it.isNotBlank() }
            )

            val result = addMemberUseCase(member)
            result.onSuccess {
                _showAddMemberDialog.value = false
                _errorMessage.value = null
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Failed to add member"
            }
        }
    }

    /**
     * Removes a member from the group.
     * Requirements: 7.11, 7.12
     */
    fun removeMember(member: SplitterMember) {
        viewModelScope.launch {
            val result = removeMemberUseCase(member.id, groupId)
            result.onSuccess {
                _memberToDelete.value = null
                _errorMessage.value = null
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Failed to remove member"
                _memberToDelete.value = null
            }
        }
    }

    /**
     * Adds a new expense to the group.
     * Requirements: 7.6, 7.7, 7.8, 7.9
     */
    fun addExpense(
        description: String,
        amountPaise: Long,
        paidByMemberId: Long,
        splitType: SplitType,
        splits: List<SplitterExpenseSplit>
    ) {
        viewModelScope.launch {
            val expense = SplitterExpense(
                groupId = groupId,
                description = description.trim(),
                amountPaise = amountPaise,
                paidByMemberId = paidByMemberId,
                splitType = splitType
            )

            val result = addExpenseUseCase(expense, splits)
            result.onSuccess {
                _showAddExpenseDialog.value = false
                _errorMessage.value = null
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Failed to add expense"
            }
        }
    }

    /**
     * Builds a UPI payment URI for settling a debt.
     * Requirements: 15.1, 15.2, 15.3, 15.4, 15.5
     */
    fun buildUpiUri(debt: SimplifiedDebt, groupName: String): String? {
        val creditor = debt.toMember
        val upiId = creditor.upiId ?: return null

        // Truncate note to 50 characters as per NPCI spec
        val note = "FinSave: $groupName settlement".take(50)

        return UpiUriBuilder.build(
            payeeVpa = upiId,
            payeeName = creditor.name,
            amountPaise = debt.amountPaise,
            transactionNote = note
        )
    }

    fun settleGroup() {
        viewModelScope.launch {
            val currentGroup = group.value ?: return@launch
            try {
                splitterRepository.updateGroup(
                    currentGroup.copy(isSettled = true, isArchived = true)
                )
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to settle group"
            }
        }
    }

    // Dialog control methods
    fun onAddMemberClick() {
        _showAddMemberDialog.value = true
    }

    fun onDismissAddMemberDialog() {
        _showAddMemberDialog.value = false
        _errorMessage.value = null
    }

    fun onAddExpenseClick() {
        _showAddExpenseDialog.value = true
    }

    fun onDismissAddExpenseDialog() {
        _showAddExpenseDialog.value = false
        _errorMessage.value = null
    }

    fun onRemoveMemberClick(member: SplitterMember) {
        _memberToDelete.value = member
    }

    fun onDismissRemoveMemberDialog() {
        _memberToDelete.value = null
        _errorMessage.value = null
    }

    fun onClearError() {
        _errorMessage.value = null
    }

    // ── E2.1: WhatsApp Balance Share ────────────────────────────

    private val _shareText = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val shareText: SharedFlow<String> = _shareText.asSharedFlow()

    /**
     * Builds a formatted balance summary and emits it via [shareText].
     * @param playStoreLink Play Store URL from strings.xml
     */
    fun onShareBalance(playStoreLink: String) {
        val groupName = group.value?.name ?: "Group"
        val balances = memberBalances.value
        if (balances.isEmpty()) return

        val sb = StringBuilder()
        sb.appendLine("FinSave — $groupName")
        sb.appendLine()

        balances.forEach { balance ->
            val name = balance.member.name
            val net = balance.netBalancePaise
            when {
                net > 0 -> sb.appendLine("$name gets back: ${IndianNumberFormatter.format(net)}")
                net < 0 -> sb.appendLine("$name owes: ${IndianNumberFormatter.format(-net)}")
                else -> sb.appendLine("$name — settled up ✅")
            }
        }

        // Simplified debts summary
        val debts = simplifiedDebts.value
        if (debts.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("To settle:")
            debts.forEach { debt ->
                sb.appendLine("${debt.fromMember.name} → ${debt.toMember.name}: ${IndianNumberFormatter.format(debt.amountPaise)}")
            }
        }

        sb.appendLine()
        sb.appendLine("Track expenses for free — no internet, 100% private:")
        sb.appendLine(playStoreLink)

        _shareText.tryEmit(sb.toString())
    }

    // ── E2.3: Settlement Confetti + Share ────────────────────────

    private val _isConfettiVisible = MutableStateFlow(false)
    val isConfettiVisible: StateFlow<Boolean> = _isConfettiVisible.asStateFlow()

    private val _showShareButtons = MutableStateFlow(false)
    val showShareButtons: StateFlow<Boolean> = _showShareButtons.asStateFlow()

    /**
     * Called after confetti finishes playing (2s delay in LaunchedEffect).
     * Hides confetti and shows Share/Done buttons.
     */
    fun onConfettiFinished() {
        _isConfettiVisible.value = false
        _showShareButtons.value = true
    }

    /**
     * Triggers confetti overlay after a successful settlement.
     */
    fun showConfetti() {
        _isConfettiVisible.value = true
        _showShareButtons.value = false
    }

    /**
     * Emits a "We're Settled!" share message via [shareText].
     */
    fun onShareSettlement() {
        val groupName = group.value?.name ?: "Group"
        val text = buildString {
            appendLine("$groupName — all settled! 🎉")
            appendLine("We used FinSave to split our bills — no internet, 100% private.")
            appendLine("Try it free: ${Constants.PLAY_STORE_LINK}")
        }
        _shareText.tryEmit(text)
    }
}

/**
 * UI state for Group Detail Screen.
 */
data class GroupDetailUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
