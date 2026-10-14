package com.k11000.monkify.playback.lyrics

data class LyricLine(
    val timestampMs: Long,
    val text: String
)

object LrcParser {
    private val TIMESTAMP_REGEX = Regex("""\[(\d{2}):(\d{2})(?:[.:](\d{2,3}))?\]""")

    /**
     * Parsea contenido en formato LRC (con marcas de tiempo [mm:ss.xx])
     * y retorna una lista ordenada cronológicamente de líneas de letra.
     */
    fun parse(lrcContent: String): List<LyricLine> {
        val result = mutableListOf<LyricLine>()

        for (rawLine in lrcContent.lineSequence()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            val matches = TIMESTAMP_REGEX.findAll(line).toList()
            if (matches.isEmpty()) continue

            val lastMatch = matches.last()
            val text = line.substring(lastMatch.range.last + 1).trim()

            for (match in matches) {
                val min = match.groupValues[1].toLongOrNull() ?: 0L
                val sec = match.groupValues[2].toLongOrNull() ?: 0L
                val fracStr = match.groupValues[3]
                val frac = if (fracStr.isNotEmpty()) {
                    fracStr.padEnd(3, '0').take(3).toLongOrNull() ?: 0L
                } else {
                    0L
                }
                val timestampMs = (min * 60 + sec) * 1000 + frac
                result.add(LyricLine(timestampMs, text))
            }
        }

        return result.sortedBy { it.timestampMs }
    }

    /**
     * Encuentra el índice de la línea que debe estar activa y resaltada
     * para la posición actual de reproducción en milisegundos.
     * Retorna -1 si aún no ha comenzado la primera línea lírica.
     */
    fun getCurrentLineIndex(lyrics: List<LyricLine>, currentPositionMs: Long): Int {
        if (lyrics.isEmpty()) return -1
        var activeIndex = -1
        for (i in lyrics.indices) {
            if (lyrics[i].timestampMs <= currentPositionMs) {
                activeIndex = i
            } else {
                break
            }
        }
        return activeIndex
    }
}
