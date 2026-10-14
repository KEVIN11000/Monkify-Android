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
import com.k11000.monkify.domain.model.Track
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
    @param:ApplicationContext private val context: Context
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

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _isZenCleared = MutableStateFlow(false)
    val isZenCleared: StateFlow<Boolean> = _isZenCleared.asStateFlow()

    fun toggleZenCleared() {
        _isZenCleared.value = !_isZenCleared.value
    }

    fun setZenCleared(cleared: Boolean) {
        _isZenCleared.value = cleared
    }

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
            mediaController?.let { controller ->
                controller.addListener(playerListener)
                _isShuffleEnabled.value = controller.shuffleModeEnabled
                _repeatMode.value = controller.repeatMode
                _isPlaying.value = controller.isPlaying
            }
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

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _isShuffleEnabled.value = shuffleModeEnabled
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _repeatMode.value = repeatMode
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

    fun playTrack(track: Track) {
        val controller = mediaController ?: return

        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .apply {
                if (!track.coverUrl.isNullOrBlank()) {
                    setArtworkUri(Uri.parse(track.coverUrl))
                }
            }
            .build()

        val streamUri = if (track.streamUrl.isNotBlank()) {
            Uri.parse(track.streamUrl)
        } else {
            Uri.parse("https://cdn.pixabay.com/audio/2022/05/27/audio_1808fbf07a.mp3")
        }

        val mediaItem = MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(streamUri)
            .setMediaMetadata(metadata)
            .build()

        controller.setMediaItem(mediaItem)
        controller.prepare()
        controller.play()
    }

    fun playTestTrack() {
        playTrack(
            Track(
                id = "test_track_1",
                title = "Lofi Study Beats",
                artist = "FASSounds",
                album = "Pixabay Chill",
                durationMs = 120000,
                coverUrl = "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=500&q=80",
                streamUrl = "https://cdn.pixabay.com/audio/2022/05/27/audio_1808fbf07a.mp3"
            )
        )
    }

    fun togglePlayPause() {
        mediaController?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
    }

    fun toggleShuffle() {
        mediaController?.let {
            val newShuffle = !it.shuffleModeEnabled
            it.shuffleModeEnabled = newShuffle
            _isShuffleEnabled.value = newShuffle
        }
    }

    fun toggleRepeatMode() {
        mediaController?.let {
            val nextMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
            it.repeatMode = nextMode
            _repeatMode.value = nextMode
        }
    }

    fun skipToNext() {
        mediaController?.let {
            if (it.hasNextMediaItem()) {
                it.seekToNextMediaItem()
            } else {
                it.seekTo(0)
            }
        }
    }

    fun skipToPrevious() {
        mediaController?.let {
            if (it.currentPosition > 3000L || !it.hasPreviousMediaItem()) {
                it.seekTo(0)
            } else {
                it.seekToPreviousMediaItem()
            }
        }
    }

    override fun onCleared() {
        mediaController?.removeListener(playerListener)
        mediaController?.release()
        super.onCleared()
    }
}
