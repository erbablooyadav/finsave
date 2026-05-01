package com.finsave.data.repository

import androidx.room.withTransaction
import com.finsave.data.local.FinSaveDatabase
import com.finsave.data.local.dao.AccountDao
import com.finsave.data.local.dao.BudgetDao
import com.finsave.data.local.dao.CategoryDao
import com.finsave.data.local.dao.SplitterExpenseDao
import com.finsave.data.local.dao.SplitterGroupDao
import com.finsave.data.local.dao.SplitterMemberDao
import com.finsave.data.local.dao.SplitterSplitDao
import com.finsave.data.local.dao.TransactionDao
import com.finsave.domain.repository.DataResetRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataResetRepositoryImpl @Inject constructor(
    private val database: FinSaveDatabase,
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao,
    private val categoryDao: CategoryDao,
    private val accountDao: AccountDao,
    private val splitterGroupDao: SplitterGroupDao,
    private val splitterMemberDao: SplitterMemberDao,
    private val splitterExpenseDao: SplitterExpenseDao,
    private val splitterSplitDao: SplitterSplitDao
) : DataResetRepository {

    override suspend fun clearAllData() {
        database.withTransaction {
            splitterSplitDao.deleteAllSplits()
            splitterExpenseDao.deleteAllExpenses()
            splitterMemberDao.deleteAllMembers()
            splitterGroupDao.deleteAllGroups()
            transactionDao.deleteAllTransactions()
            budgetDao.deleteAllBudgets()
            categoryDao.deleteAllCustomCategories()
            accountDao.resetAllBalancesToZero()
        }
    }
}
