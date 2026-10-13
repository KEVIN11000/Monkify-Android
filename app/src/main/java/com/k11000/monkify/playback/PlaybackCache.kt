package com.k11000.monkify.playback

import androidx.media3.datasource.cache.CacheEvictor
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor

/** Política de la caché de audio en disco (Tarea 9): LRU con tope de tamaño. */
object PlaybackCache {

    /** Directorio (dentro de `cacheDir`) de la caché automática de pistas escuchadas. */
    const val DIRECTORY_NAME = "media_cache"

    /** Tope de la caché LRU: 1 GiB. Al superarlo se expulsan las pistas menos recientes. */
    const val MAX_CACHE_BYTES: Long = 1024L * 1024L * 1024L

    fun newEvictor(maxBytes: Long = MAX_CACHE_BYTES): CacheEvictor =
        LeastRecentlyUsedCacheEvictor(maxBytes)
}
