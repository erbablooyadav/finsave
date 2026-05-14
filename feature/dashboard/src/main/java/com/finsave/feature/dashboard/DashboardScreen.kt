package com.finsave.feature.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finsave.core.common.formatter.IndianNumberFormatter
import com.finsave.core.ui.components.AnimatedSpendHeaderCard
import com.finsave.core.ui.components.ConfettiOverlay
import com.finsave.core.ui.components.DashboardSkeleton
import com.finsave.core.ui.components.FinSaveCard
import com.finsave.core.ui.components.StreakCard
import com.finsave.core.ui.theme.LocalSpacing
import com.finsave.domain.model.Transaction
import com.finsave.domain.model.TransactionType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DashboardScreen(
    onAddTransactionClick: () -> Unit,
    onNavigateToTransactions: () -> Unit = {},
    onNavigateToSplitter: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val totalSpendPaise by viewModel.totalSpendPaise.collectAsState()
    val recentTransactions by viewModel.recentTransactions.collectAsState()
    val totalBalancePaise by viewModel.totalBalancePaise.collectAsState()
    val useIndianSystem by viewModel.useIndianNumberSystem.collectAsState()
    val hasTotalBudget by viewModel.hasTotalBudget.collectAsState()
    val finSaveScore by viewModel.finSaveScore.collectAsState()
    val budgetStreakCount by viewModel.budgetStreakCount.collectAsState()
    val totalBudgetPaise by viewModel.totalBudgetPaise.collectAsState()
    val daysRemaining by viewModel.daysRemaining.collectAsState()
    val smsImportProgress by viewModel.smsImportProgress.collectAsState()
    val smsImportCompletion by viewModel.smsImportCompletion.collectAsState()
    val bestStreak by viewModel.bestStreak.collectAsState()
    val isStreakBroken by viewModel.isStreakBroken.collectAsState()
    val showFirstTransactionConfetti by viewModel.showFirstTransactionConfetti.collectAsState()
    val spacing = LocalSpacing.current
    val snackbarHostState = remember { SnackbarHostState() }
    var lastShownCompletionId by rememberSaveable { mutableStateOf("") }

    // Loading detection: show skeleton until first real data arrives
    var isLoading by remember { mutableStateOf(true) }
    LaunchedEffect(recentTransactions, totalSpendPaise) {
        if (recentTransactions.isNotEmpty() || totalSpendPaise != 0L) {
            isLoading = false
        }
    }
    // Auto-dismiss loading after a short timeout to handle empty-data case
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(800)
        isLoading = false
    }

    LaunchedEffect(smsImportCompletion?.workId) {
        val completion = smsImportCompletion ?: return@LaunchedEffect
        if (completion.workId.toString() != lastShownCompletionId) {
            snackbarHostState.showSnackbar(
                message = "SMS import complete: parsed ${completion.parsed}/${completion.total}, imported ${completion.imported}"
            )
            lastShownCompletionId = completion.workId.toString()
            viewModel.markSmsImportCompletionShown(completion.workId)
        }
    }

    LaunchedEffect(showFirstTransactionConfetti) {
        if (showFirstTransactionConfetti) {
            kotlinx.coroutines.delay(4000)
            viewModel.markConfettiShown()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { com.finsave.core.ui.components.FinSaveSnackbar(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransactionClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
            }
        }
    ) { paddingValues ->
        Crossfade(
            targetState = isLoading,
            label = "dashboardLoading"
        ) { loading ->
            if (loading) {
                DashboardSkeleton(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            item {
                Spacer(modifier = Modifier.height(spacing.medium))

                AnimatedSpendHeaderCard(
                    totalSpentPaise = totalSpendPaise,
                    totalBudgetPaise = totalBudgetPaise,
                    daysRemaining = daysRemaining,
                    hasTotalBudget = hasTotalBudget,
                    useIndianSystem = useIndianSystem
                )
            }

            item {
                val haptic = LocalHapticFeedback.current
                StreakCard(
                    streakCount = if (hasTotalBudget) budgetStreakCount else 0,
                    bestStreak = bestStreak,
                    isStreakBroken = isStreakBroken,
                    score = if (hasTotalBudget) finSaveScore else 0,
                    modifier = Modifier.clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                )
            }

            if (smsImportProgress != null) {
                item {
                    SmsImportProgressCard(
                        progress = smsImportProgress!!,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                FinSaveCard {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recent Transactions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "See all",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { onNavigateToTransactions() }
                            )
                        }
                        Spacer(modifier = Modifier.height(spacing.medium))
                        
                        if (recentTransactions.isEmpty()) {
                            com.finsave.core.ui.components.EmptyState(
                                message = "No transactions yet. Start your journey by adding one!",
                                icon = androidx.compose.material.icons.Icons.Default.Add
                            )
                        }
                    }
                }
            }

            if (recentTransactions.isNotEmpty()) {
                items(recentTransactions, key = { it.id }) { transaction ->
                    TransactionItem(
                        transaction = transaction,
                        useIndianSystem = useIndianSystem
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(spacing.huge))
            }
        }
            } // else
        } // Crossfade

        ConfettiOverlay(
            visible = showFirstTransactionConfetti
        )
    }
}

@Composable
private fun SmsImportProgressCard(
    progress: SmsImportProgressUi,
    modifier: Modifier = Modifier
) {
    FinSaveCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Importing SMS transactions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            LinearProgressIndicator(
                progress = {
                    if (progress.total <= 0) 0f else progress.parsed.toFloat() / progress.total.toFloat()
                },
                modifier = Modifier.fillMaxWidth().semantics {
                    contentDescription = "Importing SMS: ${((if (progress.total <= 0) 0f else progress.parsed.toFloat() / progress.total.toFloat()) * 100).toInt()}% complete"
                }
            )
            Text(
                text = "Parsed: ${progress.parsed}/${progress.total}  •  Imported: ${progress.imported}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TransactionItem(
    transaction: Transaction,
    useIndianSystem: Boolean
) {
    val spacing = LocalSpacing.current
    val formatter = DateTimeFormatter.ofPattern("MMM dd", Locale.getDefault())
    val dateStr = transaction.date.format(formatter)
        
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(spacing.medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = transaction.merchantName.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.width(spacing.medium))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.merchantName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = dateStr,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        val isExpense = transaction.type == TransactionType.DEBIT
        val formattedAmount = IndianNumberFormatter.format(
            paiseAmount = transaction.amountPaise,
            useIndianSystem = useIndianSystem,
            showPaise = true,
            showSymbol = true
        )
        Text(
            text = "${if(isExpense) "-" else "+"}$formattedAmount",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isExpense) MaterialTheme.colorScheme.onSurface else Color(0xFF4CAF50),
            modifier = Modifier.semantics {
                val amountDescription = IndianNumberFormatter.formatForAccessibility(transaction.amountPaise)
                contentDescription = if (isExpense) "Spent $amountDescription" else "Received $amountDescription"
            }
        )
    }
}

// FinSaveScoreCard was removed and replaced by StreakCard in CommonComponents.kt
