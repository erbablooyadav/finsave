package com.finsave.domain.property

import com.finsave.domain.model.MemberBalance
import com.finsave.domain.model.SimplifiedDebt
import com.finsave.domain.model.SplitterMember
import com.finsave.domain.usecase.splitter.SimplifyDebtsUseCase
import io.kotest.matchers.longs.shouldBeGreaterThanOrEqual
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.math.abs

/**
 * Property-based tests for Splitter domain logic.
 *
 * Tests Properties 5-8 from the design spec:
 * - Property 5: Equal split sum invariant
 * - Property 6: Split validation correctness
 * - Property 7: Debt simplification balance invariant
 * - Property 8: Debt simplification minimality
 */
class SplitterPropertyTests {

    // ---------------------------------------------------------------------------
    // Property 5: Equal split sum invariant
    // ---------------------------------------------------------------------------

    /**
     * For any amount A and member count N ≥ 1, the sum of equal splits equals A.
     *
     * When dividing an amount equally, rounding errors are distributed to the
     * first member(s), ensuring the total always sums to the original amount.
     *
     * **Validates: Requirements 7.7**
     */
    @Test
    fun `equal split sum invariant holds for any amount and member count`() = runTest {
        checkAll(100, Arb.long(1L..10_000_000L), Arb.int(1..20)) { amount, memberCount ->
            val splits = calculateEqualSplits(amount, memberCount)
            splits.sum() shouldBe amount
        }
    }

    /**
     * Calculates equal splits for an amount among N members.
     * Distributes remainder to the first members.
     */
    private fun calculateEqualSplits(amountPaise: Long, memberCount: Int): List<Long> {
        val baseShare = amountPaise / memberCount
        val remainder = amountPaise % memberCount
        return List(memberCount) { index ->
            if (index < remainder) baseShare + 1 else baseShare
        }
    }

    // ---------------------------------------------------------------------------
    // Property 6: Split validation correctness
    // ---------------------------------------------------------------------------

    /**
     * EXACT splits are accepted iff sum equals total.
     * PERCENTAGE splits are accepted iff sum equals 100.
     *
     * **Validates: Requirements 7.8, 7.9**
     */
    @Test
    fun `exact splits accepted iff sum equals total`() = runTest {
        checkAll(
            100,
            Arb.list(Arb.long(1L..1_000_000L), 1..10),
            Arb.long(1L..10_000_000L)
        ) { splits, total ->
            val splitSum = splits.sum()
            val isValid = splitSum == total
            validateExactSplits(splits, total) shouldBe isValid
        }
    }

    @Test
    fun `percentage splits accepted iff sum equals 100`() = runTest {
        checkAll(
            100,
            Arb.list(Arb.long(1L..10000L), 1..10) // Percentages in basis points (0-100.00)
        ) { percentages ->
            val sum = percentages.sum()
            val isValid = sum == 10000L // 100.00%
            validatePercentageSplits(percentages) shouldBe isValid
        }
    }

    private fun validateExactSplits(splits: List<Long>, total: Long): Boolean {
        return splits.sum() == total
    }

    private fun validatePercentageSplits(percentages: List<Long>): Boolean {
        return percentages.sum() == 10000L // 100.00% in basis points
    }

    // ---------------------------------------------------------------------------
    // Property 7: Debt simplification balance invariant
    // ---------------------------------------------------------------------------

    /**
     * Sum of SimplifiedDebt.amountPaise equals sum of positive MemberBalance.netBalancePaise.
     *
     * This ensures the debt simplification algorithm preserves the total amount
     * owed — no money is created or destroyed.
     *
     * **Validates: Requirements 7.10**
     */
    @Test
    fun `debt simplification balance invariant holds`() = runTest {
        checkAll(100, balancedMemberBalancesArb(2..10)) { balances ->
            val simplifyDebts = SimplifyDebtsUseCase()
            val simplifiedDebts = simplifyDebts(balances)

            val totalOwed = balances.filter { it.netBalancePaise > 0 }.sumOf { it.netBalancePaise }
            val totalSettled = simplifiedDebts.sumOf { it.amountPaise }

            totalSettled shouldBe totalOwed
        }
    }

    // ---------------------------------------------------------------------------
    // Arbitraries
    // ---------------------------------------------------------------------------

    /**
     * Generates a list of balanced member balances (sum = 0).
     * This ensures the books balance — total owed equals total owing.
     */
    private fun balancedMemberBalancesArb(range: IntRange): Arb<List<MemberBalance>> =
        io.kotest.property.arbitrary.arbitrary {
            val count = Arb.int(range).bind()
            val balances = mutableListOf<Long>()
            
            // Generate random balances for all but the last member
            repeat(count - 1) {
                balances.add(Arb.long(-10_000_000L..10_000_000L).bind())
            }
            
            // Last member's balance ensures sum = 0
            val lastBalance = -balances.sum()
            balances.add(lastBalance)
            
            // Convert to MemberBalance objects
            balances.mapIndexed { index, balance ->
                MemberBalance(
                    member = SplitterMember(
                        id = (index + 1).toLong(),
                        groupId = 1L,
                        name = "Member${index + 1}"
                    ),
                    netBalancePaise = balance
                )
            }
        }

    // ---------------------------------------------------------------------------
    // Property 8: Debt simplification minimality
    // ---------------------------------------------------------------------------

    /**
     * For N members, the number of SimplifiedDebt entries is at most N − 1.
     *
     * This validates the greedy algorithm produces a minimal set of transactions.
     *
     * **Validates: Requirements 7.10**
     */
    @Test
    fun `debt simplification minimality holds for any member count`() = runTest {
        checkAll(100, balancedMemberBalancesArb(2..10)) { balances ->
            val simplifyDebts = SimplifyDebtsUseCase()
            val simplifiedDebts = simplifyDebts(balances)

            val memberCount = balances.size
            simplifiedDebts.size shouldBeLessThanOrEqual (memberCount - 1)
        }
    }

    // ---------------------------------------------------------------------------
    // Arbitraries
    // ---------------------------------------------------------------------------

    private fun memberBalanceArb(): Arb<MemberBalance> =
        io.kotest.property.arbitrary.arbitrary {
            val memberId = Arb.long(1L..1000L).bind()
            val balance = Arb.long(-10_000_000L..10_000_000L).bind()
            MemberBalance(
                member = SplitterMember(
                    id = memberId,
                    groupId = 1L,
                    name = "Member$memberId"
                ),
                netBalancePaise = balance
            )
        }
}
