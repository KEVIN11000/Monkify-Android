package com.k11000.monkify.di

import com.k11000.monkify.data.location.CoarseLocationDataSource
import com.k11000.monkify.data.location.LocationDataSource
import com.k11000.monkify.data.repository.DownloadRepositoryImpl
import com.k11000.monkify.data.repository.OfflineFirstTrackRepository
import com.k11000.monkify.data.repository.PlaylistRepositoryImpl
import com.k11000.monkify.data.repository.UpdateRepositoryImpl
import com.k11000.monkify.data.repository.WeatherRepositoryImpl
import com.k11000.monkify.domain.repository.DownloadRepository
import com.k11000.monkify.domain.repository.PlaylistRepository
import com.k11000.monkify.domain.repository.TrackRepository
import com.k11000.monkify.domain.repository.UpdateRepository
import com.k11000.monkify.domain.repository.WeatherRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTrackRepository(
        offlineFirstTrackRepository: OfflineFirstTrackRepository
    ): TrackRepository

    @Binds
    @Singleton
    abstract fun bindUpdateRepository(
        updateRepositoryImpl: UpdateRepositoryImpl
    ): UpdateRepository

    @Binds
    @Singleton
    abstract fun bindDownloadRepository(
        downloadRepositoryImpl: DownloadRepositoryImpl
    ): DownloadRepository

    @Binds
    @Singleton
    abstract fun bindPlaylistRepository(
        playlistRepositoryImpl: PlaylistRepositoryImpl
    ): PlaylistRepository

    @Binds
    @Singleton
    abstract fun bindSyncScheduler(
        syncSchedulerImpl: com.k11000.monkify.data.sync.SyncSchedulerImpl
    ): com.k11000.monkify.domain.repository.SyncScheduler

    @Binds
    @Singleton
    abstract fun bindWeatherRepository(
        weatherRepositoryImpl: WeatherRepositoryImpl
    ): WeatherRepository

    @Binds
    @Singleton
    abstract fun bindLocationDataSource(
        coarseLocationDataSource: CoarseLocationDataSource
    ): LocationDataSource
}
