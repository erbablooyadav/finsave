package com.finsave.feature.splitter

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.finsave.core.ui.theme.LocalSpacing
import com.finsave.domain.model.SplitterExpenseSplit
import com.finsave.domain.model.SplitterMember
import com.finsave.domain.model.SplitType

/**
 * Dialog for adding a new expense to a splitter group.
 * 
 * Features:
 * - Description input
 * - Amount input (in rupees, converted to paise)
 * - Payer picker (dropdown of members)
 * - Split type selector (EQUAL, EXACT, PERCENTAGE, SHARES)
 * - Dynamic split input based on split type
 * 
 * Requirements: 7.6, 7.7, 7.8, 7.9
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(
    members: List<SplitterMember>,
    onDismiss: () -> Unit,
    onAddExpense: (String, Long, Long, SplitType, List<SplitterExpenseSplit>) -> Unit
) {
    val spacing = LocalSpacing.current

    var description by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedPayerId by remember { mutableStateOf(members.firstOrNull()?.id ?: 0L) }
    var splitType by remember { mutableStateOf(SplitType.EQUAL) }
    var expandedPayer by remember { mutableStateOf(false) }
    var expandedSplitType by remember { mutableStateOf(false) }

    // Split amounts for EXACT split type
    val splitAmounts = remember { mutableStateMapOf<Long, String>() }

    // Validation errors
    var descriptionError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var splitError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Expense") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(spacing.medium)
            ) {
                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        descriptionError = null
                    },
                    label = { Text("Description") },
                    placeholder = { Text("e.g., Dinner at restaurant") },
                    singleLine = true,
                    isError = descriptionError != null,
                    supportingText = descriptionError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )

                // Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        amountError = null
                    },
                    label = { Text("Amount (₹)") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = amountError != null,
                    supportingText = amountError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )

                // Payer picker
                ExposedDropdownMenuBox(
                    expanded = expandedPayer,
                    onExpandedChange = { expandedPayer = it }
                ) {
                    OutlinedTextField(
                        value = members.find { it.id == selectedPayerId }?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Paid by") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPayer) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedPayer,
                        onDismissRequest = { expandedPayer = false }
                    ) {
                        members.forEach { member ->
                            DropdownMenuItem(
                                text = { Text(member.name) },
                                onClick = {
                                    selectedPayerId = member.id
                                    expandedPayer = false
                                }
                            )
                        }
                    }
                }

                // Split type picker
                ExposedDropdownMenuBox(
                    expanded = expandedSplitType,
                    onExpandedChange = { expandedSplitType = it }
                ) {
                    OutlinedTextField(
                        value = formatSplitType(splitType),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Split type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSplitType) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedSplitType,
                        onDismissRequest = { expandedSplitType = false }
                    ) {
                        SplitType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(formatSplitType(type)) },
                                onClick = {
                                    splitType = type
                                    expandedSplitType = false
                                    splitError = null
                                }
                            )
                        }
                    }
                }

                // Split inputs for EXACT type
                if (splitType == SplitType.EXACT) {
                    Text(
                        text = "Enter amount for each member:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    members.forEach { member ->
                        OutlinedTextField(
                            value = splitAmounts[member.id] ?: "",
                            onValueChange = {
                                splitAmounts[member.id] = it
                                splitError = null
                            },
                            label = { Text(member.name) },
                            placeholder = { Text("0.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Split error
                if (splitError != null) {
                    Text(
                        text = splitError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // Validate inputs
                    var hasError = false

                    if (description.isBlank()) {
                        descriptionError = "Description is required"
                        hasError = true
                    }

                    val amountPaise = try {
                        val rupees = amountText.toDoubleOrNull()
                        if (rupees == null || rupees <= 0) {
                            amountError = "Amount must be greater than zero"
                            hasError = true
                            0L
                        } else {
                            (rupees * 100).toLong()
                        }
                    } catch (e: Exception) {
                        amountError = "Invalid amount"
                        hasError = true
                        0L
                    }

                    if (hasError) return@TextButton

                    // Calculate splits based on split type
                    val splits = when (splitType) {
                        SplitType.EQUAL -> {
                            // Divide equally among all members
                            val perMember = amountPaise / members.size
                            val remainder = amountPaise % members.size
                            members.mapIndexed { index, member ->
                                SplitterExpenseSplit(
                                    expenseId = 0L, // Will be set by use case
                                    memberId = member.id,
                                    amountPaise = perMember + if (index == 0) remainder else 0
                                )
                            }
                        }
                        SplitType.EXACT -> {
                            // Use manually entered amounts
                            val splits = members.mapNotNull { member ->
                                val amountStr = splitAmounts[member.id] ?: ""
                                val amount = amountStr.toDoubleOrNull()
                                if (amount == null) {
                                    splitError = "Invalid amount for ${member.name}"
                                    return@TextButton
                                }
                                SplitterExpenseSplit(
                                    expenseId = 0L,
                                    memberId = member.id,
                                    amountPaise = (amount * 100).toLong()
                                )
                            }
                            val splitSum = splits.sumOf { it.amountPaise }
                            if (splitSum != amountPaise) {
                                splitError = "Split amounts must sum to ${amountText}"
                                return@TextButton
                            }
                            splits
                        }
                        SplitType.PERCENTAGE -> {
                            // For now, default to equal split
                            // TODO: Implement percentage input UI
                            val perMember = amountPaise / members.size
                            val remainder = amountPaise % members.size
                            members.mapIndexed { index, member ->
                                SplitterExpenseSplit(
                                    expenseId = 0L,
                                    memberId = member.id,
                                    amountPaise = perMember + if (index == 0) remainder else 0
                                )
                            }
                        }
                        SplitType.SHARES -> {
                            // For now, default to equal split
                            // TODO: Implement shares input UI
                            val perMember = amountPaise / members.size
                            val remainder = amountPaise % members.size
                            members.mapIndexed { index, member ->
                                SplitterExpenseSplit(
                                    expenseId = 0L,
                                    memberId = member.id,
                                    amountPaise = perMember + if (index == 0) remainder else 0
                                )
                            }
                        }
                    }

                    onAddExpense(description, amountPaise, selectedPayerId, splitType, splits)
                }
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
 * Formats split type for display.
 */
private fun formatSplitType(type: SplitType): String {
    return when (type) {
        SplitType.EQUAL -> "Split Equally"
        SplitType.EXACT -> "Exact Amounts"
        SplitType.PERCENTAGE -> "By Percentage"
        SplitType.SHARES -> "By Shares"
    }
}
