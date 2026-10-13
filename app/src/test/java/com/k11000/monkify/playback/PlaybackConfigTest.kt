package com.k11000.monkify.playback

import androidx.media3.common.C
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackConfigTest {

    @Test
    fun `atributos de audio son musica para reproduccion multimedia`() {
        val attrs = PlaybackConfig.audioAttributes()
        assertEquals(C.USAGE_MEDIA, attrs.usage)
        assertEquals(C.AUDIO_CONTENT_TYPE_MUSIC, attrs.contentType)
    }

    @Test
    fun `focus, becoming noisy y wake mode de red estan activos`() {
        assertTrue(PlaybackConfig.HANDLE_AUDIO_FOCUS)
        assertTrue(PlaybackConfig.HANDLE_BECOMING_NOISY)
        assertEquals(C.WAKE_MODE_NETWORK, PlaybackConfig.WAKE_MODE)
    }
}
