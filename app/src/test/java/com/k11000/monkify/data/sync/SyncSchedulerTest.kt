package com.k11000.monkify.data.sync

import com.k11000.monkify.data.repository.SettingsRepository
import com.k11000.monkify.domain.model.SyncStatus
import com.k11000.monkify.domain.repository.SyncScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncSchedulerTest {

    private class FakeSyncScheduler : SyncScheduler {
        private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
        override val syncStatus: Flow<SyncStatus> = _syncStatus.asStateFlow()

        private val _lastSyncTimestamp = MutableStateFlow(0L)
        override val lastSyncTimestamp: Flow<Long> = _lastSyncTimestamp.asStateFlow()

        var periodicScheduled = false
        var immediateScheduled = false
        var cancelled = false

        override fun schedulePeriodicSync() {
            periodicScheduled = true
        }

        override fun scheduleImmediateSync() {
            immediateScheduled = true
            _syncStatus.value = SyncStatus.SYNCING
        }

        override fun cancelSync() {
            cancelled = true
            _syncStatus.value = SyncStatus.IDLE
        }

        fun completeSync(success: Boolean, timestamp: Long) {
            _syncStatus.value = if (success) SyncStatus.SUCCESS else SyncStatus.ERROR
            if (success) {
                _lastSyncTimestamp.value = timestamp
            }
        }
    }

    @Test
    fun sync_status_enum_values_are_distinct() {
        assertEquals(4, SyncStatus.values().size)
        assertTrue(SyncStatus.values().contains(SyncStatus.IDLE))
        assertTrue(SyncStatus.values().contains(SyncStatus.SYNCING))
        assertTrue(SyncStatus.values().contains(SyncStatus.SUCCESS))
        assertTrue(SyncStatus.values().contains(SyncStatus.ERROR))
    }

    @Test
    fun sync_scheduler_tracks_schedule_and_completion_flow() = runTest {
        val scheduler = FakeSyncScheduler()

        assertEquals(SyncStatus.IDLE, scheduler.syncStatus.first())
        assertEquals(0L, scheduler.lastSyncTimestamp.first())

        scheduler.scheduleImmediateSync()
        assertTrue(scheduler.immediateScheduled)
        assertEquals(SyncStatus.SYNCING, scheduler.syncStatus.first())

        val testTimestamp = 1728950000000L
        scheduler.completeSync(success = true, timestamp = testTimestamp)

        assertEquals(SyncStatus.SUCCESS, scheduler.syncStatus.first())
        assertEquals(testTimestamp, scheduler.lastSyncTimestamp.first())
    }

    @Test
    fun sync_scheduler_handles_error_state_without_corrupting_timestamp() = runTest {
        val scheduler = FakeSyncScheduler()
        val initialTimestamp = 1728900000000L
        scheduler.completeSync(success = true, timestamp = initialTimestamp)
        assertEquals(initialTimestamp, scheduler.lastSyncTimestamp.first())

        scheduler.scheduleImmediateSync()
        assertEquals(SyncStatus.SYNCING, scheduler.syncStatus.first())

        scheduler.completeSync(success = false, timestamp = 1728999999999L)
        assertEquals(SyncStatus.ERROR, scheduler.syncStatus.first())
        // El timestamp previo exitoso no debe haberse sobrescrito por un fallo
        assertEquals(initialTimestamp, scheduler.lastSyncTimestamp.first())
    }

    @Test
    fun sync_scheduler_cancellation_resets_status() = runTest {
        val scheduler = FakeSyncScheduler()
        scheduler.scheduleImmediateSync()
        assertEquals(SyncStatus.SYNCING, scheduler.syncStatus.first())

        scheduler.cancelSync()
        assertTrue(scheduler.cancelled)
        assertEquals(SyncStatus.IDLE, scheduler.syncStatus.first())
    }

    @Test
    fun settings_repository_constants_align_with_sync_spec() {
        assertEquals("last_sync_timestamp", SettingsRepository.KEY_LAST_SYNC_TIMESTAMP.name)
        assertEquals(0L, SettingsRepository.DEFAULT_LAST_SYNC_TIMESTAMP)
        assertEquals("catalog_sync_work", CatalogSyncWorker.WORK_NAME)
        assertEquals("catalog_periodic_sync_work", CatalogSyncWorker.PERIODIC_WORK_NAME)
    }
}
