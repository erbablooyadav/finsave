package com.finsave.domain.usecase.sync

interface SyncManager {
    fun startPeriodicSync()
    fun stopSync()
    fun triggerManualSync(sinceTimestamp: Long)
}
