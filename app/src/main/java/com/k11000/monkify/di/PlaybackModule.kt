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

    /** Única instancia de SimpleCache por directorio (Media3 lo exige). */
    @UnstableApi
    @Provides
    @Singleton
    fun provideSimpleCache(@ApplicationContext context: Context): SimpleCache =
        SimpleCache(
            File(context.cacheDir, PlaybackCache.DIRECTORY_NAME),
            PlaybackCache.newEvictor(),
            StandaloneDatabaseProvider(context)
        )
}
