package com.k11000.monkify.playback

import android.media.audiofx.Visualizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Procesador de espectro de frecuencias de audio FFT en tiempo real con fallback procedural
 * para garantizar robustez absoluta (Zero-Crash) ante ausencia de permisos o hardware no soportado.
 */
class AudioSpectrumProcessor(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val _spectrumFlow = MutableStateFlow(AudioSpectrumData.EMPTY)
    val spectrumFlow: StateFlow<AudioSpectrumData> = _spectrumFlow.asStateFlow()

    private var visualizer: Visualizer? = null
    private var proceduralJob: Job? = null

    private var smoothingFactor: Float = 0.35f
    private val smoothedBands = FloatArray(AudioSpectrumData.BAND_COUNT) { 0.15f }

    @Volatile
    var isFallbackActive: Boolean = true
        private set

    /**
     * Vincula el procesador a un audioSessionId de Android Media3 / ExoPlayer.
     * Si el ID es inválido (<= 0) o el sistema deniega el acceso, conmuta silenciosamente
     * al generador procedural armónico.
     */
    fun attachAudioSession(audioSessionId: Int) {
        releaseVisualizer()

        if (audioSessionId <= 0) {
            startProceduralFallback()
            return
        }

        try {
            val captureSizeRange = Visualizer.getCaptureSizeRange()
            val captureSize = captureSizeRange[1].coerceAtMost(512)

            val viz = Visualizer(audioSessionId).apply {
                this.captureSize = captureSize
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            visualizer: Visualizer?,
                            waveform: ByteArray?,
                            samplingRate: Int
                        ) {
                            // No requerido para barras FFT
                        }

                        override fun onFftDataCapture(
                            visualizer: Visualizer?,
                            fft: ByteArray?,
                            samplingRate: Int
                        ) {
                            if (fft != null) {
                                processFftBytes(fft)
                            }
                        }
                    },
                    Visualizer.getMaxCaptureRate() / 2,
                    false,
                    true
                )
                enabled = true
            }

            visualizer = viz
            isFallbackActive = false
            stopProceduralFallback()
        } catch (_: SecurityException) {
            // Falta de permiso RECORD_AUDIO: fallback transparente
            startProceduralFallback()
        } catch (_: RuntimeException) {
            // Error de hardware/HAL en dispositivos específicos: fallback transparente
            startProceduralFallback()
        } catch (_: Exception) {
            startProceduralFallback()
        }
    }

    /**
     * Procesa los bytes crudos del array FFT de Android Visualizer y los agrupa en 28 bandas.
     */
    internal fun processFftBytes(fft: ByteArray) {
        val bandCount = AudioSpectrumData.BAND_COUNT
        val halfSize = (fft.size / 2).coerceAtLeast(1)
        val chunkSize = (halfSize / bandCount).coerceAtLeast(1)

        val rawBands = FloatArray(bandCount)

        for (i in 0 until bandCount) {
            var sumMagnitude = 0f
            val startIndex = i * chunkSize
            val endIndex = (startIndex + chunkSize).coerceAtMost(halfSize)

            for (j in startIndex until endIndex) {
                val realIndex = j * 2
                val imagIndex = j * 2 + 1
                if (imagIndex < fft.size) {
                    val r = fft[realIndex].toFloat()
                    val im = fft[imagIndex].toFloat()
                    val mag = hypot(r, im)
                    sumMagnitude += mag
                }
            }

            val count = (endIndex - startIndex).coerceAtLeast(1)
            val avgMag = sumMagnitude / count
            // Normalizar a rango [0.0, 1.0]
            val normalized = (avgMag / 128f).coerceIn(0.05f, 1.0f)
            rawBands[i] = normalized
        }

        applySmoothingAndEmit(rawBands, isRealTime = true)
    }

    private fun applySmoothingAndEmit(rawBands: FloatArray, isRealTime: Boolean) {
        val output = FloatArray(AudioSpectrumData.BAND_COUNT)
        for (i in 0 until AudioSpectrumData.BAND_COUNT) {
            smoothedBands[i] = (smoothingFactor * rawBands[i] + (1f - smoothingFactor) * smoothedBands[i])
                .coerceIn(0.05f, 1.0f)
            output[i] = smoothedBands[i]
        }
        _spectrumFlow.value = AudioSpectrumData(bands = output, isRealTime = isRealTime)
    }

    /**
     * Inicia el fallback procedural armónico cuando no hay captura física.
     */
    private fun startProceduralFallback() {
        isFallbackActive = true
        if (proceduralJob?.isActive == true) return

        proceduralJob = scope.launch {
            var phase = 0f
            while (isActive) {
                phase += 0.08f
                val proceduralBands = FloatArray(AudioSpectrumData.BAND_COUNT)
                for (i in 0 until AudioSpectrumData.BAND_COUNT) {
                    val harmonicPhase = phase + (i.toFloat() / AudioSpectrumData.BAND_COUNT) * 6.2831853f
                    val wave1 = sin(harmonicPhase)
                    val wave2 = cos(harmonicPhase * 2f)
                    val combinedWave = ((wave1 + wave2 * 0.5f) / 1.5f).coerceIn(-1f, 1f)
                    proceduralBands[i] = (abs(combinedWave) * 0.75f + 0.15f).coerceIn(0.05f, 1.0f)
                }
                applySmoothingAndEmit(proceduralBands, isRealTime = false)
                delay(33L) // ~30 fps para economía de batería
            }
        }
    }

    private fun stopProceduralFallback() {
        proceduralJob?.cancel()
        proceduralJob = null
    }

    fun setSmoothing(factor: Float) {
        smoothingFactor = factor.coerceIn(0.05f, 0.95f)
    }

    /**
     * Libera de forma idempotente todos los recursos nativos y de corrutinas.
     */
    fun release() {
        releaseVisualizer()
        stopProceduralFallback()
        _spectrumFlow.value = AudioSpectrumData.EMPTY
    }

    private fun releaseVisualizer() {
        try {
            visualizer?.enabled = false
            visualizer?.release()
        } catch (_: Exception) {
            // Ignorado por seguridad durante apagado
        } finally {
            visualizer = null
        }
    }
}
