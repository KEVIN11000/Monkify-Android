package com.k11000.monkify.playback

import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackCacheTest {

    @Test
    fun `el evictor por defecto es LRU`() {
        assertTrue(PlaybackCache.newEvictor() is LeastRecentlyUsedCacheEvictor)
    }

    @Test
    fun `el tope por defecto es 1 GiB`() {
        assertEquals(1_073_741_824L, PlaybackCache.MAX_CACHE_BYTES)
    }

    @Test
    fun `se puede construir un evictor con tope personalizado`() {
        assertTrue(PlaybackCache.newEvictor(maxBytes = 1_000L) is LeastRecentlyUsedCacheEvictor)
    }
}
