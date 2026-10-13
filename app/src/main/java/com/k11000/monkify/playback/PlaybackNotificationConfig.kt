package com.k11000.monkify.playback

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import com.k11000.monkify.R

/**
 * Configuración y proveedor de MediaNotification para la barra de estado y pantalla de bloqueo (Tarea 10).
 */
object PlaybackNotificationConfig {

    const val CHANNEL_ID = "monkify_playback_channel"
    const val NOTIFICATION_ID = 1001

    /**
     * Asegura la creación del NotificationChannel en Android 8.0+ (API 26+)
     * con importancia IMPORTANCE_LOW para evitar alertas audibles en cada cambio de pista.
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.playback_notification_channel_name)
            val description = context.getString(R.string.playback_notification_channel_desc)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                this.description = description
                setShowBadge(false)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    /**
     * Crea el MediaNotification.Provider usando DefaultMediaNotificationProvider personalizado.
     */
    @OptIn(UnstableApi::class)
    fun createNotificationProvider(context: Context): MediaNotification.Provider {
        return DefaultMediaNotificationProvider.Builder(context)
            .setChannelId(CHANNEL_ID)
            .setChannelName(R.string.playback_notification_channel_name)
            .build()
    }
}
