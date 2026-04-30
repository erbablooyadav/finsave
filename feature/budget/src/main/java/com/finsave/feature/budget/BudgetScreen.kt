package com.finsave.feature.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finsave.core.ui.theme.LocalSpacing
import com.finsave.domain.model.BudgetSummary

@Composable
fun BudgetScreen(
    modifier: Modifier = Modifier,
    viewModel: BudgetViewModel = hiltViewModel()
) {
    val budgetSummaries by viewModel.budgetSummaries.collectAsState()
    val dialogBudget by viewModel.dialogBudget.collectAsState()
    val useIndianSystem by viewModel.useIndianNumberSystem.collectAsState()
    val spacing = LocalSpacing.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            // FAB is always visible (Requirement 6.1)
            FloatingActionButton(
                onClick = { viewModel.onAddBudgetClick() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Budget")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = spacing.large),
            contentPadding = PaddingValues(vertical = spacing.large),
            verticalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            item {
                Text(
                    text = "Budgets",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(spacing.medium))
            }

            if (budgetSummaries.isEmpty()) {
                item {
                    EmptyBudgetView()
                }
            } else {
                items(budgetSummaries, key = { it.budget.id }) { summary ->
                    BudgetProgressCard(
                        summary = summary,
                        useIndianSystem = useIndianSystem,
                        onClick = { viewModel.onEditBudget(summary.budget) },
                        onDelete = { viewModel.onDeleteBudget(summary.budget) }
                    )
                }
            }
        }
    }

    // Budget dialog
    if (dialogBudget != null) {
        BudgetDialog(
            budget = dialogBudget,
            onDismiss = { viewModel.onDismissDialog() }
        )
    }
}

@Composable
fun BudgetProgressCard(
    summary: BudgetSummary,
    useIndianSystem: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val spacing = LocalSpacing.current
    val progressColor = when {
        summary.percentUsed < 0.5f -> Color(0xFF4CAF50) // Green
        summary.percentUsed < 0.8f -> Color(0xFFFFC107) // Yellow
        else -> Color(0xFFF44336) // Red
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(spacing.large)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    val category = summary.category
                    if (category != null) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(category.emoji, style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(modifier = Modifier.width(spacing.medium))
                    }
                    Text(
                        text = summary.category?.name ?: "Total Monthly Budget",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${(summary.percentUsed * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = progressColor
                    )
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete budget",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(spacing.medium))
            
            LinearProgressIndicator(
                progress = { summary.percentUsed.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            
            Spacer(modifier = Modifier.height(spacing.medium))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Spent", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = com.finsave.core.common.formatter.IndianNumberFormatter.format(
                            paiseAmount = summary.spentPaise,
                            useIndianSystem = useIndianSystem,
                            showPaise = false,
                            showSymbol = true
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Remaining", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = com.finsave.core.common.formatter.IndianNumberFormatter.format(
                            paiseAmount = summary.remainingPaise,
                            useIndianSystem = useIndianSystem,
                            showPaise = false,
                            showSymbol = true
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyBudgetView() {
    val spacing = LocalSpacing.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = spacing.huge),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No active budgets",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Tap the + button to set a budget limit",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
