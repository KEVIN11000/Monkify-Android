package com.k11000.monkify.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.k11000.monkify.data.repository.SettingsRepository
import com.k11000.monkify.domain.model.SyncStatus
import com.k11000.monkify.domain.repository.SyncScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncSchedulerImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) : SyncScheduler {

    var workManagerProvider: (() -> WorkManager)? = null

    private val workManager: WorkManager?
        get() = try {
            workManagerProvider?.invoke() ?: WorkManager.getInstance(context)
        } catch (_: Throwable) {
            null
        }

    private val _manualSyncStatus = MutableStateFlow(SyncStatus.IDLE)

    override val syncStatus: Flow<SyncStatus> by lazy {
        val wm = workManager
        if (wm != null) {
            try {
                wm.getWorkInfosForUniqueWorkFlow(CatalogSyncWorker.WORK_NAME)
                    .combine(wm.getWorkInfosForUniqueWorkFlow(CatalogSyncWorker.PERIODIC_WORK_NAME)) { immediateInfos, periodicInfos ->
                        val allInfos = immediateInfos + periodicInfos
                        when {
                            allInfos.any { it.state == WorkInfo.State.RUNNING } -> SyncStatus.SYNCING
                            allInfos.any { it.state == WorkInfo.State.FAILED } -> SyncStatus.ERROR
                            allInfos.any { it.state == WorkInfo.State.SUCCEEDED } -> SyncStatus.SUCCESS
                            else -> SyncStatus.IDLE
                        }
                    }
            } catch (_: Throwable) {
                _manualSyncStatus.asStateFlow()
            }
        } else {
            _manualSyncStatus.asStateFlow()
        }
    }

    override val lastSyncTimestamp: Flow<Long> = settingsRepository.lastSyncTimestamp

    override fun schedulePeriodicSync() {
        val wm = workManager ?: return

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<CatalogSyncWorker>(
            6, TimeUnit.HOURS,
            1, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        wm.enqueueUniquePeriodicWork(
            CatalogSyncWorker.PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )
    }

    override fun scheduleImmediateSync() {
        val wm = workManager ?: run {
            _manualSyncStatus.value = SyncStatus.SYNCING
            return
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val immediateRequest = OneTimeWorkRequestBuilder<CatalogSyncWorker>()
            .setConstraints(constraints)
            .build()

        wm.enqueueUniqueWork(
            CatalogSyncWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            immediateRequest
        )
    }

    override fun cancelSync() {
        workManager?.cancelUniqueWork(CatalogSyncWorker.WORK_NAME)
        workManager?.cancelUniqueWork(CatalogSyncWorker.PERIODIC_WORK_NAME)
    }
}
