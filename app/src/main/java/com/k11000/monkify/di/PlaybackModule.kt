package com.k11000.monkify.di

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.SimpleCache
import com.k11000.monkify.data.repository.SettingsRepository
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
        databaseProvider: StandaloneDatabaseProvider,
        settingsRepository: SettingsRepository
    ): SimpleCache =
        SimpleCache(
            File(context.cacheDir, PlaybackCache.DIRECTORY_NAME),
            PlaybackCache.newEvictor(settingsRepository.getCacheLimitBytesImmediate()),
            databaseProvider
        ).also { cache ->
            settingsRepository.attachMediaCache(cache)
        }

    @UnstableApi
    @Provides
    @Singleton
    fun provideDownloadManager(
        @ApplicationContext context: Context,
        databaseProvider: StandaloneDatabaseProvider,
        cache: SimpleCache,
        settingsRepository: SettingsRepository
    ): androidx.media3.exoplayer.offline.DownloadManager {
        val upstreamFactory = createAuthenticatedHttpDataSourceFactory(settingsRepository)
        return androidx.media3.exoplayer.offline.DownloadManager(
            context,
            databaseProvider,
            cache,
            upstreamFactory,
            java.util.concurrent.Executors.newFixedThreadPool(4)
        )
    }

    // INVARIANT: Authenticate Media3 streaming via HTTP headers on trusted hosts without leaking token in URLs. Ref: SYSTEM_MAP #INV-N01, #INV-N02
    @UnstableApi
    fun createAuthenticatedHttpDataSourceFactory(
        settingsRepository: SettingsRepository
    ): androidx.media3.datasource.DataSource.Factory {
        val baseHttpFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
        return androidx.media3.datasource.ResolvingDataSource.Factory(baseHttpFactory) { dataSpec ->
            val host = dataSpec.uri.host.orEmpty()
            if (com.k11000.monkify.data.remote.api.AuthInterceptor.isTrustedHost(host)) {
                val token = settingsRepository.getServerAccessTokenImmediate().trim()
                if (token.isNotEmpty()) {
                    return@Factory dataSpec.withAdditionalHeaders(
                        mapOf(
                            "Authorization" to "Bearer $token",
                            "X-Monkify-Token" to token
                        )
                    )
                }
            }
            dataSpec
        }
    }
}
