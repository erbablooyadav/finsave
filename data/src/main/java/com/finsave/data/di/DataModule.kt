package com.finsave.data.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.finsave.core.common.Constants
import com.finsave.core.common.prefs.PreferencesManager
import com.finsave.data.local.FinSaveDatabase
import com.finsave.data.local.dao.AccountDao
import com.finsave.data.local.dao.BudgetDao
import com.finsave.data.local.dao.CategoryDao
import com.finsave.data.local.dao.SplitterExpenseDao
import com.finsave.data.local.dao.SplitterGroupDao
import com.finsave.data.local.dao.SplitterMemberDao
import com.finsave.data.local.dao.SplitterSplitDao
import com.finsave.data.local.dao.TransactionDao
import com.finsave.data.repository.DataResetRepositoryImpl
import com.finsave.data.repository.MerchantCatalogRepositoryImpl
import com.finsave.data.repository.TransactionRepositoryImpl
import com.finsave.domain.repository.DataResetRepository
import com.finsave.domain.repository.MerchantCatalogRepository
import com.finsave.data.security.AndroidKeyStoreHelper
import com.finsave.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.Arrays
import net.zetetic.database.sqlcipher.SQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
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

    private val MIGRATION_1_1 = object : Migration(1, 1) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Baseline migration — no schema changes from v1 to v1.
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        preferencesManager: PreferencesManager
    ): FinSaveDatabase {
        System.loadLibrary("sqlcipher")

        if (!preferencesManager.getBoolean(Constants.PREFS_DB_ENCRYPTED_V1, false)) {
            // One-shot wipe of any pre-existing unencrypted DB on this device.
            // SQLCipher cannot open plaintext SQLite files.
            val dbFile = context.getDatabasePath(FinSaveDatabase.DATABASE_NAME)
            SQLiteDatabase.deleteDatabase(dbFile)
            context.deleteDatabase(FinSaveDatabase.DATABASE_NAME)
            dbFile.parentFile?.let { parent ->
                kotlin.runCatching { java.io.File(parent, "${FinSaveDatabase.DATABASE_NAME}-wal").delete() }
                kotlin.runCatching { java.io.File(parent, "${FinSaveDatabase.DATABASE_NAME}-shm").delete() }
            }
            preferencesManager.setBoolean(Constants.PREFS_DB_ENCRYPTED_V1, true)
        }
        val passphrase = AndroidKeyStoreHelper.getOrCreatePassphrase(context)
        val factory = SupportOpenHelperFactory(passphrase)

        // Destructive migration fallback is intentionally omitted; every schema change must add a proper Migration.
        val db = Room.databaseBuilder(
            context,
            FinSaveDatabase::class.java,
            FinSaveDatabase.DATABASE_NAME
        )
            .openHelperFactory(factory)
            .addMigrations(MIGRATION_1_1)
            .build()

        Arrays.fill(passphrase, 0)
        return db
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

    @Binds
    @Singleton
    abstract fun bindDataResetRepository(
        impl: DataResetRepositoryImpl
    ): DataResetRepository

    @Binds
    @Singleton
    abstract fun bindMerchantCatalogRepository(
        impl: MerchantCatalogRepositoryImpl
    ): MerchantCatalogRepository
}
