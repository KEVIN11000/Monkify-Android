package com.k11000.monkify.di

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.SimpleCache
import com.k11000.monkify.playback.PlaybackCache
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlaybackModule {

    @UnstableApi
    @Provides
    @Singleton
    fun provideDatabaseProvider(@ApplicationContext context: Context): StandaloneDatabaseProvider =
        StandaloneDatabaseProvider(context)

    /** Única instancia de SimpleCache por directorio (Media3 lo exige). */
    @UnstableApi
    @Provides
    @Singleton
    fun provideSimpleCache(
        @ApplicationContext context: Context,
        databaseProvider: StandaloneDatabaseProvider
    ): SimpleCache =
        SimpleCache(
            File(context.cacheDir, PlaybackCache.DIRECTORY_NAME),
            PlaybackCache.newEvictor(),
            databaseProvider
        )

    @UnstableApi
    @Provides
    @Singleton
    fun provideDownloadManager(
        @ApplicationContext context: Context,
        databaseProvider: StandaloneDatabaseProvider,
        cache: SimpleCache
    ): androidx.media3.exoplayer.offline.DownloadManager {
        val upstreamFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
        return androidx.media3.exoplayer.offline.DownloadManager(
            context,
            databaseProvider,
            cache,
            upstreamFactory,
            java.util.concurrent.Executors.newFixedThreadPool(4)
        )
    }
}
