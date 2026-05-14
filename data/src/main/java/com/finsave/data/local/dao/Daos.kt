package com.finsave.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.finsave.data.local.entity.AccountEntity
import com.finsave.data.local.entity.BudgetEntity
import com.finsave.data.local.entity.CategoryEntity
import com.finsave.data.local.entity.SplitterExpenseEntity
import com.finsave.data.local.entity.SplitterExpenseSplitEntity
import com.finsave.data.local.entity.SplitterGroupEntity
import com.finsave.data.local.entity.SplitterMemberEntity
import com.finsave.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

// ─────────────────────────────────────────────────────────────────────────────
// Transaction DAO
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY date DESC, created_at DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT COUNT(*) FROM transactions")
    fun getTransactionCount(): Flow<Int>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE category_id = :categoryId ORDER BY date DESC")
    fun getTransactionsByCategory(categoryId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE account_id = :accountId ORDER BY date DESC")
    fun getTransactionsByAccount(accountId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun getTransactionById(id: Long): Flow<TransactionEntity?>

    /** One-shot query for use inside Room transactions (avoids Flow + withTransaction deadlock). */
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionByIdOnce(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions ORDER BY date DESC, created_at DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions 
        WHERE merchant_name LIKE '%' || :query || '%' 
           OR note LIKE '%' || :query || '%'
        ORDER BY date DESC
    """)
    fun searchTransactions(query: String): Flow<List<TransactionEntity>>
    
    @Query("""
        SELECT t.* FROM transactions t 
        JOIN transactions_fts f ON t.id = f.rowid 
        WHERE transactions_fts MATCH :query
        ORDER BY t.date DESC
    """)
    fun searchTransactionsFts(query: String): Flow<List<TransactionEntity>>

    @Query("""
        SELECT COALESCE(SUM(amount_paise), 0) FROM transactions 
        WHERE type = :type AND date BETWEEN :startDate AND :endDate
    """)
    fun getTotalByTypeAndDateRange(type: String, startDate: Long, endDate: Long): Flow<Long>

    @Query("""
        SELECT COALESCE(SUM(amount_paise), 0) as total, category_id 
        FROM transactions 
        WHERE type = 'DEBIT' AND date BETWEEN :startDate AND :endDate 
        GROUP BY category_id
    """)
    fun getSpendByCategory(startDate: Long, endDate: Long): Flow<List<CategorySpend>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransaction(id: Long)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE sms_hash = :smsHash)")
    suspend fun isSmsDuplicate(smsHash: String): Boolean
}

/**
 * Projection for category-level spending aggregation.
 */
data class CategorySpend(
    val total: Long,
    val category_id: Long?
)

// ─────────────────────────────────────────────────────────────────────────────
// Category DAO
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY sort_order ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    fun getCategoryById(id: Long): Flow<CategoryEntity?>

    @Query("SELECT * FROM categories WHERE is_default = 1 ORDER BY sort_order ASC")
    fun getDefaultCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id AND is_default = 0")
    suspend fun deleteCategory(id: Long)

    @Query("DELETE FROM categories WHERE is_default = 0")
    suspend fun deleteAllCustomCategories()

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int
}

// ─────────────────────────────────────────────────────────────────────────────
// Account DAO
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface AccountDao {

    @Query("SELECT * FROM accounts ORDER BY sort_order ASC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    fun getAccountById(id: Long): Flow<AccountEntity?>

    @Query("SELECT * FROM accounts WHERE is_default = 1 LIMIT 1")
    fun getDefaultAccount(): Flow<AccountEntity?>

    @Query("SELECT COALESCE(SUM(balance_paise), 0) FROM accounts")
    fun getTotalBalance(): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<AccountEntity>)

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteAccount(id: Long)

    @Query("UPDATE accounts SET balance_paise = balance_paise + :amountPaise WHERE id = :accountId")
    suspend fun updateBalance(accountId: Long, amountPaise: Long)

    @Query("UPDATE accounts SET balance_paise = 0")
    suspend fun resetAllBalancesToZero()

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun getAccountCount(): Int
}

// ─────────────────────────────────────────────────────────────────────────────
// Budget DAO
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface BudgetDao {

    @Query("SELECT * FROM budgets ORDER BY category_id ASC")
    fun getAllBudgets(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE is_active = 1")
    fun getActiveBudgets(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE category_id = :categoryId AND is_active = 1 LIMIT 1")
    fun getBudgetForCategory(categoryId: Long): Flow<BudgetEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity): Long

    @Update
    suspend fun updateBudget(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteBudget(id: Long)

    @Query("DELETE FROM budgets")
    suspend fun deleteAllBudgets()
}

// ─────────────────────────────────────────────────────────────────────────────
// Splitter DAOs
// ─────────────────────────────────────────────────────────────────────────────

@Dao
interface SplitterGroupDao {

    @Query("SELECT * FROM splitter_groups ORDER BY created_at DESC")
    fun getAllGroups(): Flow<List<SplitterGroupEntity>>

    @Query("SELECT * FROM splitter_groups WHERE is_settled = 0 AND is_archived = 0 ORDER BY created_at DESC")
    fun getActiveGroups(): Flow<List<SplitterGroupEntity>>

    @Query("SELECT * FROM splitter_groups WHERE id = :id")
    fun getGroupById(id: Long): Flow<SplitterGroupEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: SplitterGroupEntity): Long

    @Update
    suspend fun updateGroup(group: SplitterGroupEntity)

    @Query("DELETE FROM splitter_groups WHERE id = :id")
    suspend fun deleteGroup(id: Long)

    @Query("DELETE FROM splitter_groups")
    suspend fun deleteAllGroups()
}

@Dao
interface SplitterMemberDao {

    @Query("SELECT * FROM splitter_members WHERE group_id = :groupId ORDER BY is_current_user DESC, name ASC")
    fun getMembersByGroup(groupId: Long): Flow<List<SplitterMemberEntity>>

    @Query("SELECT * FROM splitter_members WHERE id = :id")
    fun getMemberById(id: Long): Flow<SplitterMemberEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: SplitterMemberEntity): Long

    @Update
    suspend fun updateMember(member: SplitterMemberEntity)

    @Query("DELETE FROM splitter_members WHERE id = :id")
    suspend fun deleteMember(id: Long)

    @Query("DELETE FROM splitter_members")
    suspend fun deleteAllMembers()
}

@Dao
interface SplitterExpenseDao {

    @Query("SELECT * FROM splitter_expenses WHERE group_id = :groupId ORDER BY date DESC")
    fun getExpensesByGroup(groupId: Long): Flow<List<SplitterExpenseEntity>>

    @Query("SELECT * FROM splitter_expenses WHERE id = :id")
    fun getExpenseById(id: Long): Flow<SplitterExpenseEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: SplitterExpenseEntity): Long

    @Update
    suspend fun updateExpense(expense: SplitterExpenseEntity)

    @Query("DELETE FROM splitter_expenses WHERE id = :id")
    suspend fun deleteExpense(id: Long)

    @Query("DELETE FROM splitter_expenses")
    suspend fun deleteAllExpenses()
}

@Dao
interface SplitterSplitDao {

    @Query("SELECT * FROM splitter_expense_splits WHERE expense_id = :expenseId")
    fun getSplitsByExpense(expenseId: Long): Flow<List<SplitterExpenseSplitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSplits(splits: List<SplitterExpenseSplitEntity>)

    @Query("DELETE FROM splitter_expense_splits WHERE expense_id = :expenseId")
    suspend fun deleteSplitsByExpense(expenseId: Long)

    @Query("DELETE FROM splitter_expense_splits")
    suspend fun deleteAllSplits()
}

@Dao
interface AnalyticsDao {

    @Query("SELECT * FROM analytics_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<com.finsave.data.local.entity.AnalyticsEventEntity>>

    @Query("SELECT * FROM analytics_events ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentEvents(limit: Int): Flow<List<com.finsave.data.local.entity.AnalyticsEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: com.finsave.data.local.entity.AnalyticsEventEntity)

    @Query("DELETE FROM analytics_events WHERE timestamp < :timestamp")
    suspend fun deleteEventsOlderThan(timestamp: Long)

    @Query("DELETE FROM analytics_events")
    suspend fun deleteAllEvents()
}
