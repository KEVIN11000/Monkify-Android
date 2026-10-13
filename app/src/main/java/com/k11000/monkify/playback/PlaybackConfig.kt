package com.k11000.monkify.playback

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C

/** Configuración de audio del reproductor, aislada para poder verificarla en tests JVM. */
object PlaybackConfig {

    /** Música (para que el sistema aplique ducking/pausa por focus y enrute correctamente). */
    fun audioAttributes(): AudioAttributes =
        AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

    /** El player gestiona el Audio Focus (llamadas, otras apps) automáticamente. */
    const val HANDLE_AUDIO_FOCUS = true

    /** Pausa al desconectar auriculares / Bluetooth. */
    const val HANDLE_BECOMING_NOISY = true

    /** Mantiene CPU y Wi-Fi activos mientras se hace streaming con la pantalla apagada. */
    const val WAKE_MODE = C.WAKE_MODE_NETWORK
}
