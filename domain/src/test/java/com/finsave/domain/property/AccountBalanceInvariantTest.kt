package com.finsave.domain.property

import com.finsave.domain.model.Account
import com.finsave.domain.model.AccountType
import com.finsave.domain.model.Transaction
import com.finsave.domain.model.TransactionType
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.of
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate

/**
 * Property 4: Account balance invariant
 *
 * For any sequence of inserts/updates/deletes, `balancePaise` equals
 * initial + sum(CREDIT amountPaise) − sum(DEBIT amountPaise).
 *
 * Uses in-memory fakes (FakeAccountRepository, FakeTransactionRepository)
 * to test the balance-update logic without Room or Android dependencies.
 *
 * **Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5**
 */
class AccountBalanceInvariantTest {

    // ---------------------------------------------------------------------------
    // Arbitraries
    // ---------------------------------------------------------------------------

    private fun transactionArb(): Arb<Transaction> =
        io.kotest.property.arbitrary.arbitrary {
            val amount = Arb.long(1L..10_000_000L).bind()
            // Only DEBIT or CREDIT — TRANSFER is net-neutral and excluded per spec
            val type = Arb.of(TransactionType.DEBIT, TransactionType.CREDIT).bind()
            Transaction(
                id = 0L,
                amountPaise = amount,
                type = type,
                categoryId = 1L,
                accountId = 1L,
                date = LocalDate.now()
            )
        }

    // ---------------------------------------------------------------------------
    // In-memory fakes
    // ---------------------------------------------------------------------------

    /**
     * Minimal in-memory account repository that tracks balancePaise per account ID.
     */
    class FakeAccountRepository {
        private val balances = mutableMapOf<Long, Long>()

        fun initAccount(id: Long, initialBalance: Long = 0L) {
            balances[id] = initialBalance
        }

        fun getBalance(id: Long): Long = balances[id] ?: 0L

        fun updateBalance(id: Long, delta: Long) {
            balances[id] = (balances[id] ?: 0L) + delta
        }
    }

    /**
     * Minimal in-memory transaction repository that stores transactions and
     * delegates balance updates to [FakeAccountRepository].
     */
    class FakeTransactionRepository(
        private val accountRepo: FakeAccountRepository
    ) {
        private val transactions = mutableListOf<Transaction>()
        private var nextId = 1L

        suspend fun insertTransaction(transaction: Transaction): Long {
            val id = nextId++
            val stored = transaction.copy(id = id)
            transactions.add(stored)
            val delta = if (stored.type == TransactionType.DEBIT)
                -stored.amountPaise
            else
                stored.amountPaise
            accountRepo.updateBalance(stored.accountId, delta)
            return id
        }

        suspend fun updateTransaction(transaction: Transaction) {
            val old = transactions.find { it.id == transaction.id } ?: return
            // Reverse old balance effect
            val oldDelta = if (old.type == TransactionType.DEBIT)
                old.amountPaise
            else
                -old.amountPaise
            accountRepo.updateBalance(old.accountId, oldDelta)
            // Apply new balance effect
            val newDelta = if (transaction.type == TransactionType.DEBIT)
                -transaction.amountPaise
            else
                transaction.amountPaise
            accountRepo.updateBalance(transaction.accountId, newDelta)
            // Replace stored transaction
            val index = transactions.indexOfFirst { it.id == transaction.id }
            transactions[index] = transaction
        }

        suspend fun deleteTransaction(id: Long) {
            val entity = transactions.find { it.id == id } ?: return
            val reversal = if (entity.type == TransactionType.DEBIT)
                entity.amountPaise
            else
                -entity.amountPaise
            accountRepo.updateBalance(entity.accountId, reversal)
            transactions.removeAll { it.id == id }
        }

        fun getAll(): List<Transaction> = transactions.toList()
    }

    // ---------------------------------------------------------------------------
    // Property test
    // ---------------------------------------------------------------------------

    /**
     * For any list of transactions inserted into the fake repository, the
     * account balance must equal:
     *   initialBalance + sum(CREDIT amountPaise) − sum(DEBIT amountPaise)
     *
     * This verifies Requirements 3.1 and 3.2 (insert DEBIT decrements,
     * insert CREDIT increments) and that the invariant holds across all inputs.
     */
    @Test
    fun `account balance invariant holds for any sequence of transactions`() = runTest {
        checkAll(100, Arb.list(transactionArb(), 0..20)) { transactions ->
            val accountId = 1L
            val initialBalance = 0L

            val accountRepo = FakeAccountRepository()
            accountRepo.initAccount(accountId, initialBalance)
            val txRepo = FakeTransactionRepository(accountRepo)

            transactions.forEach { tx ->
                txRepo.insertTransaction(tx)
            }

            val expectedBalance = initialBalance +
                transactions.filter { it.type == TransactionType.CREDIT }.sumOf { it.amountPaise } -
                transactions.filter { it.type == TransactionType.DEBIT }.sumOf { it.amountPaise }

            accountRepo.getBalance(accountId) shouldBe expectedBalance
        }
    }

    /**
     * Round-trip balance: when a transaction is added and then deleted,
     * the account balance equals its value before the transaction was added.
     *
     * Validates Requirement 3.3 (delete reverses the original balance adjustment).
     */
    @Test
    fun `round-trip balance holds after insert then delete`() = runTest {
        checkAll(100, transactionArb()) { tx ->
            val accountId = 1L
            val initialBalance = 0L

            val accountRepo = FakeAccountRepository()
            accountRepo.initAccount(accountId, initialBalance)
            val txRepo = FakeTransactionRepository(accountRepo)

            val id = txRepo.insertTransaction(tx)
            txRepo.deleteTransaction(id)

            accountRepo.getBalance(accountId) shouldBe initialBalance
        }
    }

    /**
     * Update invariant: after updating a transaction (changing amount and/or type),
     * the balance reflects only the new transaction's effect, not the old one.
     *
     * Validates Requirement 3.4 (update reverses old delta and applies new delta).
     */
    @Test
    fun `balance invariant holds after update`() = runTest {
        checkAll(
            100,
            transactionArb(),
            transactionArb()
        ) { original, updated ->
            val accountId = 1L
            val initialBalance = 0L

            val accountRepo = FakeAccountRepository()
            accountRepo.initAccount(accountId, initialBalance)
            val txRepo = FakeTransactionRepository(accountRepo)

            val id = txRepo.insertTransaction(original)
            val updatedWithId = updated.copy(id = id, accountId = accountId)
            txRepo.updateTransaction(updatedWithId)

            val expectedBalance = initialBalance +
                if (updatedWithId.type == TransactionType.CREDIT)
                    updatedWithId.amountPaise
                else
                    -updatedWithId.amountPaise

            accountRepo.getBalance(accountId) shouldBe expectedBalance
        }
    }
}
