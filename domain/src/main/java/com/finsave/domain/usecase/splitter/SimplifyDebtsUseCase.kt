package com.finsave.domain.usecase.splitter

import com.finsave.domain.model.MemberBalance
import com.finsave.domain.model.SimplifiedDebt
import com.finsave.domain.model.SplitterMember

import javax.inject.Inject

/**
 * SimplifyDebtsUseCase — Minimizes the number of payment transactions
 * required to fully settle a group's debts.
 *
 * Algorithm:
 * 1. Calculate net balance for each member.
 * 2. Separate into creditors (positive balance) and debtors (negative balance).
 * 3. Greedy matching: Match largest debtor with largest creditor.
 *    Settle the minimum of their absolute amounts. Repeat.
 *
 * Time complexity: O(n log n) where n = number of members.
 * Space complexity: O(n).
 *
 * For a group of 5 people with random debts, this typically reduces
 * 10+ bilateral debts to 4–5 net transactions.
 *
 * This is a pure Kotlin class with no Android dependencies — fully unit-testable.
 */
class SimplifyDebtsUseCase @Inject constructor() {

    /**
     * Simplifies debts for a group given member balances.
     *
     * @param memberBalances List of member balances (positive = owed, negative = owes)
     * @return Minimized list of debt transactions to settle all balances
     */
    operator fun invoke(memberBalances: List<MemberBalance>): List<SimplifiedDebt> {
        if (memberBalances.isEmpty()) return emptyList()

        // Separate into creditors and debtors
        // Creditor: positive balance (they are owed money)
        // Debtor: negative balance (they owe money)
        val creditors = memberBalances
            .filter { it.netBalancePaise > 0 }
            .sortedByDescending { it.netBalancePaise }
            .map { SettlementEntry(it.member, it.netBalancePaise) }
            .toMutableList()

        val debtors = memberBalances
            .filter { it.netBalancePaise < 0 }
            .sortedBy { it.netBalancePaise } // Most negative first
            .map { SettlementEntry(it.member, -it.netBalancePaise) } // Store as positive
            .toMutableList()

        val settlements = mutableListOf<SimplifiedDebt>()

        var creditorIdx = 0
        var debtorIdx = 0

        while (creditorIdx < creditors.size && debtorIdx < debtors.size) {
            val creditor = creditors[creditorIdx]
            val debtor = debtors[debtorIdx]

            // Settle the smaller of the two amounts
            val settlementAmount = minOf(creditor.remainingPaise, debtor.remainingPaise)

            if (settlementAmount > 0) {
                settlements.add(
                    SimplifiedDebt(
                        fromMember = debtor.member,
                        toMember = creditor.member,
                        amountPaise = settlementAmount
                    )
                )
            }

            // Reduce both balances
            creditor.remainingPaise -= settlementAmount
            debtor.remainingPaise -= settlementAmount

            // Move to next creditor/debtor if settled
            if (creditor.remainingPaise == 0L) creditorIdx++
            if (debtor.remainingPaise == 0L) debtorIdx++
        }

        return settlements
    }

    /**
     * Calculates net balances for all members from expenses and splits.
     *
     * For each expense:
     * - The payer gains credit equal to (total - their own split)
     * - Each other member gains debt equal to their split amount
     *
     * @param memberIds All member IDs in the group
     * @param expenses Pairs of (paidByMemberId, totalAmountPaise)
     * @param splits Triples of (expenseId, memberId, splitAmountPaise)
     * @return Map of memberId to netBalancePaise
     */
    fun calculateNetBalances(
        members: List<SplitterMember>,
        expenses: List<Pair<Long, Long>>, // (paidByMemberId, amountPaise)
        splits: Map<Long, List<Pair<Long, Long>>> // expenseId -> [(memberId, amountPaise)]
    ): List<MemberBalance> {
        val balances = mutableMapOf<Long, Long>()

        // Initialize all balances to 0
        members.forEach { balances[it.id] = 0L }

        // For each expense, the payer paid the full amount
        // Each member owes their split share
        expenses.forEachIndexed { index, (paidByMemberId, amountPaise) ->
            val expenseSplits = splits[index.toLong()] ?: return@forEachIndexed

            // Payer paid the full amount — they are owed by everyone
            balances[paidByMemberId] = (balances[paidByMemberId] ?: 0L) + amountPaise

            // Each member owes their split
            expenseSplits.forEach { (memberId, splitAmount) ->
                balances[memberId] = (balances[memberId] ?: 0L) - splitAmount
            }
        }

        return members.map { member ->
            MemberBalance(
                member = member,
                netBalancePaise = balances[member.id] ?: 0L
            )
        }
    }

    /**
     * Mutable entry used during the greedy settlement algorithm.
     */
    private class SettlementEntry(
        val member: SplitterMember,
        var remainingPaise: Long
    )
}
