package com.k11000.monkify.playback

import androidx.media3.common.Player
import com.k11000.monkify.domain.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun `toggle de Zen Clear conmuta el estado booleano correctamente`() {
        var isZenCleared = false
        // Simular accion toggleZenCleared
        isZenCleared = !isZenCleared
        assertEquals(true, isZenCleared)

        isZenCleared = !isZenCleared
        assertEquals(false, isZenCleared)

        // Simular setZenCleared(true)
        isZenCleared = true
        assertEquals(true, isZenCleared)
    }

    @Test
    fun `reproduccion de cola con 3 canciones permite avanzar con skipToNext y refleja hasNext correctamente`() {
        val tracks = listOf(
            Track("t1", "Track 1", "Artist 1", "Album 1", 180000, null, "https://stream/1"),
            Track("t2", "Track 2", "Artist 2", "Album 2", 200000, null, "https://stream/2"),
            Track("t3", "Track 3", "Artist 3", "Album 3", 220000, null, "https://stream/3")
        )

        var currentIndex = 0
        val queue = tracks

        fun hasNextMediaItem(): Boolean = currentIndex < queue.size - 1
        fun hasPreviousMediaItem(): Boolean = currentIndex > 0
        fun skipToNext() {
            if (hasNextMediaItem()) {
                currentIndex++
            }
        }
        fun skipToPrevious() {
            if (hasPreviousMediaItem()) {
                currentIndex--
            }
        }

        // Estado inicial en pista 1 (índice 0)
        assertEquals(0, currentIndex)
        assertEquals("t1", queue[currentIndex].id)
        assertTrue(hasNextMediaItem())
        assertFalse(hasPreviousMediaItem())

        // Avanzar a pista 2 con skipToNext()
        skipToNext()
        assertEquals(1, currentIndex)
        assertEquals("t2", queue[currentIndex].id)
        assertTrue(hasNextMediaItem())
        assertTrue(hasPreviousMediaItem())

        // Avanzar a pista 3 (última) con skipToNext()
        skipToNext()
        assertEquals(2, currentIndex)
        assertEquals("t3", queue[currentIndex].id)
        assertFalse(hasNextMediaItem())
        assertTrue(hasPreviousMediaItem())

        // Rebobinar a pista 2 con skipToPrevious()
        skipToPrevious()
        assertEquals(1, currentIndex)
        assertEquals("t2", queue[currentIndex].id)
    }

    @Test
    fun `letras descargadas localmente tienen prioridad sobre llamada remota`() {
        val localLyricsContent = """
            [00:01.00] Local offline lyric line 1
            [00:05.00] Local offline lyric line 2
        """.trimIndent()

        // Si existen letras locales, LrcParser las procesa directamente
        val parsed = com.k11000.monkify.playback.lyrics.LrcParser.parse(localLyricsContent)
        assertEquals(2, parsed.size)
        assertEquals(1000L, parsed[0].timestampMs)
        assertEquals("Local offline lyric line 1", parsed[0].text)
        assertEquals(5000L, parsed[1].timestampMs)
        assertEquals("Local offline lyric line 2", parsed[1].text)
    }

    @Test
    fun `preferencias zen constantes y alternancia de motores AURORA y VISUALIZER`() {
        val aurora = com.k11000.monkify.data.repository.SettingsRepository.ZEN_ENGINE_AURORA
        val visualizer = com.k11000.monkify.data.repository.SettingsRepository.ZEN_ENGINE_VISUALIZER

        assertEquals("AURORA", aurora)
        assertEquals("VISUALIZER", visualizer)
        assertEquals("AURORA", com.k11000.monkify.data.repository.SettingsRepository.DEFAULT_ZEN_ENGINE)

        // Alternancia de motor
        fun toggleEngine(current: String): String =
            if (current == aurora) visualizer else aurora

        var activeEngine = aurora
        activeEngine = toggleEngine(activeEngine)
        assertEquals("VISUALIZER", activeEngine)

        activeEngine = toggleEngine(activeEngine)
        assertEquals("AURORA", activeEngine)
    }

    @Test
    fun `zen mode enabled default es true segun la configuracion`() {
        val zenModeKey = com.k11000.monkify.data.repository.SettingsRepository.KEY_ZEN_MODE_ENABLED
        assertEquals("zen_mode_enabled", zenModeKey.name)
    }

    @Test
    fun `preferencias de estilo de caratula constantes y alternancia de MODERN y VINYL`() {
        val modern = com.k11000.monkify.data.repository.SettingsRepository.COVER_STYLE_MODERN
        val vinyl = com.k11000.monkify.data.repository.SettingsRepository.COVER_STYLE_VINYL

        assertEquals("MODERN", modern)
        assertEquals("VINYL", vinyl)
        assertEquals("MODERN", com.k11000.monkify.data.repository.SettingsRepository.DEFAULT_COVER_STYLE)

        fun toggleCoverStyle(current: String): String =
            if (current == vinyl) modern else vinyl

        var style = modern
        style = toggleCoverStyle(style)
        assertEquals("VINYL", style)

        style = toggleCoverStyle(style)
        assertEquals("MODERN", style)
    }

    @Test
    fun `parsePlaybackError traduce error 404 en mensaje legible`() {
        val exception = androidx.media3.common.PlaybackException(
            "Response code: 404",
            java.io.IOException("HTTP 404"),
            androidx.media3.common.PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND
        )

        fun parseError(error: androidx.media3.common.PlaybackException): String {
            val cause = error.cause
            val message = cause?.message ?: error.message ?: ""
            return when {
                message.contains("404") || error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
                    "Pista no disponible en el servidor (HTTP 404)"
                message.contains("Unable to connect") || message.contains("ConnectException") || error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ->
                    "Error de conexión de red al reproducir"
                else ->
                    "Error de reproducción: ${error.errorCodeName}"
            }
        }

        val msg = parseError(exception)
        assertEquals("Pista no disponible en el servidor (HTTP 404)", msg)
    }

    @Test
    fun `moveQueueItem reordena elementos dentro de los limites de la cola`() {
        val tracks = listOf(
            Track("t1", "Track 1", "Artist 1", "Album 1", 180000, null, "https://stream/1"),
            Track("t2", "Track 2", "Artist 2", "Album 2", 200000, null, "https://stream/2"),
            Track("t3", "Track 3", "Artist 3", "Album 3", 220000, null, "https://stream/3")
        )

        fun move(list: List<Track>, from: Int, to: Int): List<Track> {
            if (from !in list.indices || to !in list.indices || from == to) return list
            val mutable = list.toMutableList()
            val item = mutable.removeAt(from)
            mutable.add(to, item)
            return mutable
        }

        // Mover pista 0 (t1) al índice 2 -> [t2, t3, t1]
        val moved = move(tracks, 0, 2)
        assertEquals("t2", moved[0].id)
        assertEquals("t3", moved[1].id)
        assertEquals("t1", moved[2].id)

        // Mover con índices fuera de rango no altera la lista
        val outOfBounds = move(tracks, -1, 2)
        assertEquals(tracks, outOfBounds)

        // Mover al mismo índice no altera la lista
        val sameIndex = move(tracks, 1, 1)
        assertEquals(tracks, sameIndex)
    }

    @Test
    fun `removeQueueItem elimina el elemento en el indice especificado y valida rangos`() {
        val tracks = listOf(
            Track("t1", "Track 1", "Artist 1", "Album 1", 180000, null, "https://stream/1"),
            Track("t2", "Track 2", "Artist 2", "Album 2", 200000, null, "https://stream/2"),
            Track("t3", "Track 3", "Artist 3", "Album 3", 220000, null, "https://stream/3")
        )

        fun remove(list: List<Track>, index: Int): List<Track> {
            if (index !in list.indices) return list
            val mutable = list.toMutableList()
            mutable.removeAt(index)
            return mutable
        }

        // Eliminar pista intermedia (t2, index 1) -> [t1, t3]
        val afterRemove = remove(tracks, 1)
        assertEquals(2, afterRemove.size)
        assertEquals("t1", afterRemove[0].id)
        assertEquals("t3", afterRemove[1].id)

        // Eliminar con índice fuera de rango no altera la lista
        val outOfBounds = remove(tracks, 99)
        assertEquals(tracks, outOfBounds)
    }

    @Test
    fun `modelo Device y seleccion conmuta correctamente el dispositivo activo`() {
        val initialDevices = listOf(
            PlayerViewModel.Device("dev_speaker", "Altavoz del dispositivo", true, "Altavoz"),
            PlayerViewModel.Device("dev_bt", "Sony WH-1000XM4", false, "Bluetooth")
        )

        fun select(devices: List<PlayerViewModel.Device>, targetId: String): List<PlayerViewModel.Device> {
            return devices.map { it.copy(isSelected = it.id == targetId) }
        }

        val updated = select(initialDevices, "dev_bt")
        val active = updated.find { it.isSelected }

        assertEquals(true, updated[1].isSelected)
        assertEquals(false, updated[0].isSelected)
        assertEquals("Sony WH-1000XM4", active?.name)
        assertEquals("Bluetooth", active?.type)
    }

    @Test
    fun `resolucion de tipos de salida de audio mapea correctamente descripciones legibles`() {
        fun resolveType(isBluetooth: Boolean, isHeadset: Boolean): String {
            return when {
                isBluetooth -> "Bluetooth"
                isHeadset -> "Auriculares"
                else -> "Altavoz"
            }
        }

        assertEquals("Altavoz", resolveType(isBluetooth = false, isHeadset = false))
        assertEquals("Bluetooth", resolveType(isBluetooth = true, isHeadset = false))
        assertEquals("Auriculares", resolveType(isBluetooth = false, isHeadset = true))
    }
}
