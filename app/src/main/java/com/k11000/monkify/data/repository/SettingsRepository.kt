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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>
) {

    @Volatile
    private var cachedServerToken: String? = null

    @Volatile
    private var activeMediaCache: androidx.media3.datasource.cache.Cache? = null

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun attachMediaCache(cache: androidx.media3.datasource.cache.Cache) {
        activeMediaCache = cache
    }

    val cacheLimitMb: Flow<Long> = dataStore.data.map { preferences ->
        preferences[KEY_CACHE_LIMIT_MB] ?: DEFAULT_CACHE_LIMIT_MB
    }

    /**
     * Obtiene el límite de caché configurado en bytes de forma inmediata al arranque
     * para inicializar el `LeastRecentlyUsedCacheEvictor` de Media3 con la preferencia real del usuario.
     */
    fun getCacheLimitBytesImmediate(): Long {
        val mb = try {
            runBlocking(Dispatchers.IO) {
                dataStore.data.first()[KEY_CACHE_LIMIT_MB] ?: DEFAULT_CACHE_LIMIT_MB
            }
        } catch (_: Exception) {
            DEFAULT_CACHE_LIMIT_MB
        }
        return PlaybackCache.mbToBytes(mb)
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

    val cosmosTheme: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_COSMOS_THEME] ?: DEFAULT_COSMOS_THEME
    }

    val froggyScenery: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_FROGGY_SCENERY] ?: DEFAULT_FROGGY_SCENERY
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

    val serverAccessToken: Flow<String> = dataStore.data.map { preferences ->
        (preferences[KEY_SERVER_ACCESS_TOKEN] ?: "").also { token ->
            cachedServerToken = token
        }
    }

    /**
     * Obtiene el token de acceso de manera inmediata y libre de condiciones de carrera al arranque.
     * Si el caché en memoria aún no fue inicializado, realiza una única lectura síncrona en IO.
     */
    fun getServerAccessTokenImmediate(): String {
        cachedServerToken?.let { return it }
        return try {
            runBlocking(Dispatchers.IO) {
                val token = dataStore.data.first()[KEY_SERVER_ACCESS_TOKEN]?.trim().orEmpty()
                cachedServerToken = token
                token
            }
        } catch (_: Exception) {
            cachedServerToken.orEmpty()
        }
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

    suspend fun setCosmosTheme(theme: String) {
        dataStore.edit { preferences ->
            preferences[KEY_COSMOS_THEME] = theme
        }
    }

    suspend fun setFroggyScenery(scenery: String) {
        dataStore.edit { preferences ->
            preferences[KEY_FROGGY_SCENERY] = scenery
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

    suspend fun setServerAccessToken(token: String) {
        val cleanToken = token.trim()
        cachedServerToken = cleanToken
        dataStore.edit { preferences ->
            preferences[KEY_SERVER_ACCESS_TOKEN] = cleanToken
        }
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    suspend fun getCacheUsedBytes(): Long = withContext(Dispatchers.IO) {
        activeMediaCache?.let { cache ->
            try {
                return@withContext cache.cacheSpace.coerceAtLeast(0L)
            } catch (_: Exception) {
                // Fallback a lectura de directorio si el índice aún no está listo
            }
        }
        val cacheDir = File(context.cacheDir, PlaybackCache.DIRECTORY_NAME)
        if (!cacheDir.exists()) return@withContext 0L
        calculateDirectorySize(cacheDir)
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    suspend fun clearMediaCache(): Boolean = withContext(Dispatchers.IO) {
        val cache = activeMediaCache
        if (cache != null) {
            return@withContext try {
                val keysSnapshot = cache.keys.toList()
                for (key in keysSnapshot) {
                    cache.removeResource(key)
                }
                true
            } catch (_: Exception) {
                false
            }
        }
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
        val KEY_COSMOS_THEME = stringPreferencesKey("cosmos_theme")
        val KEY_FROGGY_SCENERY = stringPreferencesKey("froggy_scenery")
        val KEY_COVER_STYLE = stringPreferencesKey("cover_style")
        val KEY_LOCK_SCREEN_ENABLED = booleanPreferencesKey("lock_screen_enabled")
        val KEY_LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
        val KEY_SERVER_ACCESS_TOKEN = stringPreferencesKey("server_access_token")

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

        const val DEFAULT_COSMOS_THEME = "DEEP_SPACE"

        const val DEFAULT_FROGGY_SCENERY = "AUTO"
        const val FROGGY_SCENERY_AUTO = "AUTO"
        const val FROGGY_SCENERY_FIELDS = "FIELDS"
        const val FROGGY_SCENERY_HILL = "HILL"
        const val FROGGY_SCENERY_MUSHROOM = "MUSHROOM"
    }
}
