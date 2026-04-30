package com.finsave.feature.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finsave.core.common.formatter.IndianNumberFormatter
import com.finsave.core.ui.components.FinSaveCard
import com.finsave.core.ui.components.GradientHeaderCard
import com.finsave.core.ui.theme.LocalSpacing
import com.finsave.domain.model.Account
import com.finsave.domain.model.AccountType

/**
 * Account Management Screen.
 * 
 * Displays:
 * - Total balance header card
 * - List of all accounts with name, type, color, balance
 * - Add account FAB
 * 
 * Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9, 4.10, 14.5
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    onNavigateBack: () -> Unit,
    viewModel: AccountViewModel = hiltViewModel()
) {
    val spacing = LocalSpacing.current

    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val totalBalance by viewModel.totalBalance.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dialogAccount by viewModel.dialogAccount.collectAsStateWithLifecycle()
    val deleteConfirmation by viewModel.deleteConfirmation.collectAsStateWithLifecycle()

    // Show error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onClearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Accounts") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onAddAccountClick() }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Account")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
            contentPadding = PaddingValues(vertical = spacing.medium)
        ) {
            // Total balance header card
            item {
                TotalBalanceCard(
                    totalBalancePaise = totalBalance,
                    accountCount = accounts.size
                )
            }

            // Account list
            items(accounts, key = { it.id }) { account ->
                AccountCard(
                    account = account,
                    onClick = { viewModel.onEditAccount(account) },
                    onSetDefault = { viewModel.onSetDefault(account) }
                )
            }

            // Empty state
            if (accounts.isEmpty()) {
                item {
                    EmptyAccountsState()
                }
            }
        }
    }

    // Account dialog
    if (dialogAccount != null) {
        AccountDialog(
            account = dialogAccount,
            errorMessage = uiState.errorMessage,
            onDismiss = { viewModel.onDismissDialog() },
            onSave = { id, name, type, balance, color, isDefault ->
                viewModel.onSaveAccount(id, name, type, balance, color, isDefault)
            },
            onDelete = if (dialogAccount?.id != 0L) {
                { account -> viewModel.onDeleteAccountClick(account) }
            } else null
        )
    }

    // Delete confirmation dialog
    if (deleteConfirmation != null) {
        DeleteConfirmationDialog(
            account = deleteConfirmation!!,
            onDismiss = { viewModel.onDismissDeleteConfirmation() },
            onConfirm = { viewModel.onConfirmDelete() }
        )
    }
}

/**
 * Total balance header card.
 * Requirement: 4.1
 */
@Composable
private fun TotalBalanceCard(
    totalBalancePaise: Long,
    accountCount: Int,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    GradientHeaderCard(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.small)
        ) {
            Text(
                text = "Total Balance",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
            Text(
                text = IndianNumberFormatter.format(totalBalancePaise),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "$accountCount ${if (accountCount == 1) "account" else "accounts"}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * Individual account card.
 * Requirement: 4.2
 */
@Composable
private fun AccountCard(
    account: Account,
    onClick: () -> Unit,
    onSetDefault: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    val accountColor = try {
        Color(android.graphics.Color.parseColor(account.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    FinSaveCard(
        onClick = onClick,
        modifier = modifier
    ) {
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
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(accountColor)
                )

                // Account info
                Column(
                    verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = account.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (account.isDefault) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Default account",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = formatAccountType(account.type),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Balance and default toggle
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)
            ) {
                Text(
                    text = IndianNumberFormatter.format(account.balancePaise),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (account.balancePaise >= 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
                IconButton(
                    onClick = onSetDefault,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (account.isDefault) Icons.Default.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (account.isDefault) "Remove default" else "Set as default",
                        tint = if (account.isDefault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Empty state when no accounts exist.
 */
@Composable
private fun EmptyAccountsState(
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(spacing.extraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.medium)
    ) {
        Text(
            text = "💳",
            style = MaterialTheme.typography.displayLarge
        )
        Text(
            text = "No accounts yet",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Add your first account to start tracking your finances",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Delete confirmation dialog.
 * Requirements: 4.7, 4.8, 4.10
 */
@Composable
private fun DeleteConfirmationDialog(
    account: Account,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Account?") },
        text = {
            Text("Are you sure you want to delete \"${account.name}\"? This action cannot be undone.")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
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
 * Formats account type for display.
 */
private fun formatAccountType(type: AccountType): String {
    return when (type) {
        AccountType.SAVINGS -> "Savings"
        AccountType.CURRENT -> "Current"
        AccountType.CREDIT_CARD -> "Credit Card"
        AccountType.WALLET -> "Wallet"
        AccountType.CASH -> "Cash"
        AccountType.INVESTMENT -> "Investment"
    }
}
