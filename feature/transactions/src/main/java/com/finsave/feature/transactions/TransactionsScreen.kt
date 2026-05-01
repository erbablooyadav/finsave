package com.finsave.feature.transactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finsave.core.common.formatter.IndianNumberFormatter
import com.finsave.domain.model.Transaction
import com.finsave.domain.model.TransactionType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TransactionsScreen(
    modifier: Modifier = Modifier,
    viewModel: TransactionsViewModel = hiltViewModel()
) {
    val transactions by viewModel.transactions.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    val selectedTransaction by viewModel.selectedTransaction.collectAsState()
    val deleteConfirmation by viewModel.deleteConfirmation.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    var showAdvancedFilters by remember { mutableStateOf(false) }
    val collapsedMonths = remember { mutableStateMapOf<String, Boolean>() }

    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Transactions") },
                    actions = {
                        IconButton(onClick = { showAdvancedFilters = true }) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Advanced filters"
                            )
                        }
                    }
                )
                // Search bar
                SearchBar(
                    query = filterState.query,
                    onQueryChange = viewModel::onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
                // Filter chips
                FilterChips(
                    selectedFilter = filterState.typeFilter,
                    onFilterChange = viewModel::onTypeFilterChange,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                DateFilterChips(
                    selectedPreset = filterState.datePreset,
                    onFilterChange = viewModel::onDatePresetChange,
                    onCustomClick = { showAdvancedFilters = true },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        },
        snackbarHost = {
            errorMessage?.let { message ->
                Snackbar(
                    action = {
                        TextButton(onClick = viewModel::onClearError) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(message)
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                transactions.isEmpty() && filterState.query.isBlank() &&
                        filterState.typeFilter == TransactionTypeFilter.ALL &&
                        filterState.startDate == null && filterState.endDate == null -> {
                    // Empty state - no transactions at all
                    EmptyState(
                        message = "No transactions yet — add one with the + button",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                transactions.isEmpty() -> {
                    // Empty state - no results after filtering
                    EmptyState(
                        message = "No transactions found",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    val groupedByMonth = transactions
                        .groupBy { YearMonth.from(it.date) }
                        .toSortedMap(compareByDescending<YearMonth> { it })
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        groupedByMonth.forEach { (month, monthTransactions) ->
                            val monthKey = "${month.year}-${month.monthValue}"
                            val isCollapsed = collapsedMonths[monthKey] ?: false
                            item(key = "header_${month.year}_${month.monthValue}") {
                                MonthHeader(
                                    month = month,
                                    monthTransactions = monthTransactions,
                                    isCollapsed = isCollapsed,
                                    onToggle = { collapsedMonths[monthKey] = !isCollapsed }
                                )
                            }
                            if (!isCollapsed) {
                                items(items = monthTransactions, key = { it.id }) { transaction ->
                                    TransactionRow(
                                        transaction = transaction,
                                        onClick = { viewModel.onTransactionClick(transaction) },
                                        onDelete = { viewModel.onSwipeToDelete(transaction) },
                                        modifier = Modifier.animateItem()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit bottom sheet
    if (selectedTransaction != null) {
        AddTransactionBottomSheet(
            transaction = selectedTransaction,
            onDismissRequest = viewModel::onDismissEditSheet
        )
    }

    // Delete confirmation dialog
    if (deleteConfirmation != null) {
        AlertDialog(
            onDismissRequest = viewModel::onDismissDeleteConfirmation,
            title = { Text("Delete Transaction") },
            text = { Text("Are you sure you want to delete this transaction? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = viewModel::onConfirmDelete) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDismissDeleteConfirmation) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAdvancedFilters) {
        AdvancedFiltersSheet(
            filterState = filterState,
            onDismiss = { showAdvancedFilters = false },
            onReset = {
                viewModel.resetFilters()
                showAdvancedFilters = false
            },
            onApply = { minAmountPaise, maxAmountPaise, startDate, endDate, sortOption ->
                viewModel.onAmountRangeChange(minAmountPaise, maxAmountPaise)
                viewModel.onDatePresetChange(TransactionDatePreset.CUSTOM)
                viewModel.onDateRangeChange(startDate, endDate)
                viewModel.onSortOptionChange(sortOption)
                showAdvancedFilters = false
            }
        )
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text("Search transactions...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun FilterChips(
    selectedFilter: TransactionTypeFilter,
    onFilterChange: (TransactionTypeFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        items(TransactionTypeFilter.values()) { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterChange(filter) },
                label = { Text(filter.name.lowercase().replaceFirstChar { it.uppercase() }) }
            )
        }
    }
}

@Composable
private fun DateFilterChips(
    selectedPreset: TransactionDatePreset,
    onFilterChange: (TransactionDatePreset) -> Unit,
    onCustomClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        items(TransactionDatePreset.values()) { preset ->
            FilterChip(
                selected = selectedPreset == preset,
                onClick = {
                    if (preset == TransactionDatePreset.CUSTOM) onCustomClick() else onFilterChange(preset)
                },
                label = {
                    Text(
                        when (preset) {
                            TransactionDatePreset.ALL_TIME -> "All Time"
                            TransactionDatePreset.THIS_MONTH -> "This Month"
                            TransactionDatePreset.LAST_3_MONTHS -> "Last 3M"
                            TransactionDatePreset.THIS_YEAR -> "This Year"
                            TransactionDatePreset.CUSTOM -> "Custom"
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun MonthHeader(
    month: YearMonth,
    monthTransactions: List<Transaction>,
    isCollapsed: Boolean,
    onToggle: () -> Unit
) {
    val monthDebit = monthTransactions
        .filter { it.type == TransactionType.DEBIT }
        .sumOf { it.amountPaise }
    val monthCredit = monthTransactions
        .filter { it.type == TransactionType.CREDIT }
        .sumOf { it.amountPaise }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = "In: ${IndianNumberFormatter.format(monthCredit)}  Out: ${IndianNumberFormatter.format(monthDebit)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdvancedFiltersSheet(
    filterState: TransactionFilterState,
    onDismiss: () -> Unit,
    onReset: () -> Unit,
    onApply: (
        minAmountPaise: Long?,
        maxAmountPaise: Long?,
        startDate: LocalDate?,
        endDate: LocalDate?,
        sortOption: TransactionSortOption
    ) -> Unit
) {
    var minAmountRupees by remember {
        mutableStateOf(
            filterState.minAmountPaise?.let { String.format("%.2f", it / 100.0) } ?: ""
        )
    }
    var maxAmountRupees by remember {
        mutableStateOf(
            filterState.maxAmountPaise?.let { String.format("%.2f", it / 100.0) } ?: ""
        )
    }
    var startDateInput by remember { mutableStateOf(filterState.startDate?.toString() ?: "") }
    var endDateInput by remember { mutableStateOf(filterState.endDate?.toString() ?: "") }
    var selectedSortOption by remember { mutableStateOf(filterState.sortOption) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Advanced Filters",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                value = minAmountRupees,
                onValueChange = { minAmountRupees = it },
                label = { Text("Min Amount (INR)") },
                placeholder = { Text("e.g. 100.00") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = maxAmountRupees,
                onValueChange = { maxAmountRupees = it },
                label = { Text("Max Amount (INR)") },
                placeholder = { Text("e.g. 5000.00") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = startDateInput,
                onValueChange = { startDateInput = it },
                label = { Text("Start Date (YYYY-MM-DD)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = endDateInput,
                onValueChange = { endDateInput = it },
                label = { Text("End Date (YYYY-MM-DD)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Sort",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(TransactionSortOption.values()) { option ->
                    FilterChip(
                        selected = selectedSortOption == option,
                        onClick = { selectedSortOption = option },
                        label = {
                            Text(
                                when (option) {
                                    TransactionSortOption.DATE_DESC -> "Newest"
                                    TransactionSortOption.DATE_ASC -> "Oldest"
                                    TransactionSortOption.AMOUNT_DESC -> "Amount High"
                                    TransactionSortOption.AMOUNT_ASC -> "Amount Low"
                                }
                            )
                        }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onReset) { Text("Reset") }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        onApply(
                            minAmountRupees.toPaiseOrNull(),
                            maxAmountRupees.toPaiseOrNull(),
                            startDateInput.toLocalDateOrNull(),
                            endDateInput.toLocalDateOrNull(),
                            selectedSortOption
                        )
                    }
                ) {
                    Text("Apply")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

private fun String.toPaiseOrNull(): Long? {
    val cleaned = replace(",", "").replace("₹", "").trim()
    if (cleaned.isBlank()) return null
    val rupees = cleaned.toDoubleOrNull() ?: return null
    return (rupees * 100).toLong()
}

private fun String.toLocalDateOrNull(): LocalDate? {
    val cleaned = trim()
    if (cleaned.isBlank()) return null
    return runCatching { LocalDate.parse(cleaned) }.getOrNull()
}

@Composable
private fun TransactionRow(
    transaction: Transaction,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = transaction.merchantName.ifBlank { "Transaction" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = transaction.date.format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (transaction.note.isNotBlank()) {
                    Text(
                        text = transaction.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = IndianNumberFormatter.format(transaction.amountPaise),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = when (transaction.type) {
                        TransactionType.DEBIT -> MaterialTheme.colorScheme.error
                        TransactionType.CREDIT -> Color(0xFF10B981)
                        TransactionType.TRANSFER -> MaterialTheme.colorScheme.onSurface
                    }
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete transaction",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "📊",
            style = MaterialTheme.typography.displayLarge
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
