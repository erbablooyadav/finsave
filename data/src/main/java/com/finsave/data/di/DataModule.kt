package com.finsave.data.di

import android.content.Context
import androidx.room.Room
import com.finsave.data.local.FinSaveDatabase
import com.finsave.data.local.dao.AccountDao
import com.finsave.data.local.dao.BudgetDao
import com.finsave.data.local.dao.CategoryDao
import com.finsave.data.local.dao.SplitterExpenseDao
import com.finsave.data.local.dao.SplitterGroupDao
import com.finsave.data.local.dao.SplitterMemberDao
import com.finsave.data.local.dao.SplitterSplitDao
import com.finsave.data.local.dao.TransactionDao
import com.finsave.data.repository.TransactionRepositoryImpl
import com.finsave.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt DI Module — Database
 *
 * Provides the Room database instance and all DAO instances.
 * SQLCipher encryption will be added in Phase 1D by swapping the
 * SupportSQLiteOpenHelper.Factory here.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FinSaveDatabase {
        return Room.databaseBuilder(
            context,
            FinSaveDatabase::class.java,
            FinSaveDatabase.DATABASE_NAME
        )
        .fallbackToDestructiveMigration() // Dev only — replace with proper migrations before launch
        .build()
    }

    @Provides
    fun provideTransactionDao(database: FinSaveDatabase): TransactionDao =
        database.transactionDao()

    @Provides
    fun provideCategoryDao(database: FinSaveDatabase): CategoryDao =
        database.categoryDao()

    @Provides
    fun provideAccountDao(database: FinSaveDatabase): AccountDao =
        database.accountDao()

    @Provides
    fun provideBudgetDao(database: FinSaveDatabase): BudgetDao =
        database.budgetDao()

    @Provides
    fun provideSplitterGroupDao(database: FinSaveDatabase): SplitterGroupDao =
        database.splitterGroupDao()

    @Provides
    fun provideSplitterMemberDao(database: FinSaveDatabase): SplitterMemberDao =
        database.splitterMemberDao()

    @Provides
    fun provideSplitterExpenseDao(database: FinSaveDatabase): SplitterExpenseDao =
        database.splitterExpenseDao()

    @Provides
    fun provideSplitterSplitDao(database: FinSaveDatabase): SplitterSplitDao =
        database.splitterSplitDao()
}

/**
 * Hilt DI Module — Repository Bindings
 *
 * Binds repository interfaces to their implementations.
 * This is where domain ↔ data layer contract is enforced.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        impl: TransactionRepositoryImpl
    ): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(
        impl: com.finsave.data.repository.CategoryRepositoryImpl
    ): com.finsave.domain.repository.CategoryRepository

    @Binds
    @Singleton
    abstract fun bindAccountRepository(
        impl: com.finsave.data.repository.AccountRepositoryImpl
    ): com.finsave.domain.repository.AccountRepository

    @Binds
    @Singleton
    abstract fun bindBudgetRepository(
        impl: com.finsave.data.repository.BudgetRepositoryImpl
    ): com.finsave.domain.repository.BudgetRepository

    @Binds
    @Singleton
    abstract fun bindSplitterRepository(
        impl: com.finsave.data.repository.SplitterRepositoryImpl
    ): com.finsave.domain.repository.SplitterRepository
}
