package com.k11000.monkify.ui

import com.k11000.monkify.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Test

class PipConfigTest {

    @Test
    fun `constantes de acciones PiP estan configuradas correctamente`() {
        assertEquals("com.k11000.monkify.PIP_PLAY_PAUSE", MainActivity.ACTION_PIP_PLAY_PAUSE)
        assertEquals("com.k11000.monkify.PIP_NEXT", MainActivity.ACTION_PIP_NEXT)
        assertEquals("com.k11000.monkify.PIP_PREV", MainActivity.ACTION_PIP_PREV)
    }
}
