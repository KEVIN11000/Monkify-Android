package com.k11000.monkify.di

import com.k11000.monkify.data.repository.MockTrackRepository
import com.k11000.monkify.domain.repository.TrackRepository
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
        mockTrackRepository: MockTrackRepository
    ): TrackRepository
}
