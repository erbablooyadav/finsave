package com.finsave.data.repository

import com.finsave.data.local.dao.CategoryDao
import com.finsave.data.mapper.toDomain
import com.finsave.data.mapper.toEntity
import com.finsave.domain.model.Category
import com.finsave.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao
) : CategoryRepository {

    override fun getAllCategories(): Flow<List<Category>> =
        categoryDao.getAllCategories().map { entities -> entities.map { it.toDomain() } }

    override fun getCategoryById(id: Long): Flow<Category?> =
        categoryDao.getCategoryById(id).map { it?.toDomain() }

    override fun getDefaultCategories(): Flow<List<Category>> =
        categoryDao.getDefaultCategories().map { entities -> entities.map { it.toDomain() } }

    override suspend fun insertCategory(category: Category): Long =
        categoryDao.insertCategory(category.toEntity())

    override suspend fun updateCategory(category: Category) =
        categoryDao.updateCategory(category.toEntity())

    override suspend fun deleteCategory(id: Long) =
        categoryDao.deleteCategory(id)

    override suspend fun insertDefaultCategories() {
        val defaults = listOf(
            Category(name = "Food & Dining", emoji = "🍔", colorHex = "#F44336", isDefault = true),
            Category(name = "Groceries", emoji = "🛒", colorHex = "#4CAF50", isDefault = true),
            Category(name = "Shopping", emoji = "🛍️", colorHex = "#9C27B0", isDefault = true),
            Category(name = "Transport", emoji = "🚌", colorHex = "#2196F3", isDefault = true),
            Category(name = "Bills & Utilities", emoji = "💡", colorHex = "#FF9800", isDefault = true),
            Category(name = "Entertainment", emoji = "🍿", colorHex = "#E91E63", isDefault = true),
            Category(name = "Health & Fitness", emoji = "⚕️", colorHex = "#00BCD4", isDefault = true),
            Category(name = "Travel", emoji = "✈️", colorHex = "#3F51B5", isDefault = true)
        )
        categoryDao.insertCategories(defaults.map { it.toEntity() })
    }
}
