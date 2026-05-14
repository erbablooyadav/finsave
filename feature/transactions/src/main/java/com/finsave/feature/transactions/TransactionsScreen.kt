package com.finsave.feature.transactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finsave.core.common.formatter.IndianNumberFormatter
import com.finsave.domain.model.Category
import com.finsave.domain.model.Transaction
import com.finsave.domain.model.TransactionType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.YearMonth
import com.finsave.core.ui.components.TransactionsSkeleton
import com.finsave.core.ui.components.EmptyStateView
import com.finsave.core.ui.R
import com.finsave.core.ui.components.FinSaveSnackbar

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
    val snackbarHostState = remember { SnackbarHostState() }
    var showAdvancedFilters by remember { mutableStateOf(false) }
    val collapsedMonths = remember { mutableStateMapOf<String, Boolean>() }
    val categories by viewModel.categories.collectAsState()

    // Loading detection
    var isLoading by remember { mutableStateOf(true) }
    LaunchedEffect(transactions) {
        if (transactions.isNotEmpty()) isLoading = false
    }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(800)
        isLoading = false
    }

    // Collect snackbar events (undo support)
    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { event ->
            val result = snackbarHostState.showSnackbar(
                message = event.message,
                actionLabel = event.actionLabel,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                event.onAction?.invoke()
            }
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onClearError()
        }
    }

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
        snackbarHost = { FinSaveSnackbar(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                TransactionsSkeleton(
                    modifier = Modifier.fillMaxSize()
                )
            } else {
            when {
                transactions.isEmpty() && filterState.query.isBlank() &&
                        filterState.typeFilter == TransactionTypeFilter.ALL &&
                        filterState.startDate == null && filterState.endDate == null -> {
                                    // Empty state - no transactions at all
                    EmptyStateView(
                        vectorRes = R.drawable.ic_empty_transactions,
                        title = "No transactions yet",
                        body = "Add your first transaction using the + button below",
                        ctaText = "Add Transaction",
                        onCtaClick = { /* AddTransaction handled by FAB on parent */ },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                transactions.isEmpty() -> {
                                    // Empty state - no results after filtering
                    EmptyStateView(
                        vectorRes = R.drawable.ic_empty_transactions,
                        title = "No results found",
                        body = "Try adjusting your search or filters",
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
                                    TransactionRowItem(
                                        transaction = transaction,
                                        category = categories[transaction.categoryId],
                                        onClick = { viewModel.onTransactionClick(transaction) },
                                        onDelete = { viewModel.onSwipeDelete(transaction) },
                                        modifier = Modifier.animateItem()
                                    )
                                }
                            }
                        }
                    }
                }
            }
            } // else isLoading
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics {
                    contentDescription = "Income ${IndianNumberFormatter.formatForAccessibility(monthCredit)}, spending ${IndianNumberFormatter.formatForAccessibility(monthDebit)}"
                }
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

/**
 * TransactionRowItem — Redesigned transaction row with category color, SMS badge, and swipe-to-delete.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionRowItem(
    transaction: Transaction,
    category: Category?,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            // Red background with trash icon
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.error)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onError
                )
            }
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category emoji circle with color
                val categoryColor = category?.colorHex?.let { hex ->
                    try {
                        Color(android.graphics.Color.parseColor(hex))
                    } catch (_: Exception) {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                } ?: MaterialTheme.colorScheme.surfaceVariant

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(categoryColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = category?.emoji ?: transaction.merchantName.take(1).uppercase(),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Merchant name + relative time
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.merchantName.ifBlank { "Transaction" },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatRelativeTime(transaction.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // AUTO badge for SMS-imported transactions
                if (transaction.isAutoImported) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "AUTO",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Amount with color
                Text(
                    text = IndianNumberFormatter.format(transaction.amountPaise),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when (transaction.type) {
                        TransactionType.DEBIT -> MaterialTheme.colorScheme.error
                        TransactionType.CREDIT -> MaterialTheme.colorScheme.secondary
                        TransactionType.TRANSFER -> MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.semantics {
                        val amountDescription = IndianNumberFormatter.formatForAccessibility(transaction.amountPaise)
                        contentDescription = when (transaction.type) {
                            TransactionType.DEBIT -> "Spent $amountDescription"
                            TransactionType.CREDIT -> "Received $amountDescription"
                            TransactionType.TRANSFER -> "Transferred $amountDescription"
                        }
                    }
                )
            }
        }
    }
}

/**
 * Formats a date as relative time string.
 */
private fun formatRelativeTime(date: LocalDate): String {
    val today = LocalDate.now()
    val daysBetween = ChronoUnit.DAYS.between(date, today)
    return when {
        daysBetween == 0L -> "Today"
        daysBetween == 1L -> "Yesterday"
        daysBetween < 7L -> "${daysBetween}d ago"
        else -> date.format(DateTimeFormatter.ofPattern("EEE dd MMM"))
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
