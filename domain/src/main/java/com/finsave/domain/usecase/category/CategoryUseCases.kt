package com.finsave.domain.usecase.category

import com.finsave.domain.model.Category
import com.finsave.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Use case: Get all categories.
 */
class GetCategoriesUseCase @Inject constructor(
    private val repository: CategoryRepository
) {
    operator fun invoke(): Flow<List<Category>> = repository.getAllCategories()
}

/**
 * Use case: Add a new category.
 * Validates: name non-empty, total count < 50.
 * Requirements: 5.4, 5.5
 */
class AddCategoryUseCase @Inject constructor(
    private val repository: CategoryRepository
) {
    suspend operator fun invoke(category: Category): Result<Long> {
        // Validation: name non-empty
        if (category.name.isBlank()) {
            return Result.failure(IllegalArgumentException("Category name is required"))
        }

        // Validation: total count < MAX_CUSTOM_CATEGORIES (50)
        val currentCount = repository.getAllCategories().first().size
        if (currentCount >= 50) {
            return Result.failure(IllegalStateException("Maximum category limit reached"))
        }

        return try {
            val id = repository.insertCategory(category)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Use case: Delete a category.
 * Validates: isDefault == false (cannot delete default categories).
 * Requirements: 5.7
 */
class DeleteCategoryUseCase @Inject constructor(
    private val repository: CategoryRepository
) {
    suspend operator fun invoke(id: Long): Result<Unit> {
        // Check: category is not default
        val category = repository.getCategoryById(id).first()
        if (category == null) {
            return Result.failure(IllegalArgumentException("Category not found"))
        }

        if (category.isDefault) {
            return Result.failure(IllegalStateException("Default categories cannot be deleted"))
        }

        return try {
            repository.deleteCategory(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Use case: Reorder categories.
 * Bulk-updates sortOrder for all affected categories.
 * Requirements: 5.8
 */
class ReorderCategoriesUseCase @Inject constructor(
    private val repository: CategoryRepository
) {
    suspend operator fun invoke(reorderedCategories: List<Category>): Result<Unit> {
        return try {
            reorderedCategories.forEachIndexed { index, category ->
                repository.updateCategory(category.copy(sortOrder = index))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
