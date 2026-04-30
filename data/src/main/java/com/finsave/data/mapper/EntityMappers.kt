package com.finsave.data.mapper

import com.finsave.data.local.entity.AccountEntity
import com.finsave.data.local.entity.BudgetEntity
import com.finsave.data.local.entity.CategoryEntity
import com.finsave.data.local.entity.SplitterExpenseEntity
import com.finsave.data.local.entity.SplitterExpenseSplitEntity
import com.finsave.data.local.entity.SplitterGroupEntity
import com.finsave.data.local.entity.SplitterMemberEntity
import com.finsave.data.local.entity.TransactionEntity
import com.finsave.domain.model.Account
import com.finsave.domain.model.AccountType
import com.finsave.domain.model.Budget
import com.finsave.domain.model.BudgetPeriod
import com.finsave.domain.model.Category
import com.finsave.domain.model.SplitType
import com.finsave.domain.model.SplitterExpense
import com.finsave.domain.model.SplitterExpenseSplit
import com.finsave.domain.model.SplitterGroup
import com.finsave.domain.model.SplitterMember
import com.finsave.domain.model.Transaction
import com.finsave.domain.model.TransactionType
import com.finsave.core.common.extensions.toLocalDate
import com.finsave.core.common.extensions.toLocalDateTime
import com.finsave.core.common.extensions.toEpochMillis
import com.finsave.core.common.extensions.IST
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Entity ↔ Domain mappers.
 *
 * Keeps the data layer (Room entities) decoupled from the domain layer.
 * All conversions happen here — domain models never know about Room.
 */

// ── Transaction ────────────────────────────────────────────────────────────

fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = id,
    amountPaise = amountPaise,
    type = TransactionType.valueOf(type),
    categoryId = categoryId ?: 0,
    accountId = accountId,
    merchantName = merchantName,
    note = note,
    date = date.toLocalDate(),
    upiId = upiId,
    isAutoImported = isAutoImported,
    smsHash = smsHash,
    createdAt = createdAt.toLocalDateTime()
)

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    amountPaise = amountPaise,
    type = type.name,
    categoryId = if (categoryId == 0L) null else categoryId,
    accountId = accountId,
    merchantName = merchantName,
    note = note,
    date = date.toEpochMillis(),
    upiId = upiId,
    isAutoImported = isAutoImported,
    smsHash = smsHash,
    createdAt = createdAt.atZone(IST).toInstant().toEpochMilli()
)

// ── Category ───────────────────────────────────────────────────────────────

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    emoji = emoji,
    colorHex = colorHex,
    budgetLimitPaise = budgetLimitPaise,
    sortOrder = sortOrder,
    isDefault = isDefault,
    isCustom = isCustom
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    emoji = emoji,
    colorHex = colorHex,
    budgetLimitPaise = budgetLimitPaise,
    sortOrder = sortOrder,
    isDefault = isDefault,
    isCustom = isCustom
)

// ── Account ────────────────────────────────────────────────────────────────

fun AccountEntity.toDomain(): Account = Account(
    id = id,
    name = name,
    type = AccountType.valueOf(type),
    balancePaise = balancePaise,
    colorHex = colorHex,
    isDefault = isDefault,
    sortOrder = sortOrder
)

fun Account.toEntity(): AccountEntity = AccountEntity(
    id = id,
    name = name,
    type = type.name,
    balancePaise = balancePaise,
    colorHex = colorHex,
    isDefault = isDefault,
    sortOrder = sortOrder
)

// ── Budget ─────────────────────────────────────────────────────────────────

fun BudgetEntity.toDomain(): Budget = Budget(
    id = id,
    categoryId = categoryId,
    limitAmountPaise = limitAmountPaise,
    periodType = BudgetPeriod.valueOf(periodType),
    startDate = startDate.toLocalDate(),
    endDate = endDate?.toLocalDate(),
    rollover = rollover,
    isActive = isActive
)

fun Budget.toEntity(): BudgetEntity = BudgetEntity(
    id = id,
    categoryId = categoryId,
    limitAmountPaise = limitAmountPaise,
    periodType = periodType.name,
    startDate = startDate.toEpochMillis(),
    endDate = endDate?.toEpochMillis(),
    rollover = rollover,
    isActive = isActive
)

// ── Splitter Group ─────────────────────────────────────────────────────────

fun SplitterGroupEntity.toDomain(): SplitterGroup = SplitterGroup(
    id = id,
    name = name,
    emoji = emoji,
    createdAt = createdAt.toLocalDateTime(),
    isSettled = isSettled,
    isArchived = isArchived
)

fun SplitterGroup.toEntity(): SplitterGroupEntity = SplitterGroupEntity(
    id = id,
    name = name,
    emoji = emoji,
    createdAt = createdAt.atZone(IST).toInstant().toEpochMilli(),
    isSettled = isSettled,
    isArchived = isArchived
)

// ── Splitter Member ────────────────────────────────────────────────────────

fun SplitterMemberEntity.toDomain(): SplitterMember = SplitterMember(
    id = id,
    groupId = groupId,
    name = name,
    upiId = upiId,
    isCurrentUser = isCurrentUser,
    colorHex = colorHex
)

fun SplitterMember.toEntity(): SplitterMemberEntity = SplitterMemberEntity(
    id = id,
    groupId = groupId,
    name = name,
    upiId = upiId,
    isCurrentUser = isCurrentUser,
    colorHex = colorHex
)

// ── Splitter Expense ───────────────────────────────────────────────────────

fun SplitterExpenseEntity.toDomain(): SplitterExpense = SplitterExpense(
    id = id,
    groupId = groupId,
    description = description,
    amountPaise = amountPaise,
    paidByMemberId = paidByMemberId,
    splitType = SplitType.valueOf(splitType),
    date = date.toLocalDateTime()
)

fun SplitterExpense.toEntity(): SplitterExpenseEntity = SplitterExpenseEntity(
    id = id,
    groupId = groupId,
    description = description,
    amountPaise = amountPaise,
    paidByMemberId = paidByMemberId,
    splitType = splitType.name,
    date = date.atZone(IST).toInstant().toEpochMilli()
)

// ── Splitter Expense Split ─────────────────────────────────────────────────

fun SplitterExpenseSplitEntity.toDomain(): SplitterExpenseSplit = SplitterExpenseSplit(
    id = id,
    expenseId = expenseId,
    memberId = memberId,
    amountPaise = amountPaise
)

fun SplitterExpenseSplit.toEntity(): SplitterExpenseSplitEntity = SplitterExpenseSplitEntity(
    id = id,
    expenseId = expenseId,
    memberId = memberId,
    amountPaise = amountPaise
)
