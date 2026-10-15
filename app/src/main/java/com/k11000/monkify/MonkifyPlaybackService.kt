package com.k11000.monkify

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import com.k11000.monkify.playback.PlaybackConfig
import com.k11000.monkify.playback.PlaybackNotificationConfig
import com.k11000.monkify.util.AppLogger
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import com.k11000.monkify.data.repository.SettingsRepository
import com.k11000.monkify.lockscreen.LockScreenActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@UnstableApi
@AndroidEntryPoint
class MonkifyPlaybackService : MediaSessionService() {

    @Inject
    lateinit var cache: SimpleCache

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private var mediaSession: MediaSession? = null
    private var isLockScreenPrefEnabled = false
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                val player = mediaSession?.player
                if (player?.isPlaying == true && isLockScreenPrefEnabled) {
                    val lockIntent = Intent(this@MonkifyPlaybackService, LockScreenActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    startActivity(lockIntent)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        PlaybackNotificationConfig.createNotificationChannel(this)
        setMediaNotificationProvider(PlaybackNotificationConfig.createNotificationProvider(this))

        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        registerReceiver(screenReceiver, filter)

        serviceScope.launch {
            settingsRepository.lockScreenEnabled.collect { enabled ->
                isLockScreenPrefEnabled = enabled
            }
        }

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        val defaultDataSourceFactory = DefaultDataSource.Factory(this, httpDataSourceFactory)

        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(defaultDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory))
            .setAudioAttributes(PlaybackConfig.audioAttributes(), PlaybackConfig.HANDLE_AUDIO_FOCUS)
            .setHandleAudioBecomingNoisy(PlaybackConfig.HANDLE_BECOMING_NOISY)
            .setWakeMode(PlaybackConfig.WAKE_MODE)
            .build()

        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                AppLogger.e("PlaybackService", "Error en ExoPlayer: ${error.errorCodeName} - ${error.message}", error)
            }
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                AppLogger.i("PlaybackService", "Transición de pista: id=${mediaItem?.mediaId}, uri=${mediaItem?.localConfiguration?.uri}")
            }
        })

        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivity)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(screenReceiver)
        } catch (_: Exception) {}
        serviceScope.cancel()

        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
