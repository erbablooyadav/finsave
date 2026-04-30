package com.finsave.domain.model

import java.time.LocalDateTime

/**
 * Bill Splitter domain models.
 *
 * Designed for the killer feature — group expense splitting with
 * UPI settle-up deep links. All balances are in paise for precision.
 */

/**
 * A group of people splitting expenses (e.g., "Goa Trip", "Flat Mates").
 * Groups exist entirely on-device — no server, no accounts.
 */
data class SplitterGroup(
    val id: Long = 0,
    val name: String,
    val emoji: String = "👥",
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val isSettled: Boolean = false,
    val isArchived: Boolean = false
)

/**
 * A member of a splitter group.
 * Members are local — they don't need to be FinSave users.
 * UPI ID is optional but enables one-tap settle-up.
 */
data class SplitterMember(
    val id: Long = 0,
    val groupId: Long,
    val name: String,
    val upiId: String? = null,
    val isCurrentUser: Boolean = false,
    val colorHex: String = "#4F46E5"
)

/**
 * An expense within a splitter group.
 */
data class SplitterExpense(
    val id: Long = 0,
    val groupId: Long,
    val description: String,
    val amountPaise: Long,
    val paidByMemberId: Long,
    val splitType: SplitType,
    val date: LocalDateTime = LocalDateTime.now()
)

/**
 * How an expense is divided among group members.
 */
enum class SplitType {
    EQUAL,       // Divide equally among all members
    EXACT,       // Each member's exact share entered manually
    PERCENTAGE,  // Each member's percentage of total
    SHARES       // Ratio-based (e.g., 2:1:1)
}

/**
 * The split detail — how much each member owes for a specific expense.
 */
data class SplitterExpenseSplit(
    val id: Long = 0,
    val expenseId: Long,
    val memberId: Long,
    val amountPaise: Long
)

/**
 * A simplified debt between two members — output of the debt simplification algorithm.
 */
data class SimplifiedDebt(
    val fromMember: SplitterMember,
    val toMember: SplitterMember,
    val amountPaise: Long
)

/**
 * Balance summary for a single member within a group.
 */
data class MemberBalance(
    val member: SplitterMember,
    val netBalancePaise: Long // Positive = owed money, Negative = owes money
)
