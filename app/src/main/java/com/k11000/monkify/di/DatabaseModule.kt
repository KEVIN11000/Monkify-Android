package com.k11000.monkify.di

import android.content.Context
import androidx.room.Room
import com.k11000.monkify.data.local.MonkifyDatabase
import com.k11000.monkify.data.local.dao.TrackDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideMonkifyDatabase(@ApplicationContext context: Context): MonkifyDatabase {
        return Room.databaseBuilder(
            context,
            MonkifyDatabase::class.java,
            MonkifyDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration(true).build()
    }

    @Provides
    @Singleton
    fun provideTrackDao(database: MonkifyDatabase): TrackDao {
        return database.trackDao()
    }
}
