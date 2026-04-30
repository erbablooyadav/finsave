package com.finsave.data.repository

import com.finsave.data.local.dao.SplitterExpenseDao
import com.finsave.data.local.dao.SplitterGroupDao
import com.finsave.data.local.dao.SplitterMemberDao
import com.finsave.data.local.dao.SplitterSplitDao
import com.finsave.data.mapper.toDomain
import com.finsave.data.mapper.toEntity
import com.finsave.domain.model.SplitterExpense
import com.finsave.domain.model.SplitterExpenseSplit
import com.finsave.domain.model.SplitterGroup
import com.finsave.domain.model.SplitterMember
import com.finsave.domain.repository.SplitterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SplitterRepositoryImpl @Inject constructor(
    private val groupDao: SplitterGroupDao,
    private val memberDao: SplitterMemberDao,
    private val expenseDao: SplitterExpenseDao,
    private val splitDao: SplitterSplitDao
) : SplitterRepository {

    override fun getAllGroups(): Flow<List<SplitterGroup>> =
        groupDao.getAllGroups().map { list -> list.map { it.toDomain() } }

    override fun getActiveGroups(): Flow<List<SplitterGroup>> =
        groupDao.getActiveGroups().map { list -> list.map { it.toDomain() } }

    override fun getGroupById(id: Long): Flow<SplitterGroup?> =
        groupDao.getGroupById(id).map { it?.toDomain() }

    override suspend fun insertGroup(group: SplitterGroup): Long =
        groupDao.insertGroup(group.toEntity())

    override suspend fun updateGroup(group: SplitterGroup) =
        groupDao.updateGroup(group.toEntity())

    override suspend fun deleteGroup(id: Long) =
        groupDao.deleteGroup(id)

    override fun getMembersByGroup(groupId: Long): Flow<List<SplitterMember>> =
        memberDao.getMembersByGroup(groupId).map { list -> list.map { it.toDomain() } }

    override fun getMemberById(id: Long): Flow<SplitterMember?> =
        memberDao.getMemberById(id).map { it?.toDomain() }

    override suspend fun insertMember(member: SplitterMember): Long =
        memberDao.insertMember(member.toEntity())

    override suspend fun updateMember(member: SplitterMember) =
        memberDao.updateMember(member.toEntity())

    override suspend fun deleteMember(id: Long) =
        memberDao.deleteMember(id)

    override fun getExpensesByGroup(groupId: Long): Flow<List<SplitterExpense>> =
        expenseDao.getExpensesByGroup(groupId).map { list -> list.map { it.toDomain() } }

    override fun getExpenseById(id: Long): Flow<SplitterExpense?> =
        expenseDao.getExpenseById(id).map { it?.toDomain() }

    override suspend fun insertExpense(expense: SplitterExpense): Long =
        expenseDao.insertExpense(expense.toEntity())

    override suspend fun updateExpense(expense: SplitterExpense) =
        expenseDao.updateExpense(expense.toEntity())

    override suspend fun deleteExpense(id: Long) =
        expenseDao.deleteExpense(id)

    override fun getSplitsByExpense(expenseId: Long): Flow<List<SplitterExpenseSplit>> =
        splitDao.getSplitsByExpense(expenseId).map { list -> list.map { it.toDomain() } }

    override suspend fun insertSplits(splits: List<SplitterExpenseSplit>) =
        splitDao.insertSplits(splits.map { it.toEntity() })

    override suspend fun deleteSplitsByExpense(expenseId: Long) =
        splitDao.deleteSplitsByExpense(expenseId)
}
