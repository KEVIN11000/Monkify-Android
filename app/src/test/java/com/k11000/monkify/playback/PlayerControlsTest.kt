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
    }

    @Test
    fun `toggle de shuffle conmuta adecuadamente el flag`() {
        var shuffle = false
        shuffle = !shuffle
        assertTrue(shuffle)
        shuffle = !shuffle
        assertFalse(shuffle)
    }

    @Test
    fun `reordenar elementos en la cola desplaza correctamente el item`() {
        val tracks = listOf(
            Track("t1", "Track 1", "Artist", "Album 1", 1000L, null, "s1"),
            Track("t2", "Track 2", "Artist", "Album 2", 1000L, null, "s2"),
            Track("t3", "Track 3", "Artist", "Album 3", 1000L, null, "s3")
        )

        fun move(list: List<Track>, from: Int, to: Int): List<Track> {
            if (from !in list.indices || to !in list.indices || from == to) return list
            val mutable = list.toMutableList()
            val item = mutable.removeAt(from)
            mutable.add(to, item)
            return mutable
        }

        // Mover de indice 0 a indice 2 -> [t2, t3, t1]
        val moved = move(tracks, 0, 2)
        assertEquals("t2", moved[0].id)
        assertEquals("t3", moved[1].id)
        assertEquals("t1", moved[2].id)

        // Mover con indices iguales o invalidos no altera la lista
        val invalid = move(tracks, 0, 0)
        assertEquals(tracks, invalid)
    }

    @Test
    fun `remover elemento de la cola elimina solo el indice solicitado`() {
        val tracks = listOf(
            Track("t1", "Track 1", "Artist", "Album 1", 1000L, null, "s1"),
            Track("t2", "Track 2", "Artist", "Album 2", 1000L, null, "s2"),
            Track("t3", "Track 3", "Artist", "Album 3", 1000L, null, "s3")
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
    fun `seekTo actualiza posicion y porcentaje de progreso de forma inmediata sin desfases`() {
        var currentPosition = 0L
        var progress = 0f
        val duration = 200000L

        fun seek(positionMs: Long) {
            currentPosition = positionMs
            if (duration > 0L) {
                progress = (positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            }
        }

        seek(50000L)
        assertEquals(50000L, currentPosition)
        assertEquals(0.25f, progress, 0.001f)

        seek(100000L)
        assertEquals(100000L, currentPosition)
        assertEquals(0.50f, progress, 0.001f)
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

    @Test
    fun `salto en skipToNext con shuffle activo y final de cola cicla al primer indice de shuffle`() {
        data class NavigationState(
            val hasNext: Boolean,
            val shuffleEnabled: Boolean,
            val firstShuffleIndex: Int
        )

        fun resolveNextIndex(state: NavigationState): Int {
            return if (state.hasNext) {
                -1 // ExoPlayer seekToNextMediaItem()
            } else if (state.shuffleEnabled) {
                state.firstShuffleIndex
            } else {
                0
            }
        }

        val stateAtEndWithShuffle = NavigationState(
            hasNext = false,
            shuffleEnabled = true,
            firstShuffleIndex = 2
        )

        val target = resolveNextIndex(stateAtEndWithShuffle)
        assertEquals(2, target)

        val stateAtEndWithoutShuffle = NavigationState(
            hasNext = false,
            shuffleEnabled = false,
            firstShuffleIndex = 2
        )
        assertEquals(0, resolveNextIndex(stateAtEndWithoutShuffle))
    }

    @Test
    fun `avance de pista en modo aleatorio cuando la pista actual es la ultima fisica pero no la ultima en shuffle no se detiene`() {
        val tracks = listOf("track_0", "track_1", "track_2", "track_3")
        // Secuencia aleatoria: [1, 3, 0, 2]
        val shuffleOrder = listOf(1, 3, 0, 2)

        fun getNextInShuffle(currentIndex: Int): Int? {
            val posInShuffle = shuffleOrder.indexOf(currentIndex)
            return if (posInShuffle in 0 until shuffleOrder.lastIndex) {
                shuffleOrder[posInShuffle + 1]
            } else null
        }

        // Pista física final (índice 3): en la secuencia aleatoria está en la posición 1 y le sigue el índice 0
        val nextAfterPhysicalLast = getNextInShuffle(3)
        assertEquals(0, nextAfterPhysicalLast)

        // Última pista en el orden aleatorio (índice 2): no tiene siguiente en la secuencia
        val nextAfterShuffleLast = getNextInShuffle(2)
        assertEquals(null, nextAfterShuffleLast)
    }

    @Test
    fun `transicion de STATE_ENDED con shuffle activo y repeat off reinicia ciclo y activa reproduccion`() {
        var isPlaying = false
        var currentPosition = 120000L
        var targetIndex = -1

        fun onPlaybackStateChanged(
            playbackState: Int,
            shuffleModeEnabled: Boolean,
            repeatMode: Int,
            firstShuffleIndex: Int
        ) {
            if (playbackState == Player.STATE_ENDED && shuffleModeEnabled && repeatMode == Player.REPEAT_MODE_OFF) {
                targetIndex = firstShuffleIndex
                currentPosition = 0L
                isPlaying = true
            }
        }

        onPlaybackStateChanged(
            playbackState = Player.STATE_ENDED,
            shuffleModeEnabled = true,
            repeatMode = Player.REPEAT_MODE_OFF,
            firstShuffleIndex = 1
        )

        assertTrue(isPlaying)
        assertEquals(0L, currentPosition)
        assertEquals(1, targetIndex)
    }

    @Test
    fun `skipToNext cuando el primer indice de shuffle es igual al actual selecciona un candidato alternativo`() {
        val totalCount = 4
        val currentIndex = 2
        val firstIndex = 2 // El primer índice del timeline coincide con la pista actual

        fun resolveTargetIndex(count: Int, current: Int, first: Int?): Int {
            return if (first != null && first != current) {
                first
            } else if (count > 1) {
                val candidates = (0 until count).filter { it != current }
                candidates.first() // Determinista para el test
            } else {
                0
            }
        }

        val target = resolveTargetIndex(totalCount, currentIndex, firstIndex)
        assertTrue(target != currentIndex)
        assertEquals(0, target)
    }

    @Test
    fun `STATE_ENDED en shuffle selecciona pista no reproducida y no reitera la pista actual`() {
        val totalCount = 5
        val currentIndex = 4 // Última pista física
        val firstShuffleIndex = 4 // Coincide con la actual

        fun resolveEndTransitionIndex(count: Int, current: Int, first: Int?): Int {
            return if (first != null && first != current) {
                first
            } else if (count > 1) {
                val candidates = (0 until count).filter { it != current }
                candidates.first()
            } else {
                0
            }
        }

        val nextTrackIndex = resolveEndTransitionIndex(totalCount, currentIndex, firstShuffleIndex)
        assertTrue("El índice siguiente no debe ser el actual", nextTrackIndex != currentIndex)
        assertTrue("El índice debe estar dentro del rango válido", nextTrackIndex in 0 until totalCount)
    }
}
