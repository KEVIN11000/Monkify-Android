package com.k11000.monkify.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.k11000.monkify.MonkifyPlaybackService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private var mediaController: MediaController? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentTrackTitle = MutableStateFlow("Sin reproducir")
    val currentTrackTitle: StateFlow<String> = _currentTrackTitle.asStateFlow()

    private val _currentTrackArtist = MutableStateFlow("")
    val currentTrackArtist: StateFlow<String> = _currentTrackArtist.asStateFlow()

    private val _coverUrl = MutableStateFlow<String?>(null)
    val coverUrl: StateFlow<String?> = _coverUrl.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    // Monkify Connect Devices
    data class Device(val id: String, val name: String, val isSelected: Boolean)

    private val _devices = MutableStateFlow(
        listOf(
            Device("this_device", "Este Dispositivo (Android 15)", true),
            Device("desktop_pc", "Monkify Desktop (PC Sala)", false),
            Device("tv_living", "Monkify TV (Living)", false)
        )
    )
    val devices: StateFlow<List<Device>> = _devices.asStateFlow()

    private val _activeDeviceName = MutableStateFlow("Este Dispositivo")
    val activeDeviceName: StateFlow<String> = _activeDeviceName.asStateFlow()

    fun selectDevice(deviceId: String) {
        _devices.value = _devices.value.map { device ->
            val selected = device.id == deviceId
            if (selected) {
                _activeDeviceName.value = device.name
            }
            device.copy(isSelected = selected)
        }
    }

    init {
        initializeController()
    }

    private fun initializeController() {
        val sessionToken = SessionToken(context, ComponentName(context, MonkifyPlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        
        controllerFuture.addListener({
            mediaController = controllerFuture.get()
            mediaController?.addListener(playerListener)
        }, ContextCompat.getMainExecutor(context))
        
        // Polling loop for playback progress
        viewModelScope.launch {
            while (true) {
                delay(1000)
                mediaController?.let {
                    if (it.isPlaying) {
                        _currentPosition.value = it.currentPosition
                        _duration.value = it.duration.coerceAtLeast(0)
                        if (_duration.value > 0) {
                            _progress.value = (_currentPosition.value.toFloat() / _duration.value.toFloat())
                        }
                    }
                }
            }
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            _currentTrackTitle.value = mediaMetadata.title?.toString() ?: "Desconocido"
            _currentTrackArtist.value = mediaMetadata.artist?.toString() ?: ""
            _coverUrl.value = mediaMetadata.artworkUri?.toString()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)
            if (playbackState == Player.STATE_READY) {
                mediaController?.let {
                    _duration.value = it.duration.coerceAtLeast(0)
                }
            }
        }
    }

    fun playTestTrack() {
        val controller = mediaController ?: return
        
        val metadata = MediaMetadata.Builder()
            .setTitle("Lofi Study Beats")
            .setArtist("FASSounds")
            .setArtworkUri(Uri.parse("https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=500&q=80"))
            .build()

        val mediaItem = MediaItem.Builder()
            .setMediaId("test_track_1")
            // Royalty-free Lofi Track from Pixabay for testing
            .setUri("https://cdn.pixabay.com/audio/2022/05/27/audio_1808fbf07a.mp3") 
            .setMediaMetadata(metadata)
            .build()

        controller.setMediaItem(mediaItem)
        controller.prepare()
        controller.play()
    }

    fun togglePlayPause() {
        mediaController?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
    }

    override fun onCleared() {
        mediaController?.removeListener(playerListener)
        mediaController?.release()
        super.onCleared()
    }
}
