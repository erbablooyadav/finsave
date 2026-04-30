package com.finsave.feature.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.finsave.core.ui.theme.LocalSpacing

/**
 * Dialog for adding a new category.
 * 
 * Fields: name (text), emoji (single emoji picker), color (color picker with ≥ 12 presets)
 * Validation: name non-empty, valid emoji, selected color
 * 
 * Requirements: 5.2, 5.3, 5.4, 5.5
 */
@Composable
fun AddCategoryDialog(
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (name: String, emoji: String, colorHex: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    var name by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#4F46E5") }

    // Local validation errors
    var nameError by remember { mutableStateOf<String?>(null) }
    var emojiError by remember { mutableStateOf<String?>(null) }

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
                    text = "Add Category",
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
                    label = { Text("Category Name") },
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

                // Emoji picker
                Text(
                    text = "Emoji",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                EmojiPicker(
                    selectedEmoji = selectedEmoji,
                    onEmojiSelected = {
                        selectedEmoji = it
                        emojiError = null
                    },
                    isError = emojiError != null
                )

                if (emojiError != null) {
                    Text(
                        text = emojiError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

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

                // Error message (general)
                if (errorMessage != null && !errorMessage.contains("name", ignoreCase = true)) {
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
                                nameError = "Category name is required"
                                return@Button
                            }

                            // Validate emoji
                            if (selectedEmoji.isEmpty()) {
                                emojiError = "Please select an emoji"
                                return@Button
                            }

                            onSave(name, selectedEmoji, selectedColor)
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
 * Emoji picker with common emojis.
 * Single emoji selection.
 */
@Composable
private fun EmojiPicker(
    selectedEmoji: String,
    onEmojiSelected: (String) -> Unit,
    isError: Boolean = false,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    // Common category emojis
    val emojis = listOf(
        "🍔", "🍕", "☕", "🛒", "🚗", "⛽",
        "🏠", "💡", "📱", "👕", "🎬", "🎮",
        "✈️", "🏥", "💊", "📚", "🎓", "💰",
        "🎁", "🐕", "🌳", "🔧", "💼", "🎨",
        "🏋️", "🧘", "🍷", "🎵", "📦", "🚌"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isError) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
            verticalArrangement = Arrangement.spacedBy(spacing.small),
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(spacing.medium)
        ) {
            items(emojis) { emoji ->
                EmojiCircle(
                    emoji = emoji,
                    isSelected = selectedEmoji == emoji,
                    onClick = { onEmojiSelected(emoji) }
                )
            }
        }
    }
}

/**
 * Individual emoji circle.
 */
@Composable
private fun EmojiCircle(
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Color picker with preset colors (≥ 12 presets).
 * Requirement: 5.2
 */
@Composable
private fun ColorPicker(
    selectedColor: String,
    onColorSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    // 15 preset colors
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
        "#EC4899", // Light Pink
        "#F59E0B", // Amber
        "#10B981", // Emerald
        "#64748B"  // Slate
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
        verticalArrangement = Arrangement.spacedBy(spacing.small),
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
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
