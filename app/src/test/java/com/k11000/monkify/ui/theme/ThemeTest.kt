package com.k11000.monkify.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ThemeTest {

    @Test
    fun `el color primario de Monkify es azul electrico y no verde`() {
        assertEquals(Color(0xFF2979FF), MonkifyPrimary)
        assertEquals(MonkifyElectricBlue, MonkifyPrimary)
        assertNotEquals(Color(0xFF1DB954), MonkifyPrimary)
    }

    @Test
    fun `el fondo es modo oscuro puro (#121212) y superficie (#1E1E1E)`() {
        assertEquals(Color(0xFF121212), MonkifyBackground)
        assertEquals(Color(0xFF1E1E1E), MonkifySurface)
    }

    @Test
    fun `el esquema de color asigna correctamente los acentos y superficies`() {
        assertEquals(MonkifyElectricBlue, MonkifyColorScheme.primary)
        assertEquals(MonkifyBackground, MonkifyColorScheme.background)
        assertEquals(MonkifySurface, MonkifyColorScheme.surface)
        assertEquals(MonkifyDarkGray, MonkifyColorScheme.surfaceVariant)
        assertEquals(Color.White, MonkifyColorScheme.onPrimary)
    }
}
