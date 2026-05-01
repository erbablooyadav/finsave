package com.finsave.feature.splitter

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finsave.core.common.formatter.IndianNumberFormatter
import com.finsave.core.ui.components.FinSaveCard
import com.finsave.core.ui.theme.LocalSpacing
import com.finsave.domain.model.MemberBalance
import com.finsave.domain.model.SimplifiedDebt
import com.finsave.domain.model.SplitterExpense
import com.finsave.domain.model.SplitterMember
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

/**
 * Group Detail Screen — Shows members, expenses, balances, and debts for a splitter group.
 * 
 * Features:
 * - Members tab: List of members with add/remove actions
 * - Expenses tab: List of expenses with add action
 * - Balance summary: Per-member balance ("gets back" / "owes")
 * - Simplified debt list: Minimal payment transactions to settle
 * - UPI settle-up: One-tap payment via UPI deep link
 * 
 * Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7, 7.8, 7.9, 7.10, 7.11, 7.12, 7.13, 7.14, 14.6, 15.1, 15.2, 15.3, 15.4, 15.5
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: GroupDetailViewModel = hiltViewModel()
) {
    val spacing = LocalSpacing.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val group by viewModel.group.collectAsStateWithLifecycle()
    val members by viewModel.members.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val memberBalances by viewModel.memberBalances.collectAsStateWithLifecycle()
    val simplifiedDebts by viewModel.simplifiedDebts.collectAsStateWithLifecycle()
    val isGroupSettleable by viewModel.isGroupSettleable.collectAsStateWithLifecycle()
    val isSettled by viewModel.isSettled.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val showAddMemberDialog by viewModel.showAddMemberDialog.collectAsStateWithLifecycle()
    val showAddExpenseDialog by viewModel.showAddExpenseDialog.collectAsStateWithLifecycle()
    val memberToDelete by viewModel.memberToDelete.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showSettleConfirmation by remember { mutableStateOf(false) }
    var showSettledOverlay by remember { mutableStateOf(false) }
    val settledOverlayAlpha by animateFloatAsState(
        targetValue = if (showSettledOverlay) 1f else 0f,
        animationSpec = tween(durationMillis = 250),
        label = "settledOverlayAlpha"
    )

    LaunchedEffect(showSettledOverlay) {
        if (showSettledOverlay) {
            delay(2_000)
            onNavigateBack()
        }
    }

    // Show error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onClearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing.small),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(group?.emoji ?: "")
                        Text(group?.name ?: "Group")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 0) {
                        viewModel.onAddMemberClick()
                    } else {
                        viewModel.onAddExpenseClick()
                    }
                }
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = if (selectedTab == 0) "Add member" else "Add expense"
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Tabs
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Members") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Expenses") }
                    )
                }

                // Tab content
                when (selectedTab) {
                    0 -> MembersTab(
                        members = members,
                        memberBalances = memberBalances,
                        simplifiedDebts = simplifiedDebts,
                        groupName = group?.name ?: "",
                        isGroupSettleable = isGroupSettleable,
                        isSettled = isSettled,
                        onRemoveMember = { viewModel.onRemoveMemberClick(it) },
                        onSettleGroup = { showSettleConfirmation = true },
                        onSettleViaUpi = { debt ->
                            val uri = viewModel.buildUpiUri(debt, group?.name ?: "")
                            if (uri == null) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Invalid UPI details. Please update the member's UPI ID.")
                                }
                            } else {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                                    context.startActivity(intent)
                                } catch (e: ActivityNotFoundException) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("No UPI app found. Please install Google Pay, PhonePe, or Paytm.")
                                    }
                                }
                            }
                        }
                    )
                    1 -> ExpensesTab(
                        expenses = expenses,
                        members = members
                    )
                }
            }

            if (showSettledOverlay) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f * settledOverlayAlpha))
                        .alpha(settledOverlayAlpha),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = Color(0xFF16A34A),
                        tonalElevation = 8.dp
                    ) {
                        Text(
                            text = "All Settled!",
                            modifier = Modifier.padding(horizontal = 32.dp, vertical = 24.dp),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    // Add Member Dialog
    if (showAddMemberDialog) {
        AddMemberDialog(
            onDismiss = { viewModel.onDismissAddMemberDialog() },
            onAddMember = { name, upiId ->
                viewModel.addMember(name, upiId)
            }
        )
    }

    // Add Expense Dialog
    if (showAddExpenseDialog && members.isNotEmpty()) {
        AddExpenseDialog(
            members = members,
            onDismiss = { viewModel.onDismissAddExpenseDialog() },
            onAddExpense = { description, amountPaise, paidByMemberId, splitType, splits ->
                viewModel.addExpense(description, amountPaise, paidByMemberId, splitType, splits)
            }
        )
    }

    // Delete Member Confirmation
    if (memberToDelete != null) {
        DeleteMemberDialog(
            member = memberToDelete!!,
            onDismiss = { viewModel.onDismissRemoveMemberDialog() },
            onConfirm = {
                viewModel.removeMember(memberToDelete!!)
            }
        )
    }

    if (showSettleConfirmation) {
        AlertDialog(
            onDismissRequest = { showSettleConfirmation = false },
            title = { Text("Settle & Archive Group?") },
            text = { Text("This will mark the group as settled and move it to Settled Groups.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSettleConfirmation = false
                        viewModel.settleGroup()
                        showSettledOverlay = true
                    }
                ) {
                    Text("Settle")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettleConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Members tab — Shows member list, balances, and simplified debts.
 * Requirements: 7.1, 7.2, 7.11, 7.13, 7.14
 */
