package com.finsave.feature.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finsave.core.ui.theme.LocalSpacing
import com.finsave.domain.model.TransactionType
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionBottomSheet(
    transaction: com.finsave.domain.model.Transaction? = null,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddTransactionViewModel = hiltViewModel()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }
    
    val amountPaise by viewModel.amountPaise.collectAsState()
    val transactionType by viewModel.transactionType.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    val merchantName by viewModel.merchantName.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val isEditMode by viewModel.isEditMode.collectAsState()

    var amountString by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Initialize with transaction data if in edit mode
    LaunchedEffect(transaction) {
        if (transaction != null) {
            viewModel.initializeWithTransaction(transaction)
            // Convert paise to rupees string for display
            val rupees = transaction.amountPaise / 100.0
            amountString = if (rupees % 1.0 == 0.0) {
                rupees.toInt().toString()
            } else {
                String.format(Locale.ROOT, "%.2f", rupees)
            }
        }
    }

    LaunchedEffect(viewModel.uiEvent) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is AddTransactionViewModel.UiEvent.Success -> onDismissRequest()
                is AddTransactionViewModel.UiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier.fillMaxHeight(0.9f),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        val spacing = LocalSpacing.current

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = spacing.large)
                    .padding(paddingValues)
            ) {
                // Header: Type Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = spacing.medium),
                    horizontalArrangement = Arrangement.Center
                ) {
                    SingleChoiceSegmentedButtonRow {
                        SegmentedButton(
                            selected = transactionType == TransactionType.DEBIT,
                            onClick = { viewModel.onTypeChange(TransactionType.DEBIT) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) {
                            Text("Expense")
                        }
                        SegmentedButton(
                            selected = transactionType == TransactionType.CREDIT,
                            onClick = { viewModel.onTypeChange(TransactionType.CREDIT) },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Text("Income")
                        }
                    }
                }

                // Amount Display
                Text(
                    text = "₹ " + if (amountString.isEmpty()) "0" else amountString,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (transactionType == TransactionType.DEBIT) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(spacing.large))

                // Merchant Input
                OutlinedTextField(
                    value = merchantName,
                    onValueChange = viewModel::onMerchantChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Merchant or Note") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(spacing.medium))

                // Category Chips
                Text("Category", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(spacing.small))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(spacing.small)
                ) {
                    items(categories) { category ->
                        FilterChip(
                            selected = selectedCategoryId == category.id,
                            onClick = { viewModel.onCategorySelect(category.id) },
                            label = { Text(category.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Delete button in edit mode
                if (isEditMode) {
                    OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = spacing.medium),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Delete Transaction", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Custom Numpad
                NumericKeypad(
                    onNumberClick = { num ->
                        amountString += num
                        viewModel.onAmountChange(amountString)
                    },
                    onBackspaceClick = {
                        if (amountString.isNotEmpty()) {
                            amountString = amountString.dropLast(1)
                            viewModel.onAmountChange(amountString)
                        }
                    },
                    onDoneClick = {
                        viewModel.saveTransaction()
                    },
                    doneButtonText = if (isEditMode) "Update" else "Save Transaction"
                )
                
                Spacer(modifier = Modifier.height(spacing.large))
            }
        }

        // Delete confirmation dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Transaction") },
                text = { Text("Are you sure you want to delete this transaction? This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            viewModel.deleteTransaction()
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun NumericKeypad(
    onNumberClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onDoneClick: () -> Unit,
    doneButtonText: String = "Save Transaction"
) {
    val spacing = LocalSpacing.current
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(".", "0", "⌫")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.medium)
    ) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.medium)
            ) {
                row.forEach { key ->
                    KeypadButton(
                        text = key,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (key == "⌫") onBackspaceClick()
                            else onNumberClick(key)
                        }
                    )
                }
            }
        }
        Button(
            onClick = onDoneClick,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(doneButtonText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun KeypadButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .aspectRatio(1.8f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (text == "⌫") {
            Icon(Icons.Default.Close, contentDescription = "Backspace") // Reusing Close as backspace for now
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
