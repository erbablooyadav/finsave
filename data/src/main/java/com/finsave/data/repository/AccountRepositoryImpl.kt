package com.finsave.data.repository

import com.finsave.data.local.dao.AccountDao
import com.finsave.data.mapper.toDomain
import com.finsave.data.mapper.toEntity
import com.finsave.domain.model.Account
import com.finsave.domain.model.AccountType
import com.finsave.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao
) : AccountRepository {

    override fun getAllAccounts(): Flow<List<Account>> =
        accountDao.getAllAccounts().map { entities -> entities.map { it.toDomain() } }

    override fun getAccountById(id: Long): Flow<Account?> =
        accountDao.getAccountById(id).map { it?.toDomain() }

    override fun getDefaultAccount(): Flow<Account?> =
        accountDao.getDefaultAccount().map { it?.toDomain() }

    override fun getTotalBalance(): Flow<Long> =
        accountDao.getTotalBalance()

    override suspend fun insertAccount(account: Account): Long =
        accountDao.insertAccount(account.toEntity())

    override suspend fun updateAccount(account: Account) =
        accountDao.updateAccount(account.toEntity())

    override suspend fun deleteAccount(id: Long) =
        accountDao.deleteAccount(id)

    override suspend fun updateBalance(accountId: Long, amountPaise: Long) =
        accountDao.updateBalance(accountId, amountPaise)

    override suspend fun insertDefaultAccounts() {
        val defaults = listOf(
            Account(name = "Bank Account", type = AccountType.SAVINGS, isDefault = true),
            Account(name = "Cash Wallet", type = AccountType.CASH, colorHex = "#FFC107")
        )
        accountDao.insertAccounts(defaults.map { it.toEntity() })
    }
}