@Composable
private fun MembersTab(
    members: List<SplitterMember>,
    memberBalances: List<MemberBalance>,
    simplifiedDebts: List<SimplifiedDebt>,
    groupName: String,
    isGroupSettleable: Boolean,
    isSettled: Boolean,
    onRemoveMember: (SplitterMember) -> Unit,
    onSettleGroup: () -> Unit,
    onSettleViaUpi: (SimplifiedDebt) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = spacing.medium),
        contentPadding = PaddingValues(vertical = spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.medium)
    ) {
        // Balance summary section
        if (memberBalances.isNotEmpty()) {
            item {
                Text(
                    text = "Balance Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = spacing.small)
                )
            }

            items(memberBalances, key = { "balance_${it.member.id}" }) { balance ->
                MemberBalanceCard(
                    balance = balance,
                    onRemove = { onRemoveMember(balance.member) }
                )
            }
        }

        // Simplified debts section
        if (simplifiedDebts.isNotEmpty()) {
            item {
                Text(
                    text = "Settle Up",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = spacing.medium, bottom = spacing.small)
                )
            }

            items(simplifiedDebts, key = { "debt_${it.fromMember.id}_${it.toMember.id}" }) { debt ->
                SimplifiedDebtCard(
                    debt = debt,
                    onSettleViaUpi = { onSettleViaUpi(debt) }
                )
            }
        }

        // Members list section
        item {
            Text(
                text = "Members",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = spacing.medium, bottom = spacing.small)
            )
        }

        if (members.isEmpty()) {
            item {
                EmptyMembersState()
            }
        } else {
            items(members, key = { "member_${it.id}" }) { member ->
                MemberCard(
                    member = member,
                    onRemove = { onRemoveMember(member) }
                )
            }
        }

        if (isGroupSettleable && !isSettled) {
            item {
                Button(
                    onClick = onSettleGroup,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = spacing.medium),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF16A34A),
                        contentColor = Color.White
                    )
                ) {
                    Text("Settle & Archive Group")
                }
            }
        }
    }
}

/**
 * Expenses tab — Shows expense list.
 * Requirement: 7.1
 */
@Composable
private fun ExpensesTab(
    expenses: List<SplitterExpense>,
    members: List<SplitterMember>,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = spacing.medium),
        contentPadding = PaddingValues(vertical = spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.medium)
    ) {
        if (expenses.isEmpty()) {
            item {
                EmptyExpensesState()
            }
        } else {
            items(expenses, key = { it.id }) { expense ->
                ExpenseCard(
                    expense = expense,
                    paidByMember = members.find { it.id == expense.paidByMemberId }
                )
            }
        }
    }
}

/**
 * Member balance card — Shows member's net balance.
 * Requirement: 7.2
 */
