package com.k11000.monkify.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PlaybackNotificationConfigTest {

    @Test
    fun `constantes de canal y notificacion estan configuradas correctamente`() {
        assertEquals("monkify_playback_channel", PlaybackNotificationConfig.CHANNEL_ID)
        assertEquals(1001, PlaybackNotificationConfig.NOTIFICATION_ID)
    }

    @Test
    fun `nombre de canal es consistente`() {
        assertNotNull(PlaybackNotificationConfig.CHANNEL_ID)
    }
}
