package com.k11000.monkify.data

import com.k11000.monkify.data.repository.SettingsRepository
import com.k11000.monkify.ui.navigation.Destinations
import com.k11000.monkify.ui.screens.viewmodel.SettingsUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class SettingsTest {

    @Test
    fun `constantes y defaults de configuracion de cache son correctos`() {
        assertEquals(1024L, SettingsRepository.DEFAULT_CACHE_LIMIT_MB)
        assertEquals("cache_limit_mb", SettingsRepository.KEY_CACHE_LIMIT_MB.name)
        assertEquals("dynamic_color", SettingsRepository.KEY_DYNAMIC_COLOR.name)
        assertEquals("zen_mode_enabled", SettingsRepository.KEY_ZEN_MODE_ENABLED.name)
        assertEquals("zen_engine", SettingsRepository.KEY_ZEN_ENGINE.name)
        assertEquals("AURORA", SettingsRepository.DEFAULT_ZEN_ENGINE)
        assertEquals("AURORA", SettingsRepository.ZEN_ENGINE_AURORA)
        assertEquals("VISUALIZER", SettingsRepository.ZEN_ENGINE_VISUALIZER)
    }

    @Test
    fun `ruta de navegacion para ajustes esta configurada`() {
        assertEquals("settings", Destinations.SETTINGS)
    }

    @Test
    fun `estado inicial de SettingsUiState tiene defaults validos`() {
        val state = SettingsUiState()
        assertEquals(1024L, state.cacheLimitMb)
        assertEquals(0L, state.cacheUsedBytes)
        assertFalse(state.dynamicColor)
        assertFalse(state.isClearingCache)
        assertNull(state.message)
        assertEquals(true, state.zenModeEnabled)
        assertEquals("AURORA", state.zenEngine)
    }

    @Test
    fun `actualizacion de estado de SettingsUiState funciona adecuadamente`() {
        val state = SettingsUiState().copy(
            cacheLimitMb = 2048L,
            dynamicColor = true,
            zenModeEnabled = false,
            zenEngine = "VISUALIZER"
        )
        assertEquals(2048L, state.cacheLimitMb)
        assertEquals(true, state.dynamicColor)
        assertEquals(false, state.zenModeEnabled)
        assertEquals("VISUALIZER", state.zenEngine)
    }
}
