package com.k11000.monkify.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.k11000.monkify.BuildConfig
import com.k11000.monkify.data.remote.api.MonkifyApi
import com.k11000.monkify.data.remote.dto.toDomain
import com.k11000.monkify.data.repository.LocalTrackRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.io.IOException

class CatalogSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    var injectedApi: MonkifyApi? = null
    var injectedLocalRepo: LocalTrackRepository? = null
    var injectedSettingsRepo: com.k11000.monkify.data.repository.SettingsRepository? = null

    constructor(
        appContext: Context,
        workerParams: WorkerParameters,
        api: MonkifyApi?,
        localRepo: LocalTrackRepository?
    ) : this(appContext, workerParams) {
        this.injectedApi = api
        this.injectedLocalRepo = localRepo
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface CatalogSyncWorkerEntryPoint {
        fun monkifyApi(): MonkifyApi
        fun localTrackRepository(): LocalTrackRepository
        fun settingsRepository(): com.k11000.monkify.data.repository.SettingsRepository
    }

    override suspend fun doWork(): Result {
        return try {
            val api = injectedApi ?: run {
                val entryPoint = EntryPointAccessors.fromApplication(
                    applicationContext,
                    CatalogSyncWorkerEntryPoint::class.java
                )
                entryPoint.monkifyApi()
            }
            val localRepo = injectedLocalRepo ?: run {
                val entryPoint = EntryPointAccessors.fromApplication(
                    applicationContext,
                    CatalogSyncWorkerEntryPoint::class.java
                )
                entryPoint.localTrackRepository()
            }

            val remoteTrackDtos = api.getTracks(limit = 100, offset = 0)
            val remoteTracks = remoteTrackDtos.map { it.toDomain(BuildConfig.BASE_URL) }

            localRepo.syncRemoteCatalog(remoteTracks)

            try {
                val settingsRepo = injectedSettingsRepo ?: run {
                    val entryPoint = EntryPointAccessors.fromApplication(
                        applicationContext,
                        CatalogSyncWorkerEntryPoint::class.java
                    )
                    entryPoint.settingsRepository()
                }
                settingsRepo.setLastSyncTimestamp(System.currentTimeMillis())
            } catch (_: Throwable) {
                // No bloquear el éxito del worker si falla el guardado de la marca temporal
            }

            Result.success()
        } catch (_: IOException) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        } catch (_: Exception) {
            Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "catalog_sync_work"
        const val PERIODIC_WORK_NAME = "catalog_periodic_sync_work"
    }
}
