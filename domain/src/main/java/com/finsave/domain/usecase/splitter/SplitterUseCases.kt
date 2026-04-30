package com.finsave.domain.usecase.splitter

import com.finsave.domain.model.MemberBalance
import com.finsave.domain.model.SplitterExpense
import com.finsave.domain.model.SplitterExpenseSplit
import com.finsave.domain.model.SplitterMember
import com.finsave.domain.model.SplitType
import com.finsave.domain.repository.SplitterRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Use case: Add a new member to a splitter group.
 * Validates: name non-empty, optional UPI ID regex.
 * Requirements: 7.3, 7.4, 7.5
 */
class AddMemberUseCase @Inject constructor(
    private val repository: SplitterRepository
) {
    suspend operator fun invoke(member: SplitterMember): Result<Long> {
        // Validation: name non-empty
        if (member.name.isBlank()) {
            return Result.failure(IllegalArgumentException("Member name is required"))
        }

        // Validation: UPI ID format (if provided)
        if (member.upiId != null && member.upiId.isNotBlank()) {
            val vpaRegex = Regex("^[a-zA-Z0-9.\\-]+@[a-zA-Z][a-zA-Z0-9]*$")
            if (!vpaRegex.matches(member.upiId)) {
                return Result.failure(IllegalArgumentException("Invalid UPI ID format"))
            }
        }

        return try {
            val id = repository.insertMember(member)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Use case: Add a new expense to a splitter group.
 * Validates: amount > 0, split sums correct.
 * Requirements: 7.6, 7.7, 7.8, 7.9
 */
class AddExpenseUseCase @Inject constructor(
    private val repository: SplitterRepository
) {
    suspend operator fun invoke(
        expense: SplitterExpense,
        splits: List<SplitterExpenseSplit>
    ): Result<Long> {
        // Validation: amount > 0
        if (expense.amountPaise <= 0) {
            return Result.failure(IllegalArgumentException("Expense amount must be positive"))
        }

        // Validation: split sums correct
        val splitSum = splits.sumOf { it.amountPaise }
        when (expense.splitType) {
            SplitType.EQUAL, SplitType.EXACT, SplitType.SHARES -> {
                if (splitSum != expense.amountPaise) {
                    return Result.failure(IllegalArgumentException("Split amounts must sum to the total expense amount"))
                }
            }
            SplitType.PERCENTAGE -> {
                // For percentage, the splits store the percentage values (0-100)
                // The sum should equal 100
                if (splitSum != 10000L) { // 100.00% in basis points
                    return Result.failure(IllegalArgumentException("Percentages must sum to 100%"))
                }
            }
        }

        return try {
            val expenseId = repository.insertExpense(expense)
            // Update splits with the new expense ID
            val updatedSplits = splits.map { it.copy(expenseId = expenseId) }
            repository.insertSplits(updatedSplits)
            Result.success(expenseId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Use case: Remove a member from a splitter group.
 * Validates: member has no expenses as payer.
 * Requirements: 7.11, 7.12
 */
class RemoveMemberUseCase @Inject constructor(
    private val repository: SplitterRepository
) {
    suspend operator fun invoke(memberId: Long, groupId: Long): Result<Unit> {
        // Check: member has no expenses as payer
        val expenses = repository.getExpensesByGroup(groupId).first()
        val memberExpenses = expenses.filter { it.paidByMemberId == memberId }
        if (memberExpenses.isNotEmpty()) {
            return Result.failure(IllegalStateException("Cannot remove a member with existing expenses"))
        }

        return try {
            repository.deleteMember(memberId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Use case: Calculate member balances for a splitter group.
 * Wraps SimplifyDebtsUseCase.calculateNetBalances.
 * Requirements: 7.10
 */
class CalculateMemberBalancesUseCase @Inject constructor(
    private val repository: SplitterRepository,
    private val simplifyDebts: SimplifyDebtsUseCase
) {
    suspend operator fun invoke(groupId: Long): Result<List<MemberBalance>> {
        return try {
            val members = repository.getMembersByGroup(groupId).first()
            val expenses = repository.getExpensesByGroup(groupId).first()

            // Build expense list for SimplifyDebtsUseCase
            val expenseList = expenses.map { it.paidByMemberId to it.amountPaise }

            // Build splits map for SimplifyDebtsUseCase
            val splitsMap = mutableMapOf<Long, List<Pair<Long, Long>>>()
            expenses.forEachIndexed { index, expense ->
                val splits = repository.getSplitsByExpense(expense.id).first()
                splitsMap[index.toLong()] = splits.map { it.memberId to it.amountPaise }
            }

            val balances = simplifyDebts.calculateNetBalances(members, expenseList, splitsMap)
            Result.success(balances)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
