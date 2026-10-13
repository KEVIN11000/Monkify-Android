package com.k11000.monkify.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.k11000.monkify.playback.PlaybackCache
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>
) {

    val cacheLimitMb: Flow<Long> = dataStore.data.map { preferences ->
        preferences[KEY_CACHE_LIMIT_MB] ?: DEFAULT_CACHE_LIMIT_MB
    }

    val dynamicColor: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_DYNAMIC_COLOR] ?: false
    }

    suspend fun setCacheLimitMb(mb: Long) {
        dataStore.edit { preferences ->
            preferences[KEY_CACHE_LIMIT_MB] = mb
        }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_DYNAMIC_COLOR] = enabled
        }
    }

    suspend fun getCacheUsedBytes(): Long = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, PlaybackCache.DIRECTORY_NAME)
        if (!cacheDir.exists()) return@withContext 0L
        calculateDirectorySize(cacheDir)
    }

    suspend fun clearMediaCache(): Boolean = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, PlaybackCache.DIRECTORY_NAME)
        if (!cacheDir.exists()) return@withContext true
        cacheDir.deleteRecursively().also {
            cacheDir.mkdirs()
        }
    }

    private fun calculateDirectorySize(dir: File): Long {
        var size = 0L
        val files = dir.listFiles() ?: return 0L
        for (file in files) {
            size += if (file.isDirectory) calculateDirectorySize(file) else file.length()
        }
        return size
    }

    companion object {
        val KEY_CACHE_LIMIT_MB = longPreferencesKey("cache_limit_mb")
        val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        const val DEFAULT_CACHE_LIMIT_MB: Long = 1024L // 1 GB
    }
}
