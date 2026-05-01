package com.finsave.app.workers

import com.finsave.core.common.work.BudgetCheckScheduler as BudgetCheckSchedulerContract
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerBudgetCheckScheduler @Inject constructor(
    private val delegate: BudgetCheckScheduler
) : BudgetCheckSchedulerContract {

    override fun scheduleBudgetCheck() {
        delegate.scheduleBudgetCheck()
    }
}
