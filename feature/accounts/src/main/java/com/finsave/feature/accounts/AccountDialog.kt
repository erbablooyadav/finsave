package com.finsave.feature.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.finsave.core.ui.theme.LocalSpacing
import com.finsave.domain.model.Account
import com.finsave.domain.model.AccountType

/**
 * Dialog for adding or editing an account.
 * 
 * Fields: name, type, opening balance, color
 * Validation: name non-empty, opening balance ≥ 0
 * 
 * Requirements: 4.3, 4.4, 4.5, 4.6
 */
@Composable
fun AccountDialog(
    account: Account?,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (id: Long, name: String, type: AccountType, balancePaise: Long, colorHex: String, isDefault: Boolean) -> Unit,
    onDelete: ((Account) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (account == null) return

    val spacing = LocalSpacing.current
    val isEditMode = account.id != 0L

    var name by remember(account) { mutableStateOf(account.name) }
    var selectedType by remember(account) { mutableStateOf(account.type) }
    var balanceInput by remember(account) {
        mutableStateOf(
            if (account.balancePaise == 0L) "" else (account.balancePaise / 100.0).toString()
        )
    }
    var selectedColor by remember(account) { mutableStateOf(account.colorHex) }
    var isDefault by remember(account) { mutableStateOf(account.isDefault) }

    // Local validation errors
    var nameError by remember { mutableStateOf<String?>(null) }
    var balanceError by remember { mutableStateOf<String?>(null) }

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
                    text = if (isEditMode) "Edit Account" else "Add Account",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Name field
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = null
                    },
                    label = { Text("Account Name") },
                    isError = nameError != null || (errorMessage?.contains("name", ignoreCase = true) == true),
                    supportingText = {
                        if (nameError != null) {
                            Text(nameError!!, color = MaterialTheme.colorScheme.error)
                        } else if (errorMessage?.contains("name", ignoreCase = true) == true) {
                            Text(errorMessage, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Account type selector
                Text(
                    text = "Account Type",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                AccountTypeSelector(
                    selectedType = selectedType,
                    onTypeSelected = { selectedType = it }
                )

                // Opening balance field
                OutlinedTextField(
                    value = balanceInput,
                    onValueChange = {
                        balanceInput = it
                        balanceError = null
                    },
                    label = { Text(if (isEditMode) "Current Balance" else "Opening Balance") },
                    prefix = { Text("₹") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = balanceError != null || (errorMessage?.contains("balance", ignoreCase = true) == true),
                    supportingText = {
                        if (balanceError != null) {
                            Text(balanceError!!, color = MaterialTheme.colorScheme.error)
                        } else if (errorMessage?.contains("balance", ignoreCase = true) == true) {
                            Text(errorMessage, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Color picker
                Text(
                    text = "Color",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ColorPicker(
                    selectedColor = selectedColor,
                    onColorSelected = { selectedColor = it }
                )

                // Set as default toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Set as Default",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(
                        checked = isDefault,
                        onCheckedChange = { isDefault = it }
                    )
                }

                // Error message (general)
                if (errorMessage != null && 
                    !errorMessage.contains("name", ignoreCase = true) && 
                    !errorMessage.contains("balance", ignoreCase = true)) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.small)
                ) {
                    // Delete button (edit mode only)
                    if (isEditMode && onDelete != null) {
                        OutlinedButton(
                            onClick = { onDelete(account) },
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
                            // Validate name
                            if (name.trim().isEmpty()) {
                                nameError = "Account name is required"
                                return@Button
                            }

                            // Validate balance
                            val balancePaise = try {
                                val rupees = balanceInput.trim().toDoubleOrNull() ?: 0.0
                                if (rupees < 0) {
                                    balanceError = "Balance must be non-negative"
                                    return@Button
                                }
                                (rupees * 100).toLong()
                            } catch (e: Exception) {
                                balanceError = "Invalid balance"
                                return@Button
                            }

                            onSave(account.id, name, selectedType, balancePaise, selectedColor, isDefault)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

/**
 * Account type selector with chips.
 */
@Composable
private fun AccountTypeSelector(
    selectedType: AccountType,
    onTypeSelected: (AccountType) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.small)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.small)
        ) {
            AccountTypeChip(
                type = AccountType.SAVINGS,
                isSelected = selectedType == AccountType.SAVINGS,
                onClick = { onTypeSelected(AccountType.SAVINGS) },
                modifier = Modifier.weight(1f)
            )
            AccountTypeChip(
                type = AccountType.CURRENT,
                isSelected = selectedType == AccountType.CURRENT,
                onClick = { onTypeSelected(AccountType.CURRENT) },
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.small)
        ) {
            AccountTypeChip(
                type = AccountType.CREDIT_CARD,
                isSelected = selectedType == AccountType.CREDIT_CARD,
                onClick = { onTypeSelected(AccountType.CREDIT_CARD) },
                modifier = Modifier.weight(1f)
            )
            AccountTypeChip(
                type = AccountType.WALLET,
                isSelected = selectedType == AccountType.WALLET,
                onClick = { onTypeSelected(AccountType.WALLET) },
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.small)
        ) {
            AccountTypeChip(
                type = AccountType.CASH,
                isSelected = selectedType == AccountType.CASH,
                onClick = { onTypeSelected(AccountType.CASH) },
                modifier = Modifier.weight(1f)
            )
            AccountTypeChip(
                type = AccountType.INVESTMENT,
                isSelected = selectedType == AccountType.INVESTMENT,
                onClick = { onTypeSelected(AccountType.INVESTMENT) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Individual account type chip.
 */
@Composable
private fun AccountTypeChip(
    type: AccountType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = type.name.replace("_", " "),
                style = MaterialTheme.typography.labelMedium
            )
        },
        modifier = modifier
    )
}

/**
 * Color picker with preset colors.
 */
@Composable
private fun ColorPicker(
    selectedColor: String,
    onColorSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    val colors = listOf(
        "#4F46E5", // Indigo
        "#7C3AED", // Purple
        "#DB2777", // Pink
        "#DC2626", // Red
        "#EA580C", // Orange
        "#CA8A04", // Yellow
        "#16A34A", // Green
        "#0891B2", // Cyan
        "#0284C7", // Blue
        "#6366F1", // Light Indigo
        "#8B5CF6", // Light Purple
        "#64748B"  // Slate
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
        verticalArrangement = Arrangement.spacedBy(spacing.small),
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        items(colors) { colorHex ->
            ColorCircle(
                colorHex = colorHex,
                isSelected = selectedColor.equals(colorHex, ignoreCase = true),
                onClick = { onColorSelected(colorHex) }
            )
        }
    }
}

/**
 * Individual color circle.
 */
@Composable
private fun ColorCircle(
    colorHex: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
