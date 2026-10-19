package com.k11000.monkify.playback.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcParserTest {

    private val sampleLrc = """
        [ti:Bohemian Rhapsody]
        [ar:Queen]
        [al:A Night at the Opera]
        
        [00:00.90] Is this the real life?
        [00:04.20] Is this just fantasy?
        [00:15.50] Caught in a landslide
        [00:20.100] No escape from reality
        [01:02.80] Open your eyes
    """.trimIndent()

    @Test
    fun `parse convierte texto LRC con marcas mm ss xx en lista de LyricLine ordenadas`() {
        val lyrics = LrcParser.parse(sampleLrc)

        assertEquals(5, lyrics.size)
        assertEquals(900L, lyrics[0].timestampMs)
        assertEquals("Is this the real life?", lyrics[0].text)

        assertEquals(4200L, lyrics[1].timestampMs)
        assertEquals("Is this just fantasy?", lyrics[1].text)

        assertEquals(15500L, lyrics[2].timestampMs)
        assertEquals("Caught in a landslide", lyrics[2].text)

        assertEquals(20100L, lyrics[3].timestampMs)
        assertEquals("No escape from reality", lyrics[3].text)

        assertEquals(62800L, lyrics[4].timestampMs)
        assertEquals("Open your eyes", lyrics[4].text)
    }

    @Test
    fun `parse interpreta marcas de 3 digitos de milisegundos correctamente`() {
        val lrc = "[00:10.125] Precise line"
        val lyrics = LrcParser.parse(lrc)

        assertEquals(1, lyrics.size)
        assertEquals(10125L, lyrics[0].timestampMs)
        assertEquals("Precise line", lyrics[0].text)
    }

    @Test
    fun `parse interpreta marcas sin fraccion decimal correctamente`() {
        val lrc = "[02:30] Line without fraction"
        val lyrics = LrcParser.parse(lrc)

        assertEquals(1, lyrics.size)
        assertEquals(150000L, lyrics[0].timestampMs)
        assertEquals("Line without fraction", lyrics[0].text)
    }

    @Test
    fun `parse ignora metadatos y encabezados de archivo LRC`() {
        val metadataOnly = """
            [ti:Test Title]
            [ar:Test Artist]
            [al:Test Album]
            [by:LRC Maker]
            [offset:+200]
        """.trimIndent()

        val lyrics = LrcParser.parse(metadataOnly)
        assertTrue(lyrics.isEmpty())
    }

    @Test
    fun `parse soporta multiples marcas de tiempo en el mismo verso`() {
        val lrc = "[00:10.00][00:30.00] Repeated chorus"
        val lyrics = LrcParser.parse(lrc)

        assertEquals(2, lyrics.size)
        assertEquals(10000L, lyrics[0].timestampMs)
        assertEquals("Repeated chorus", lyrics[0].text)
        assertEquals(30000L, lyrics[1].timestampMs)
        assertEquals("Repeated chorus", lyrics[1].text)
    }

    @Test
    fun `getCurrentLineIndex identifica correctamente la linea activa segun la posicion de reproduccion`() {
        val lyrics = LrcParser.parse(sampleLrc)

        // Antes de que empiece la primera línea (500 ms < 900 ms)
        assertEquals(-1, LrcParser.getCurrentLineIndex(lyrics, 500L))

        // Justo al inicio de la primera línea (900 ms)
        assertEquals(0, LrcParser.getCurrentLineIndex(lyrics, 900L))

        // Durante la primera línea (2000 ms)
        assertEquals(0, LrcParser.getCurrentLineIndex(lyrics, 2000L))

        // Durante la segunda línea (10000 ms)
        assertEquals(1, LrcParser.getCurrentLineIndex(lyrics, 10000L))

        // En la última línea (65000 ms)
        assertEquals(4, LrcParser.getCurrentLineIndex(lyrics, 65000L))
    }

    @Test
    fun `getCurrentLineIndex con lista vacia retorna menos uno`() {
        assertEquals(-1, LrcParser.getCurrentLineIndex(emptyList(), 10000L))
    }

    @Test
    fun `parse soporta texto plano estatico asignando timestampMs menos uno`() {
        val plainLyrics = """
            Is this the real life?
            Is this just fantasy?
            Caught in a landslide
            No escape from reality
        """.trimIndent()

        val lyrics = LrcParser.parse(plainLyrics)

        assertEquals(4, lyrics.size)
        assertTrue(lyrics.all { it.timestampMs == -1L })
        assertEquals("Is this the real life?", lyrics[0].text)
        assertEquals("No escape from reality", lyrics[3].text)
    }

    @Test
    fun `getCurrentLineIndex con letra estatica retorna menos uno en cualquier posicion`() {
        val plainLyrics = """
            Linea 1
            Linea 2
        """.trimIndent()
        val lyrics = LrcParser.parse(plainLyrics)

        assertEquals(-1, LrcParser.getCurrentLineIndex(lyrics, 0L))
        assertEquals(-1, LrcParser.getCurrentLineIndex(lyrics, 15000L))
    }

    @Test
    fun `seek tactil a una marca de tiempo ubica de inmediato la linea activa correspondiente`() {
        val lyrics = LrcParser.parse(sampleLrc)

        // Simulación: usuario pulsa en el verso "Caught in a landslide" (15500 ms)
        val selectedTimestamp = lyrics[2].timestampMs
        assertEquals(15500L, selectedTimestamp)

        // Al saltar el reproductor a esa marca exacta, el índice activo es 2
        val activeIndex = LrcParser.getCurrentLineIndex(lyrics, selectedTimestamp)
        assertEquals(2, activeIndex)
        assertEquals("Caught in a landslide", lyrics[activeIndex].text)
    }
}