@Composable
private fun MemberBalanceCard(
    balance: MemberBalance,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val member = balance.member
    val netBalance = balance.netBalancePaise

    val memberColor = try {
        Color(android.graphics.Color.parseColor(member.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    FinSaveCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.medium),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Color indicator
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(memberColor)
                )

                // Member info
                Column {
                    Text(
                        text = member.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = when {
                            netBalance > 0 -> "gets back ${IndianNumberFormatter.format(netBalance)}"
                            netBalance < 0 -> "owes ${IndianNumberFormatter.format(-netBalance)}"
                            else -> "settled up"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = when {
                            netBalance > 0 -> Color(0xFF4CAF50)
                            netBalance < 0 -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.semantics {
                            contentDescription = when {
                                netBalance > 0 -> "${member.name} gets back ${IndianNumberFormatter.formatForAccessibility(netBalance)}"
                                netBalance < 0 -> "${member.name} owes ${IndianNumberFormatter.formatForAccessibility(-netBalance)}"
                                else -> "${member.name} settled up"
                            }
                        }
                    )
                }
            }

            // Remove button
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove member",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * Simplified debt card — Shows a single payment to settle.
 * Requirements: 7.13, 7.14, 15.1, 15.2, 15.3
 */
@Composable
private fun SimplifiedDebtCard(
    debt: SimplifiedDebt,
    onSettleViaUpi: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val hasUpiId = debt.toMember.upiId != null

    FinSaveCard(modifier = modifier) {
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.small)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${debt.fromMember.name} → ${debt.toMember.name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = IndianNumberFormatter.format(debt.amountPaise),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.semantics {
                            contentDescription = "${debt.fromMember.name} owes ${debt.toMember.name} ${IndianNumberFormatter.formatForAccessibility(debt.amountPaise)}"
                        }
                    )
                }
            }

            // Settle via UPI button
            Button(
                onClick = onSettleViaUpi,
                enabled = hasUpiId,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (hasUpiId) "Settle via UPI" else "No UPI ID on file")
            }
        }
    }
}

/**
 * Member card — Shows member info.
 * Requirement: 7.1
 */
@Composable
private fun MemberCard(
    member: SplitterMember,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    val memberColor = try {
        Color(android.graphics.Color.parseColor(member.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    FinSaveCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.medium),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Color indicator
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(memberColor)
                )

                // Member info
                Column {
                    Text(
                        text = member.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    val upiIdValue = member.upiId
                    if (upiIdValue != null) {
                        Text(
                            text = upiIdValue,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Remove button
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove member",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * Expense card — Shows expense details.
 * Requirement: 7.1, 14.6
 */
@Composable
private fun ExpenseCard(
    expense: SplitterExpense,
    paidByMember: SplitterMember?,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    FinSaveCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)
            ) {
                Text(
                    text = expense.description,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Paid by ${paidByMember?.name ?: "Unknown"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = expense.date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = IndianNumberFormatter.format(expense.amountPaise),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics {
                    contentDescription = IndianNumberFormatter.formatForAccessibility(expense.amountPaise)
                }
            )
        }
    }
}

/**
 * Empty state for members.
 */
@Composable
private fun EmptyMembersState(modifier: Modifier = Modifier) {
    val spacing = LocalSpacing.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(spacing.extraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.medium)
    ) {
        Text(text = "👥", style = MaterialTheme.typography.displayLarge)
        Text(
            text = "No members yet",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Add members to start splitting expenses",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Empty state for expenses.
 */
@Composable
private fun EmptyExpensesState(modifier: Modifier = Modifier) {
    val spacing = LocalSpacing.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(spacing.extraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.medium)
    ) {
        Text(text = "💰", style = MaterialTheme.typography.displayLarge)
        Text(
            text = "No expenses yet",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Add an expense to start tracking",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Add Member Dialog.
 * Requirements: 7.3, 7.4, 7.5
 */
@Composable
private fun AddMemberDialog(
    onDismiss: () -> Unit,
    onAddMember: (String, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var upiId by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Member") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = upiId,
                    onValueChange = { upiId = it },
                    label = { Text("UPI ID (optional)") },
                    placeholder = { Text("name@upi") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onAddMember(name, upiId.takeIf { it.isNotBlank() })
                },
                enabled = name.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Delete Member Confirmation Dialog.
 * Requirement: 7.11
 */
@Composable
private fun DeleteMemberDialog(
    member: SplitterMember,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Remove Member?") },
        text = {
            Text("Are you sure you want to remove ${member.name} from this group?")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Remove")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
