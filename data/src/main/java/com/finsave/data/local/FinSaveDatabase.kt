package com.finsave.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.finsave.data.local.dao.AccountDao
import com.finsave.data.local.dao.BudgetDao
import com.finsave.data.local.dao.CategoryDao
import com.finsave.data.local.dao.SplitterExpenseDao
import com.finsave.data.local.dao.SplitterGroupDao
import com.finsave.data.local.dao.SplitterMemberDao
import com.finsave.data.local.dao.SplitterSplitDao
import com.finsave.data.local.dao.TransactionDao
import com.finsave.data.local.entity.AccountEntity
import com.finsave.data.local.entity.BudgetEntity
import com.finsave.data.local.entity.CategoryEntity
import com.finsave.data.local.entity.SplitterExpenseEntity
import com.finsave.data.local.entity.SplitterExpenseSplitEntity
import com.finsave.data.local.entity.SplitterGroupEntity
import com.finsave.data.local.entity.SplitterMemberEntity
import com.finsave.data.local.entity.TransactionEntity

/**
 * FinSave Room Database
 *
 * Version 1 baseline — all 8 entity tables.
 * SQLCipher encryption will be added in Phase 1D (Security Polish).
 *
 * Current setup:
 * - Standard Room database without encryption
 * - Schema exported to /schemas for migration testing
 * - All entities indexed for query performance
 *
 * Phase 1D upgrade path:
 * - Replace SupportSQLiteOpenHelper.Factory with SQLCipher's
 * - Generate AES-256 key from Android Keystore
 * - Transparent to all DAO queries — zero code changes above DB layer
 */
@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        AccountEntity::class,
        BudgetEntity::class,
        SplitterGroupEntity::class,
        SplitterMemberEntity::class,
        SplitterExpenseEntity::class,
        SplitterExpenseSplitEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class FinSaveDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun accountDao(): AccountDao
    abstract fun budgetDao(): BudgetDao
    abstract fun splitterGroupDao(): SplitterGroupDao
    abstract fun splitterMemberDao(): SplitterMemberDao
    abstract fun splitterExpenseDao(): SplitterExpenseDao
    abstract fun splitterSplitDao(): SplitterSplitDao

    companion object {
        const val DATABASE_NAME = "finsave_database"
    }
}
