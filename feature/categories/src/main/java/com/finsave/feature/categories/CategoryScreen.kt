package com.finsave.feature.categories

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finsave.core.ui.components.FinSaveCard
import com.finsave.core.ui.theme.LocalSpacing
import com.finsave.domain.model.Category
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * Category Management Screen.
 * 
 * Displays:
 * - Default categories (18) followed by custom categories
 * - Add category FAB
 * - Drag-to-reorder functionality
 * - Long-press to delete custom categories
 * 
 * Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CategoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: CategoryViewModel = hiltViewModel()
) {
    val spacing = LocalSpacing.current

    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showAddDialog by viewModel.showAddDialog.collectAsStateWithLifecycle()
    val deleteConfirmation by viewModel.deleteConfirmation.collectAsStateWithLifecycle()

    // Mutable list for reordering
    var reorderableCategories by remember(categories) { mutableStateOf(categories) }

    // Show error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onClearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categories") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onAddCategoryClick() }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Category")
            }
        },
        snackbarHost = { com.finsave.core.ui.components.FinSaveSnackbar(snackbarHostState) }
    ) { paddingValues ->
        if (categories.isEmpty()) {
            EmptyCategoriesState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            CategoryList(
                categories = reorderableCategories,
                onReorder = { fromIndex, toIndex ->
                    reorderableCategories = reorderableCategories.toMutableList().apply {
                        add(toIndex, removeAt(fromIndex))
                    }
                },
                onReorderFinished = {
                    viewModel.onReorderCategories(reorderableCategories)
                },
                onLongPress = { category ->
                    viewModel.onDeleteCategoryClick(category)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }
    }

    // Add category dialog
    if (showAddDialog) {
        AddCategoryDialog(
            errorMessage = uiState.errorMessage,
            onDismiss = { viewModel.onDismissDialog() },
            onSave = { name, emoji, color ->
                viewModel.onSaveCategory(name, emoji, color)
            }
        )
    }

    // Delete confirmation dialog
    if (deleteConfirmation != null) {
        DeleteConfirmationDialog(
            category = deleteConfirmation!!,
            onDismiss = { viewModel.onDismissDeleteConfirmation() },
            onConfirm = { viewModel.onConfirmDelete() }
        )
    }
}

/**
 * Category list with drag-to-reorder functionality.
 * Requirement: 5.8
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryList(
    categories: List<Category>,
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit,
    onReorderFinished: () -> Unit,
    onLongPress: (Category) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val haptic = LocalHapticFeedback.current

    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        onReorder(from.index, to.index)
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        // Call onReorderFinished after each move
        onReorderFinished()
    }

    LazyColumn(
        state = lazyListState,
        modifier = modifier.padding(horizontal = spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
        contentPadding = PaddingValues(vertical = spacing.medium)
    ) {
        // Section header: Default Categories
        item {
            Text(
                text = "Default Categories",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = spacing.small)
            )
        }

        // Default categories
        val defaultCategories = categories.filter { it.isDefault }
        items(defaultCategories, key = { it.id }) { category ->
            ReorderableItem(reorderableLazyListState, key = category.id) { isDragging ->
                CategoryCard(
                    category = category,
                    isDragging = isDragging,
                    onLongPress = { onLongPress(category) },
                    modifier = Modifier.animateItem()
                )
            }
        }

        // Section header: Custom Categories (if any)
        val customCategories = categories.filter { it.isCustom }
        if (customCategories.isNotEmpty()) {
            item {
                Text(
                    text = "Custom Categories",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = spacing.medium, bottom = spacing.small)
                )
            }

            items(customCategories, key = { it.id }) { category ->
                ReorderableItem(reorderableLazyListState, key = category.id) { isDragging ->
                    CategoryCard(
                        category = category,
                        isDragging = isDragging,
                        onLongPress = { onLongPress(category) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

/**
 * Individual category card.
 * Shows: emoji, name, color swatch, drag handle.
 * Long-press on custom categories shows delete option.
 * Requirements: 5.1, 5.6
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryCard(
    category: Category,
    isDragging: Boolean,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val haptic = LocalHapticFeedback.current

    val categoryColor = try {
        Color(android.graphics.Color.parseColor(category.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { /* No action on click */ },
                onLongClick = {
                    if (category.isCustom) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongPress()
                    }
                }
            ),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDragging) 8.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.medium),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Emoji
                Text(
                    text = category.emoji,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.size(48.dp)
                )

                // Category name
                Column(
                    verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)
                ) {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (category.isDefault) {
                        Text(
                            text = "Default",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Color swatch
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(categoryColor)
                )

                // Drag handle
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Drag to reorder",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Empty state when no categories exist.
 */
@Composable
private fun EmptyCategoriesState(
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    Column(
        modifier = modifier.padding(spacing.extraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🏷️",
            style = MaterialTheme.typography.displayLarge
        )
        Spacer(modifier = Modifier.height(spacing.medium))
        Text(
            text = "No categories yet",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(spacing.small))
        Text(
            text = "Add your first category to organize your transactions",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Delete confirmation dialog.
 * Requirements: 5.6, 5.7
 */
@Composable
private fun DeleteConfirmationDialog(
    category: Category,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Category?") },
        text = {
            Text("Are you sure you want to delete \"${category.name}\"? This action cannot be undone.")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
