package com.k11000.monkify.playback

/**
 * Representa el estado instantáneo normalizado del espectro de frecuencias de audio.
 * Diseñado para alimentar de forma desacoplada y eficiente a los visualizadores de Canvas.
 */
data class AudioSpectrumData(
    val bands: FloatArray,
    val isRealTime: Boolean,
    val timestamp: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AudioSpectrumData) return false
        return bands.contentEquals(other.bands) && isRealTime == other.isRealTime
    }

    override fun hashCode(): Int {
        var result = bands.contentHashCode()
        result = 31 * result + isRealTime.hashCode()
        return result
    }

    companion object {
        const val BAND_COUNT = 28

        val EMPTY = AudioSpectrumData(
            bands = FloatArray(BAND_COUNT) { 0.15f },
            isRealTime = false
        )
    }
}
