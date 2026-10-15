package com.k11000.monkify.domain.repository

import com.k11000.monkify.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

interface SyncScheduler {
    fun schedulePeriodicSync()
    fun scheduleImmediateSync()
    fun cancelSync()
    val syncStatus: Flow<SyncStatus>
    val lastSyncTimestamp: Flow<Long>
}
