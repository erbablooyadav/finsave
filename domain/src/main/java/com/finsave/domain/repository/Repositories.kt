package com.finsave.domain.repository

import com.finsave.domain.model.Account
import com.finsave.domain.model.Budget
import com.finsave.domain.model.BudgetSummary
import com.finsave.domain.model.Category
import com.finsave.domain.model.SplitterExpense
import com.finsave.domain.model.SplitterExpenseSplit
import com.finsave.domain.model.SplitterGroup
import com.finsave.domain.model.SplitterMember
import com.finsave.domain.model.Transaction
import com.finsave.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Transaction repository interface.
 * All data operations return Flow for reactive UI updates.
 */
interface TransactionRepository {
    fun getAllTransactions(): Flow<List<Transaction>>
    fun getTransactionsByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<Transaction>>
    fun getTransactionsByCategory(categoryId: Long): Flow<List<Transaction>>
    fun getTransactionsByAccount(accountId: Long): Flow<List<Transaction>>
    fun getTransactionById(id: Long): Flow<Transaction?>
    fun getRecentTransactions(limit: Int = 5): Flow<List<Transaction>>
    fun searchTransactions(query: String): Flow<List<Transaction>>
    fun getTotalByTypeAndDateRange(type: TransactionType, startDate: LocalDate, endDate: LocalDate): Flow<Long>
    fun getSpendByCategory(startDate: LocalDate, endDate: LocalDate): Flow<List<com.finsave.domain.model.CategorySpendSummary>>
    suspend fun insertTransaction(transaction: Transaction): Long
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(id: Long)
    suspend fun isSmsDuplicate(smsHash: String): Boolean
}

/**
 * Category repository interface.
 */
interface CategoryRepository {
    fun getAllCategories(): Flow<List<Category>>
    fun getCategoryById(id: Long): Flow<Category?>
    fun getDefaultCategories(): Flow<List<Category>>
    suspend fun insertCategory(category: Category): Long
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(id: Long)
    suspend fun insertDefaultCategories()
}

/**
 * Account repository interface.
 */
interface AccountRepository {
    fun getAllAccounts(): Flow<List<Account>>
    fun getAccountById(id: Long): Flow<Account?>
    fun getDefaultAccount(): Flow<Account?>
    fun getTotalBalance(): Flow<Long>
    suspend fun insertAccount(account: Account): Long
    suspend fun updateAccount(account: Account)
    suspend fun deleteAccount(id: Long)
    suspend fun updateBalance(accountId: Long, amountPaise: Long)
    suspend fun insertDefaultAccounts()
}

/**
 * Budget repository interface.
 */
interface BudgetRepository {
    fun getAllBudgets(): Flow<List<Budget>>
    fun getActiveBudgets(): Flow<List<Budget>>
    fun getBudgetForCategory(categoryId: Long): Flow<Budget?>
    fun getBudgetSummaries(startDate: LocalDate, endDate: LocalDate): Flow<List<BudgetSummary>>
    suspend fun insertBudget(budget: Budget): Long
    suspend fun updateBudget(budget: Budget)
    suspend fun deleteBudget(id: Long)
}

/**
 * Splitter repository interface — handles all bill splitting data.
 */
interface SplitterRepository {
    // Groups
    fun getAllGroups(): Flow<List<SplitterGroup>>
    fun getActiveGroups(): Flow<List<SplitterGroup>>
    fun getGroupById(id: Long): Flow<SplitterGroup?>
    suspend fun insertGroup(group: SplitterGroup): Long
    suspend fun updateGroup(group: SplitterGroup)
    suspend fun deleteGroup(id: Long)

    // Members
    fun getMembersByGroup(groupId: Long): Flow<List<SplitterMember>>
    fun getMemberById(id: Long): Flow<SplitterMember?>
    suspend fun insertMember(member: SplitterMember): Long
    suspend fun updateMember(member: SplitterMember)
    suspend fun deleteMember(id: Long)

    // Expenses
    fun getExpensesByGroup(groupId: Long): Flow<List<SplitterExpense>>
    fun getExpenseById(id: Long): Flow<SplitterExpense?>
    suspend fun insertExpense(expense: SplitterExpense): Long
    suspend fun updateExpense(expense: SplitterExpense)
    suspend fun deleteExpense(id: Long)

    // Splits
    fun getSplitsByExpense(expenseId: Long): Flow<List<SplitterExpenseSplit>>
    suspend fun insertSplits(splits: List<SplitterExpenseSplit>)
    suspend fun deleteSplitsByExpense(expenseId: Long)
}

interface DataResetRepository {
    suspend fun clearAllData()
}

interface MerchantCatalogRepository {
    suspend fun getBundledMerchantNames(): List<String>
}
