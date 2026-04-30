package com.finsave.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Entity: Transaction
 *
 * Amounts stored as Long in paise (1 INR = 100 paise).
 * Indexed on date, categoryId, accountId for fast queries.
 * smsHash enables deduplication of auto-imported SMS transactions.
 */
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["account_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["date"]),
        Index(value = ["category_id"]),
        Index(value = ["account_id"]),
        Index(value = ["type"]),
        Index(value = ["sms_hash"], unique = true),
        Index(value = ["merchant_name"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "amount_paise")
    val amountPaise: Long,

    @ColumnInfo(name = "type")
    val type: String, // DEBIT, CREDIT, TRANSFER

    @ColumnInfo(name = "category_id")
    val categoryId: Long?,

    @ColumnInfo(name = "account_id")
    val accountId: Long,

    @ColumnInfo(name = "merchant_name")
    val merchantName: String = "",

    @ColumnInfo(name = "note")
    val note: String = "",

    @ColumnInfo(name = "date")
    val date: Long, // Epoch millis at start of day (IST)

    @ColumnInfo(name = "upi_id")
    val upiId: String? = null,

    @ColumnInfo(name = "is_auto_imported")
    val isAutoImported: Boolean = false,

    @ColumnInfo(name = "sms_hash")
    val smsHash: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Room Entity: Category
 *
 * 18 default categories ship with the app.
 * Users can add unlimited custom categories.
 */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "emoji")
    val emoji: String,

    @ColumnInfo(name = "color_hex")
    val colorHex: String,

    @ColumnInfo(name = "budget_limit_paise")
    val budgetLimitPaise: Long? = null,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,

    @ColumnInfo(name = "is_default")
    val isDefault: Boolean = true,

    @ColumnInfo(name = "is_custom")
    val isCustom: Boolean = false
)

/**
 * Room Entity: Account
 *
 * Mirrors real bank/wallet accounts for tracking.
 * FinSave never connects to actual banks.
 */
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "type")
    val type: String, // SAVINGS, CURRENT, CREDIT_CARD, WALLET, CASH, INVESTMENT

    @ColumnInfo(name = "balance_paise")
    val balancePaise: Long = 0,

    @ColumnInfo(name = "color_hex")
    val colorHex: String = "#4F46E5",

    @ColumnInfo(name = "is_default")
    val isDefault: Boolean = false,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0
)

/**
 * Room Entity: Budget
 */
@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["category_id"])]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "category_id")
    val categoryId: Long? = null, // null = total budget across all categories

    @ColumnInfo(name = "limit_amount_paise")
    val limitAmountPaise: Long,

    @ColumnInfo(name = "period_type")
    val periodType: String, // MONTHLY, WEEKLY, CUSTOM

    @ColumnInfo(name = "start_date")
    val startDate: Long,

    @ColumnInfo(name = "end_date")
    val endDate: Long? = null,

    @ColumnInfo(name = "rollover")
    val rollover: Boolean = false,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true
)

/**
 * Room Entity: Splitter Group
 */
@Entity(tableName = "splitter_groups")
data class SplitterGroupEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "emoji")
    val emoji: String = "👥",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_settled")
    val isSettled: Boolean = false,

    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean = false
)

/**
 * Room Entity: Splitter Member
 */
@Entity(
    tableName = "splitter_members",
    foreignKeys = [
        ForeignKey(
            entity = SplitterGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["group_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["group_id"])]
)
data class SplitterMemberEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "group_id")
    val groupId: Long,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "upi_id")
    val upiId: String? = null,

    @ColumnInfo(name = "is_current_user")
    val isCurrentUser: Boolean = false,

    @ColumnInfo(name = "color_hex")
    val colorHex: String = "#4F46E5"
)

/**
 * Room Entity: Splitter Expense
 */
@Entity(
    tableName = "splitter_expenses",
    foreignKeys = [
        ForeignKey(
            entity = SplitterGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["group_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SplitterMemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["paid_by_member_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["group_id"]),
        Index(value = ["paid_by_member_id"])
    ]
)
data class SplitterExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "group_id")
    val groupId: Long,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "amount_paise")
    val amountPaise: Long,

    @ColumnInfo(name = "paid_by_member_id")
    val paidByMemberId: Long,

    @ColumnInfo(name = "split_type")
    val splitType: String, // EQUAL, EXACT, PERCENTAGE, SHARES

    @ColumnInfo(name = "date")
    val date: Long = System.currentTimeMillis()
)

/**
 * Room Entity: Splitter Expense Split (Join table)
 * Links expenses to members with their individual share amounts.
 */
@Entity(
    tableName = "splitter_expense_splits",
    foreignKeys = [
        ForeignKey(
            entity = SplitterExpenseEntity::class,
            parentColumns = ["id"],
            childColumns = ["expense_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SplitterMemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["member_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["expense_id"]),
        Index(value = ["member_id"])
    ]
)
data class SplitterExpenseSplitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "expense_id")
    val expenseId: Long,

    @ColumnInfo(name = "member_id")
    val memberId: Long,

    @ColumnInfo(name = "amount_paise")
    val amountPaise: Long
)
