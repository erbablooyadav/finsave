package com.finsave.feature.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finsave.core.ui.theme.LocalSpacing
import com.finsave.domain.model.Budget
import com.finsave.domain.model.BudgetPeriod
import com.finsave.domain.model.Category
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Dialog for creating or editing a budget.
 * 
 * Fields:
 * - Scope: category picker or "Total" (null categoryId)
 * - Limit amount: positive number in rupees
 * - Period: MONTHLY, WEEKLY, or CUSTOM
 * - Date pickers: shown when CUSTOM period is selected
 * 
 * Validation:
 * - Limit amount must be > 0 (Requirement 6.5)
 * - No duplicate category+period (Requirement 6.6)
 * 
 * Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7, 6.8, 6.9
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetDialog(
    budget: Budget? = null,
    onDismiss: () -> Unit,
    viewModel: BudgetViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    if (budget == null) return

    val spacing = LocalSpacing.current
    val isEditMode = budget.id != 0L

    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    // Form state
    var selectedCategoryId by remember(budget) { mutableStateOf(budget.categoryId) }
    var limitInput by remember(budget) {
        mutableStateOf(
            if (budget.limitAmountPaise == 0L) "" else (budget.limitAmountPaise / 100.0).toString()
        )
    }
    var selectedPeriod by remember(budget) { mutableStateOf(budget.periodType) }
    var startDate by remember(budget) { mutableStateOf(budget.startDate) }
    var endDate by remember(budget) { mutableStateOf(budget.endDate ?: budget.startDate) }

    // UI state
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showPeriodDropdown by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    // Local validation errors
    var limitError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(spacing.medium),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(spacing.large),
                verticalArrangement = Arrangement.spacedBy(spacing.medium)
            ) {
                // Title
                Text(
                    text = if (isEditMode) "Edit Budget" else "Create Budget",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Scope selector (Category or Total)
                Text(
                    text = "Budget Scope",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ExposedDropdownMenuBox(
                    expanded = showCategoryDropdown,
                    onExpandedChange = { showCategoryDropdown = it }
                ) {
                    OutlinedTextField(
                        value = if (selectedCategoryId == null) {
                            "Total Budget"
                        } else {
                            categories.find { it.id == selectedCategoryId }?.name ?: "Select Category"
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Scope") },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select scope"
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        singleLine = true
                    )

                    ExposedDropdownMenu(
                        expanded = showCategoryDropdown,
                        onDismissRequest = { showCategoryDropdown = false }
                    ) {
                        // Total budget option
                        DropdownMenuItem(
                            text = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(spacing.small),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("💰", style = MaterialTheme.typography.titleMedium)
                                    Text("Total Budget")
                                }
                            },
                            onClick = {
                                selectedCategoryId = null
                                showCategoryDropdown = false
                            }
                        )

                        HorizontalDivider()

                        // Category options
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(spacing.small),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.secondaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(category.emoji, style = MaterialTheme.typography.titleMedium)
                                        }
                                        Text(category.name)
                                    }
                                },
                                onClick = {
                                    selectedCategoryId = category.id
                                    showCategoryDropdown = false
                                }
                            )
                        }
                    }
                }

                // Limit amount field
                OutlinedTextField(
                    value = limitInput,
                    onValueChange = {
                        limitInput = it
                        limitError = null
                    },
                    label = { Text("Budget Limit") },
                    prefix = { Text("₹") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = limitError != null || (errorMessage?.contains("amount", ignoreCase = true) == true),
                    supportingText = {
                        val error = errorMessage
                        if (limitError != null) {
                            Text(limitError!!, color = MaterialTheme.colorScheme.error)
                        } else if (error != null && error.contains("amount", ignoreCase = true)) {
                            Text(error, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Period selector
                Text(
                    text = "Budget Period",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ExposedDropdownMenuBox(
                    expanded = showPeriodDropdown,
                    onExpandedChange = { showPeriodDropdown = it }
                ) {
                    OutlinedTextField(
                        value = formatPeriod(selectedPeriod),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Period") },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select period"
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        singleLine = true
                    )

                    ExposedDropdownMenu(
                        expanded = showPeriodDropdown,
                        onDismissRequest = { showPeriodDropdown = false }
                    ) {
                        BudgetPeriod.entries.forEach { period ->
                            DropdownMenuItem(
                                text = { Text(formatPeriod(period)) },
                                onClick = {
                                    selectedPeriod = period
                                    showPeriodDropdown = false
                                }
                            )
                        }
                    }
                }

                // Date pickers for CUSTOM period
                if (selectedPeriod == BudgetPeriod.CUSTOM) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.small)
                    ) {
                        // Start date
                        OutlinedTextField(
                            value = formatDate(startDate),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Start Date") },
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showStartDatePicker = true },
                            singleLine = true
                        )

                        // End date
                        OutlinedTextField(
                            value = formatDate(endDate),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("End Date") },
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showEndDatePicker = true },
                            singleLine = true
                        )
                    }
                }

                // General error message
                errorMessage?.let { error ->
                    if (!error.contains("amount", ignoreCase = true)) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.small)
                ) {
                    // Delete button (edit mode only)
                    if (isEditMode) {
                        OutlinedButton(
                            onClick = {
                                viewModel.onDeleteBudget(budget)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Delete")
                        }
                    }

                    // Cancel button
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    // Save button
                    Button(
                        onClick = {
                            // Validate limit amount
                            val limitPaise = try {
                                val rupees = limitInput.trim().toDoubleOrNull() ?: 0.0
                                if (rupees <= 0) {
                                    limitError = "Budget amount must be greater than zero"
                                    return@Button
                                }
                                (rupees * 100).toLong()
                            } catch (e: Exception) {
                                limitError = "Invalid amount"
                                return@Button
                            }

                            // Create budget object
                            val updatedBudget = budget.copy(
                                categoryId = selectedCategoryId,
                                limitAmountPaise = limitPaise,
                                periodType = selectedPeriod,
                                startDate = startDate,
                                endDate = if (selectedPeriod == BudgetPeriod.CUSTOM) endDate else null
                            )

                            // Save via ViewModel
                            if (isEditMode) {
                                viewModel.onUpdateBudget(updatedBudget)
                            } else {
                                viewModel.onAddBudget(updatedBudget)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }

    // Date pickers
    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = startDate.toEpochDay() * 24 * 60 * 60 * 1000
            )
            DatePicker(state = datePickerState)

            LaunchedEffect(datePickerState.selectedDateMillis) {
                datePickerState.selectedDateMillis?.let { millis ->
                    startDate = LocalDate.ofInstant(Instant.ofEpochMilli(millis), ZoneId.of("Asia/Kolkata"))
                }
            }
        }
    }

    if (showEndDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = endDate.toEpochDay() * 24 * 60 * 60 * 1000
            )
            DatePicker(state = datePickerState)

            LaunchedEffect(datePickerState.selectedDateMillis) {
                datePickerState.selectedDateMillis?.let { millis ->
                    endDate = LocalDate.ofInstant(Instant.ofEpochMilli(millis), ZoneId.of("Asia/Kolkata"))
                }
            }
        }
    }
}

/**
 * Formats budget period for display.
 */
private fun formatPeriod(period: BudgetPeriod): String {
    return when (period) {
        BudgetPeriod.MONTHLY -> "Monthly"
        BudgetPeriod.WEEKLY -> "Weekly"
        BudgetPeriod.CUSTOM -> "Custom"
    }
}

/**
 * Formats date for display.
 */
private fun formatDate(date: LocalDate): String {
    return date.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
}
