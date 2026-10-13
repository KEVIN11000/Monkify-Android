package com.k11000.monkify

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import com.k11000.monkify.playback.PlaybackConfig
import com.k11000.monkify.playback.PlaybackNotificationConfig
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@UnstableApi
@AndroidEntryPoint
class MonkifyPlaybackService : MediaSessionService() {

    @Inject
    lateinit var cache: SimpleCache

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        PlaybackNotificationConfig.createNotificationChannel(this)
        setMediaNotificationProvider(PlaybackNotificationConfig.createNotificationProvider(this))

        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(DefaultHttpDataSource.Factory())
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory))
            .setAudioAttributes(PlaybackConfig.audioAttributes(), PlaybackConfig.HANDLE_AUDIO_FOCUS)
            .setHandleAudioBecomingNoisy(PlaybackConfig.HANDLE_BECOMING_NOISY)
            .setWakeMode(PlaybackConfig.WAKE_MODE)
            .build()

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
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
