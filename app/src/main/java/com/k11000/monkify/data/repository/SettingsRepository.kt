package com.k11000.monkify.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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

    val zenModeEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_ZEN_MODE_ENABLED] ?: true
    }

    val zenEngine: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_ZEN_ENGINE] ?: DEFAULT_ZEN_ENGINE
    }

    val coverStyle: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_COVER_STYLE] ?: DEFAULT_COVER_STYLE
    }

    val lockScreenEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_LOCK_SCREEN_ENABLED] ?: DEFAULT_LOCK_SCREEN_ENABLED
    }

    val lastSyncTimestamp: Flow<Long> = dataStore.data.map { preferences ->
        preferences[KEY_LAST_SYNC_TIMESTAMP] ?: DEFAULT_LAST_SYNC_TIMESTAMP
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

    suspend fun setZenModeEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_ZEN_MODE_ENABLED] = enabled
        }
    }

    suspend fun setZenEngine(engine: String) {
        dataStore.edit { preferences ->
            preferences[KEY_ZEN_ENGINE] = engine
        }
    }

    suspend fun setCoverStyle(style: String) {
        dataStore.edit { preferences ->
            preferences[KEY_COVER_STYLE] = style
        }
    }

    suspend fun setLockScreenEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_LOCK_SCREEN_ENABLED] = enabled
        }
    }

    suspend fun setLastSyncTimestamp(timestamp: Long) {
        dataStore.edit { preferences ->
            preferences[KEY_LAST_SYNC_TIMESTAMP] = timestamp
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
        val KEY_ZEN_MODE_ENABLED = booleanPreferencesKey("zen_mode_enabled")
        val KEY_ZEN_ENGINE = stringPreferencesKey("zen_engine")
        val KEY_COVER_STYLE = stringPreferencesKey("cover_style")
        val KEY_LOCK_SCREEN_ENABLED = booleanPreferencesKey("lock_screen_enabled")
        val KEY_LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")

        const val DEFAULT_CACHE_LIMIT_MB: Long = 1024L // 1 GB
        const val DEFAULT_ZEN_ENGINE = "AURORA"
        const val DEFAULT_COVER_STYLE = "MODERN"
        const val COVER_STYLE_MODERN = "MODERN"
        const val COVER_STYLE_VINYL = "VINYL"
        const val DEFAULT_LOCK_SCREEN_ENABLED = false
        const val DEFAULT_LAST_SYNC_TIMESTAMP: Long = 0L
        const val ZEN_ENGINE_AURORA = "AURORA"
        const val ZEN_ENGINE_VISUALIZER = "VISUALIZER"
        const val ZEN_ENGINE_FROGGY = "FROGGY"
        const val ZEN_ENGINE_COSMOS = "COSMOS"
        const val ZEN_ENGINE_NONE = "NONE"
    }
}
