package com.finsave.feature.dashboard

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finsave.core.common.formatter.IndianNumberFormatter
import com.finsave.core.ui.components.FinSaveCard
import com.finsave.core.ui.components.GradientHeaderCard
import com.finsave.core.ui.theme.Indigo600
import com.finsave.core.ui.theme.Indigo800
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
    val smsImportProgress by viewModel.smsImportProgress.collectAsState()
    val smsImportCompletion by viewModel.smsImportCompletion.collectAsState()
    val spacing = LocalSpacing.current
    val snackbarHostState = remember { SnackbarHostState() }
    var lastShownCompletionId by rememberSaveable { mutableStateOf("") }

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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            item {
                Spacer(modifier = Modifier.height(spacing.medium))

                GradientHeaderCard(
                    gradientColors = listOf(Indigo600, Indigo800)
                ) {
                    Column {
                        Text(
                            text = "Spent this month",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(spacing.extraSmall))
                        Text(
                            text = IndianNumberFormatter.format(
                                paiseAmount = totalSpendPaise,
                                useIndianSystem = useIndianSystem,
                                showPaise = true,
                                showSymbol = true
                            ),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.semantics {
                                contentDescription = IndianNumberFormatter.formatForAccessibility(totalSpendPaise)
                            }
                        )
                        Spacer(modifier = Modifier.height(spacing.medium))
                        
                        // Days remaining banner
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                                .padding(spacing.small),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(spacing.small))
                            Text(
                                text = "${viewModel.daysRemaining} days left in month",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            item {
                FinSaveScoreCard(
                    score = if (hasTotalBudget) finSaveScore else null,
                    streakCount = if (hasTotalBudget) budgetStreakCount else null
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
                            Text(
                                text = "Your transactions will appear here",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                modifier = Modifier.fillMaxWidth()
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

@Composable
fun FinSaveScoreCard(
    score: Int?,
    streakCount: Int?
) {
    val spacing = LocalSpacing.current
    
    FinSaveCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // FinSave Score
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "FinSave Score",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(spacing.extraSmall))
                Text(
                    text = score?.toString() ?: "—",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (score != null) {
                    Text(
                        text = "out of 100",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(80.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            
            // Budget Streak
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Budget Streak",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(spacing.extraSmall))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (streakCount != null && streakCount >= 3) {
                        Text(
                            text = "🔥",
                            style = MaterialTheme.typography.displayMedium
                        )
                        Spacer(modifier = Modifier.width(spacing.extraSmall))
                    }
                    Text(
                        text = streakCount?.toString() ?: "—",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (streakCount != null && streakCount >= 3) 
                            Color(0xFFFF6B35) else MaterialTheme.colorScheme.primary
                    )
                }
                if (streakCount != null) {
                    Text(
                        text = if (streakCount == 1) "day" else "days",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
