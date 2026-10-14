package com.k11000.monkify.playback

import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerControlsTest {

    private fun nextRepeatMode(currentMode: Int): Int = when (currentMode) {
        Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
        Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
        else -> Player.REPEAT_MODE_OFF
    }

    private fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    @Test
    fun `ciclo de repeticion avanza correctamente por los 3 estados de Media3`() {
        var mode = Player.REPEAT_MODE_OFF
        mode = nextRepeatMode(mode)
        assertEquals(Player.REPEAT_MODE_ALL, mode)

        mode = nextRepeatMode(mode)
        assertEquals(Player.REPEAT_MODE_ONE, mode)

        mode = nextRepeatMode(mode)
        assertEquals(Player.REPEAT_MODE_OFF, mode)
    }

    @Test
    fun `formateador de tiempo calcula minutos y segundos correctamente`() {
        assertEquals("0:00", formatTime(0L))
        assertEquals("1:15", formatTime(75000L))
        assertEquals("4:02", formatTime(242000L))
    }
}
