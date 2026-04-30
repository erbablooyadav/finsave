package com.finsave.feature.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finsave.domain.model.Category
import com.finsave.domain.usecase.category.AddCategoryUseCase
import com.finsave.domain.usecase.category.DeleteCategoryUseCase
import com.finsave.domain.usecase.category.GetCategoriesUseCase
import com.finsave.domain.usecase.category.ReorderCategoriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for CategoryScreen.
 * Manages category list, add/delete operations, and drag-to-reorder.
 *
 * Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8
 */
@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val getCategories: GetCategoriesUseCase,
    private val addCategory: AddCategoryUseCase,
    private val deleteCategory: DeleteCategoryUseCase,
    private val reorderCategories: ReorderCategoriesUseCase
) : ViewModel() {

    // Category list from repository (default + custom, sorted by sortOrder)
    val categories: StateFlow<List<Category>> = getCategories()
        .map { list -> list.sortedBy { it.sortOrder } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // UI state
    private val _uiState = MutableStateFlow(CategoryUiState())
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    // Dialog state
    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog.asStateFlow()

    // Delete confirmation state
    private val _deleteConfirmation = MutableStateFlow<Category?>(null)
    val deleteConfirmation: StateFlow<Category?> = _deleteConfirmation.asStateFlow()

    /**
     * Opens the add category dialog.
     * Requirement: 5.2
     */
    fun onAddCategoryClick() {
        _showAddDialog.value = true
    }

    /**
     * Closes the add category dialog.
     */
    fun onDismissDialog() {
        _showAddDialog.value = false
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Saves a new category.
     * Validates: name non-empty, total count < 50.
     * Requirements: 5.3, 5.4, 5.5
     */
    fun onSaveCategory(
        name: String,
        emoji: String,
        colorHex: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val category = Category(
                name = name.trim(),
                emoji = emoji,
                colorHex = colorHex,
                isDefault = false,
                isCustom = true,
                sortOrder = categories.value.size // Add to end
            )

            val result = addCategory(category)

            result.fold(
                onSuccess = {
                    _showAddDialog.value = false
                    _uiState.update { it.copy(isLoading = false) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to add category"
                        )
                    }
                }
            )
        }
    }

    /**
     * Shows delete confirmation dialog for a custom category.
     * Requirement: 5.6
     */
    fun onDeleteCategoryClick(category: Category) {
        if (category.isDefault) {
            _uiState.update {
                it.copy(errorMessage = "Default categories cannot be deleted")
            }
            return
        }
        _deleteConfirmation.value = category
    }

    /**
     * Dismisses delete confirmation dialog.
     */
    fun onDismissDeleteConfirmation() {
        _deleteConfirmation.value = null
    }

    /**
     * Confirms category deletion.
     * Validates: isDefault == false.
     * Requirements: 5.6, 5.7
     */
    fun onConfirmDelete() {
        val category = _deleteConfirmation.value ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            deleteCategory(category.id).fold(
                onSuccess = {
                    _deleteConfirmation.value = null
                    _uiState.update { it.copy(isLoading = false) }
                },
                onFailure = { error ->
                    _deleteConfirmation.value = null
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to delete category"
                        )
                    }
                }
            )
        }
    }

    /**
     * Reorders categories after drag-and-drop.
     * Persists new sortOrder values for all affected categories.
     * Requirement: 5.8
     */
    fun onReorderCategories(reorderedList: List<Category>) {
        viewModelScope.launch {
            // Update sortOrder for all categories based on new positions
            val updatedCategories = reorderedList.mapIndexed { index, category ->
                category.copy(sortOrder = index)
            }

            reorderCategories(updatedCategories).fold(
                onSuccess = {
                    // Success - the Flow will automatically update
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(errorMessage = error.message ?: "Failed to reorder categories")
                    }
                }
            )
        }
    }

    /**
     * Clears error message.
     */
    fun onClearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

/**
 * UI state for CategoryScreen.
 */
data class CategoryUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
