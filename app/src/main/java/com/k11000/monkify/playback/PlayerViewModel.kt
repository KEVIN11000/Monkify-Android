package com.k11000.monkify.playback

import android.content.ComponentName
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.k11000.monkify.MonkifyPlaybackService
import com.k11000.monkify.domain.model.Track
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import com.k11000.monkify.data.repository.SettingsRepository
import com.k11000.monkify.data.remote.api.MonkifyApi
import com.k11000.monkify.data.remote.dto.ActionResponseDto
import com.k11000.monkify.data.remote.dto.AddTrackToPlaylistRequest
import com.k11000.monkify.data.remote.dto.CreatePlaylistRequest
import com.k11000.monkify.data.remote.dto.PlaylistSummaryDto
import com.k11000.monkify.domain.model.DownloadState
import com.k11000.monkify.domain.repository.DownloadRepository
import com.k11000.monkify.data.repository.LocalTrackRepository
import com.k11000.monkify.domain.repository.PlaylistRepository
import com.k11000.monkify.playback.lyrics.LyricLine
import com.k11000.monkify.playback.lyrics.LrcParser

enum class PlaybackSourceType {
    PLAYLIST,
    SEARCH,
    HOME,
    AD_HOC_QUEUE
}

data class PlaybackSource(
    val type: PlaybackSourceType = PlaybackSourceType.AD_HOC_QUEUE,
    val title: String = "Lista sin nombre",
    val playlistId: String? = null
) {
    companion object {
        fun playlist(id: String, name: String) = PlaybackSource(
            type = PlaybackSourceType.PLAYLIST,
            title = name,
            playlistId = id
        )

        fun adHoc(title: String = "Lista sin nombre") = PlaybackSource(
            type = PlaybackSourceType.AD_HOC_QUEUE,
            title = title,
            playlistId = null
        )
    }
}

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val localTrackRepository: LocalTrackRepository,
    private val downloadRepository: DownloadRepository,
    private val monkifyApi: MonkifyApi,
    private val playlistRepository: PlaylistRepository? = null,
    private val settingsRepository: SettingsRepository? = null,
    private val stateHolder: PlaybackSessionStateHolder? = null
) : ViewModel() {

    val zenModeEnabled: StateFlow<Boolean> = settingsRepository?.zenModeEnabled
        ?.stateIn(viewModelScope, SharingStarted.Eagerly, true)
        ?: MutableStateFlow(true)

    val zenEngine: StateFlow<String> = settingsRepository?.zenEngine
        ?.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsRepository.DEFAULT_ZEN_ENGINE)
        ?: MutableStateFlow(SettingsRepository.DEFAULT_ZEN_ENGINE)

    val coverStyle: StateFlow<String> = settingsRepository?.coverStyle
        ?.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsRepository.DEFAULT_COVER_STYLE)
        ?: MutableStateFlow(SettingsRepository.DEFAULT_COVER_STYLE)

    fun toggleCoverStyle() {
        val nextStyle = if (coverStyle.value == SettingsRepository.COVER_STYLE_VINYL) {
            SettingsRepository.COVER_STYLE_MODERN
        } else {
            SettingsRepository.COVER_STYLE_VINYL
        }
        viewModelScope.launch {
            settingsRepository?.setCoverStyle(nextStyle)
        }
    }

    private var mediaController: MediaController? = null

    private val _lyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    val lyrics: StateFlow<List<LyricLine>> = _lyrics.asStateFlow()

    private val _isLoadingLyrics = MutableStateFlow(false)
    val isLoadingLyrics: StateFlow<Boolean> = _isLoadingLyrics.asStateFlow()

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

    private val _playbackError = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val playbackError: SharedFlow<String> = _playbackError.asSharedFlow()

    private val _isZenCleared = MutableStateFlow(false)
    val isZenCleared: StateFlow<Boolean> = _isZenCleared.asStateFlow()

    private val _playbackSource = MutableStateFlow(PlaybackSource.adHoc())
    val playbackSource: StateFlow<PlaybackSource> = _playbackSource.asStateFlow()

    private var currentQueue: List<Track> = emptyList()
    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    val queue: StateFlow<List<Track>> = _queue.asStateFlow()

    private var currentTrack: Track? = null

    private val _isCurrentFavorite = MutableStateFlow(false)
    val isCurrentFavorite: StateFlow<Boolean> = _isCurrentFavorite.asStateFlow()

    private val _isCurrentDownloaded = MutableStateFlow(false)
    val isCurrentDownloaded: StateFlow<Boolean> = _isCurrentDownloaded.asStateFlow()

    private val _isCurrentDownloading = MutableStateFlow(false)
    val isCurrentDownloading: StateFlow<Boolean> = _isCurrentDownloading.asStateFlow()

    private val _currentDownloadState = MutableStateFlow<DownloadState>(DownloadState.NotDownloaded)
    val currentDownloadState: StateFlow<DownloadState> = _currentDownloadState.asStateFlow()

    private var downloadStateJob: Job? = null
    private var favoriteJob: Job? = null

    fun setPlaybackSource(source: PlaybackSource) {
        _playbackSource.value = source
    }

    private fun observeTrackDownloadState(trackId: String) {
        downloadStateJob?.cancel()
        // Chequeo inmediato en disco para evitar que se muestre como 'no descargado' al cargar
        val localPath = downloadRepository.getLocalAudioPath(trackId)
        if (!localPath.isNullOrBlank()) {
            _isCurrentDownloaded.value = true
            _currentDownloadState.value = DownloadState.Downloaded(localPath)
        } else {
            _isCurrentDownloaded.value = false
            _currentDownloadState.value = DownloadState.NotDownloaded
        }

        downloadStateJob = viewModelScope.launch {
            downloadRepository.getDownloadState(trackId).collect { state ->
                _currentDownloadState.value = state
                _isCurrentDownloaded.value = state is DownloadState.Downloaded
                _isCurrentDownloading.value = state is DownloadState.Downloading
            }
        }
    }

    private fun observeTrackFavoriteState(trackId: String) {
        favoriteJob?.cancel()
        favoriteJob = viewModelScope.launch {
            localTrackRepository.isFavorite(trackId).collect { isFav ->
                _isCurrentFavorite.value = isFav
            }
        }
    }

    fun toggleZenCleared() {
        _isZenCleared.value = !_isZenCleared.value
    }

    fun setZenCleared(cleared: Boolean) {
        _isZenCleared.value = cleared
    }

    fun resolveCurrentTrack(): Track? {
        if (currentTrack != null) return currentTrack
        val item = mediaController?.currentMediaItem ?: return null
        val id = item.mediaId.ifBlank { return null }
        val resolved = currentQueue.find { it.id == id } ?: Track(
            id = id,
            title = item.mediaMetadata.title?.toString() ?: _currentTrackTitle.value,
            artist = item.mediaMetadata.artist?.toString() ?: _currentTrackArtist.value,
            album = item.mediaMetadata.albumTitle?.toString() ?: "",
            durationMs = _duration.value,
            coverUrl = item.mediaMetadata.artworkUri?.toString() ?: _coverUrl.value,
            streamUrl = item.localConfiguration?.uri?.toString() ?: ""
        )
        currentTrack = resolved
        return resolved
    }

    fun toggleFavorite() {
        val track = resolveCurrentTrack() ?: run {
            com.k11000.monkify.util.AppLogger.w("PlayerViewModel", "toggleFavorite: no hay pista activa en reproducción")
            return
        }
        viewModelScope.launch {
            try {
                localTrackRepository.toggleFavorite(track)
                com.k11000.monkify.util.AppLogger.i("PlayerViewModel", "toggleFavorite: alternado favorito para '${track.title}' (${track.id})")
            } catch (e: Exception) {
                com.k11000.monkify.util.AppLogger.e("PlayerViewModel", "Error al alternar favorito", e)
            }
        }
    }

    fun toggleDownload() {
        val track = resolveCurrentTrack() ?: run {
            com.k11000.monkify.util.AppLogger.w("PlayerViewModel", "toggleDownload: no hay pista activa en reproducción")
            return
        }
        viewModelScope.launch {
            downloadRepository.toggleDownload(track)
        }
    }

    fun skipToQueueIndex(index: Int) {
        if (index in currentQueue.indices) {
            mediaController?.seekTo(index, 0L)
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in currentQueue.indices || toIndex !in currentQueue.indices || fromIndex == toIndex) {
            return
        }
        val mutable = currentQueue.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        currentQueue = mutable
        _queue.value = mutable

        try {
            mediaController?.moveMediaItem(fromIndex, toIndex)
        } catch (e: Exception) {
            com.k11000.monkify.util.AppLogger.e("PlayerViewModel", "Error al mover MediaItem en ExoPlayer", e)
        }
    }

    fun removeQueueItem(index: Int) {
        if (index !in currentQueue.indices) return
        val mutable = currentQueue.toMutableList()
        mutable.removeAt(index)
        currentQueue = mutable
        _queue.value = mutable

        try {
            mediaController?.removeMediaItem(index)
        } catch (e: Exception) {
            com.k11000.monkify.util.AppLogger.e("PlayerViewModel", "Error al remover MediaItem de ExoPlayer", e)
        }
    }

    fun saveCurrentQueueAsPlaylist(name: String, onResult: (Result<Unit>) -> Unit = {}) {
        val tracksToSave = currentQueue.ifEmpty { listOfNotNull(currentTrack ?: resolveCurrentTrack()) }
        if (tracksToSave.isEmpty()) {
            onResult(Result.failure(IllegalStateException("No hay pistas activas en la cola para guardar")))
            return
        }
        viewModelScope.launch {
            try {
                if (playlistRepository != null) {
                    val createRes = playlistRepository.createPlaylist(name)
                    createRes.fold(
                        onSuccess = { createdPl ->
                            for (t in tracksToSave) {
                                playlistRepository.addTrackToPlaylist(createdPl.id, t.id)
                            }
                            _playbackSource.value = PlaybackSource.playlist(createdPl.id, name)
                            onResult(Result.success(Unit))
                        },
                        onFailure = { err ->
                            onResult(Result.failure(err))
                        }
                    )
                } else {
                    val pl = monkifyApi.createPlaylist(CreatePlaylistRequest(name = name))
                    for (t in tracksToSave) {
                        monkifyApi.addTrackToPlaylist(pl.id, AddTrackToPlaylistRequest(t.id))
                    }
                    _playbackSource.value = PlaybackSource.playlist(pl.id, name)
                    onResult(Result.success(Unit))
                }
            } catch (e: Exception) {
                com.k11000.monkify.util.AppLogger.e("PlayerViewModel", "Error al guardar cola como playlist", e)
                onResult(Result.failure(e))
            }
        }
    }

    private val _availablePlaylists = MutableStateFlow<List<PlaylistSummaryDto>>(emptyList())
    val availablePlaylists: StateFlow<List<PlaylistSummaryDto>> = _availablePlaylists.asStateFlow()

    private val _isLoadingPlaylists = MutableStateFlow(false)
    val isLoadingPlaylists: StateFlow<Boolean> = _isLoadingPlaylists.asStateFlow()

    val currentPlayingTrack: Track?
        get() = currentTrack

    fun loadPlaylistsForDialog() {
        viewModelScope.launch {
            _isLoadingPlaylists.value = true
            try {
                if (playlistRepository != null) {
                    val res = playlistRepository.getPlaylists()
                    res.onSuccess {
                        _availablePlaylists.value = it
                    }.onFailure {
                        _availablePlaylists.value = emptyList()
                    }
                } else {
                    val dtos = monkifyApi.getPlaylists()
                    _availablePlaylists.value = dtos
                }
            } catch (_: Exception) {
                _availablePlaylists.value = emptyList()
            } finally {
                _isLoadingPlaylists.value = false
            }
        }
    }

    fun addCurrentTrackToPlaylist(playlistId: String, onResult: (Result<Unit>) -> Unit = {}) {
        val track = currentTrack ?: run {
            onResult(Result.failure(IllegalStateException("No hay pista activa en reproducción")))
            return
        }
        viewModelScope.launch {
            try {
                if (playlistRepository != null) {
                    val res = playlistRepository.addTrackToPlaylist(playlistId, track.id)
                    onResult(res)
                } else {
                    val resp = monkifyApi.addTrackToPlaylist(playlistId, AddTrackToPlaylistRequest(track.id))
                    if (resp.success) {
                        onResult(Result.success(Unit))
                    } else {
                        onResult(Result.failure(RuntimeException(resp.message)))
                    }
                }
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    fun createPlaylistAndAddCurrentTrack(name: String, onResult: (Result<Unit>) -> Unit = {}) {
        val track = currentTrack ?: run {
            onResult(Result.failure(IllegalStateException("No hay pista activa en reproducción")))
            return
        }
        viewModelScope.launch {
            try {
                if (playlistRepository != null) {
                    val createRes = playlistRepository.createPlaylist(name)
                    createRes.fold(
                        onSuccess = { createdPl ->
                            val addRes = playlistRepository.addTrackToPlaylist(createdPl.id, track.id)
                            onResult(addRes)
                        },
                        onFailure = { err ->
                            onResult(Result.failure(err))
                        }
                    )
                } else {
                    val pl = monkifyApi.createPlaylist(CreatePlaylistRequest(name = name))
                    val addResp = monkifyApi.addTrackToPlaylist(pl.id, AddTrackToPlaylistRequest(track.id))
                    if (addResp.success) {
                        onResult(Result.success(Unit))
                    } else {
                        onResult(Result.failure(RuntimeException(addResp.message)))
                    }
                }
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    // Salidas de Audio del Sistema (Monkify Audio Route)
    data class Device(
        val id: String,
        val name: String,
        val isSelected: Boolean,
        val type: String = "Altavoz"
    )

    private val _devices = MutableStateFlow<List<Device>>(
        listOf(
            Device("this_device", "Altavoz del dispositivo", true, "Altavoz")
        )
    )
    val devices: StateFlow<List<Device>> = _devices.asStateFlow()

    private val _activeDeviceName = MutableStateFlow("Altavoz del dispositivo")
    val activeDeviceName: StateFlow<String> = _activeDeviceName.asStateFlow()

    fun refreshAudioDevices() {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val outputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                if (outputDevices.isNotEmpty()) {
                    val deviceList = outputDevices.map { dev ->
                        val (friendlyName, typeDesc) = resolveAudioDeviceDescription(dev)
                        Device(
                            id = "dev_${dev.id}",
                            name = friendlyName,
                            isSelected = isDeviceLikelyActive(dev, audioManager),
                            type = typeDesc
                        )
                    }
                    val hasActive = deviceList.any { it.isSelected }
                    val finalDevices = if (!hasActive && deviceList.isNotEmpty()) {
                        deviceList.mapIndexed { i, d -> if (i == 0) d.copy(isSelected = true) else d }
                    } else {
                        deviceList
                    }
                    _devices.value = finalDevices
                    val active = finalDevices.firstOrNull { it.isSelected } ?: finalDevices.firstOrNull()
                    if (active != null) {
                        _activeDeviceName.value = active.name
                    }
                    return
                }
            }
        } catch (e: Exception) {
            com.k11000.monkify.util.AppLogger.e("PlayerViewModel", "Error al obtener dispositivos de audio", e)
        }
        _devices.value = listOf(
            Device("default_speaker", "Altavoz del dispositivo", isSelected = true, type = "Altavoz")
        )
        _activeDeviceName.value = "Altavoz del dispositivo"
    }

    private fun resolveAudioDeviceDescription(dev: AudioDeviceInfo): Pair<String, String> {
        val prodName = dev.productName?.toString()?.takeIf { it.isNotBlank() }
        return when (dev.type) {
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER ->
                Pair(prodName ?: "Altavoz del dispositivo", "Altavoz")
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_USB_DEVICE ->
                Pair(prodName ?: "Auriculares con cable", "Auriculares")
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_BLE_HEADSET,
            AudioDeviceInfo.TYPE_BLE_SPEAKER ->
                Pair(prodName ?: "Dispositivo Bluetooth", "Bluetooth")
            else ->
                Pair(prodName ?: "Salida de audio (${dev.id})", "Audio")
        }
    }

    private fun isDeviceLikelyActive(dev: AudioDeviceInfo, audioManager: AudioManager): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val comm = audioManager.communicationDevice
            if (comm != null && comm.id == dev.id) return true
        }
        @Suppress("DEPRECATION")
        return when (dev.type) {
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> audioManager.isBluetoothA2dpOn
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_USB_HEADSET -> audioManager.isWiredHeadsetOn
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> !audioManager.isBluetoothA2dpOn && !audioManager.isWiredHeadsetOn
            else -> false
        }
    }

    fun selectDevice(deviceId: String) {
        _devices.value = _devices.value.map { device ->
            val selected = device.id == deviceId
            if (selected) {
                _activeDeviceName.value = device.name
            }
            device.copy(isSelected = selected)
        }
    }

    private var pendingPlayRequest: (() -> Unit)? = null

    init {
        stateHolder?.let { holder ->
            holder.currentTrack.value?.let { track ->
                currentTrack = track
                _currentTrackTitle.value = track.title
                _currentTrackArtist.value = track.artist
                _coverUrl.value = track.coverUrl
            }
            if (holder.queue.value.isNotEmpty()) {
                currentQueue = holder.queue.value
                _queue.value = holder.queue.value
            }
            if (holder.lyrics.value.isNotEmpty()) {
                _lyrics.value = holder.lyrics.value
            }
            _playbackSource.value = holder.playbackSource.value
        }
        initializeController()
        refreshAudioDevices()
    }

    private fun syncFromController(controller: Player) {
        val currentItem = controller.currentMediaItem
        if (currentItem != null) {
            val mediaId = currentItem.mediaId
            val title = currentItem.mediaMetadata.title?.toString()?.takeIf { it.isNotBlank() } ?: _currentTrackTitle.value
            val artist = currentItem.mediaMetadata.artist?.toString()?.takeIf { it.isNotBlank() } ?: _currentTrackArtist.value
            val artworkUri = currentItem.mediaMetadata.artworkUri?.toString() ?: _coverUrl.value

            val track = currentQueue.find { it.id == mediaId }
                ?: stateHolder?.queue?.value?.find { it.id == mediaId }
                ?: Track(
                    id = mediaId,
                    title = title,
                    artist = artist,
                    album = currentItem.mediaMetadata.albumTitle?.toString() ?: "",
                    durationMs = controller.duration.coerceAtLeast(0L),
                    coverUrl = artworkUri,
                    streamUrl = currentItem.localConfiguration?.uri?.toString() ?: ""
                )

            currentTrack = track
            _currentTrackTitle.value = track.title
            _currentTrackArtist.value = track.artist
            _coverUrl.value = track.coverUrl
            if (controller.duration > 0) {
                _duration.value = controller.duration
            }
            _currentPosition.value = controller.currentPosition.coerceAtLeast(0L)
            _isPlaying.value = controller.isPlaying

            stateHolder?.updateCurrentTrack(track)

            if (_queue.value.isEmpty()) {
                if (stateHolder != null && stateHolder.queue.value.isNotEmpty()) {
                    currentQueue = stateHolder.queue.value
                    _queue.value = stateHolder.queue.value
                } else if (controller.mediaItemCount > 0) {
                    val reconstructedQueue = mutableListOf<Track>()
                    for (i in 0 until controller.mediaItemCount) {
                        val item = controller.getMediaItemAt(i)
                        val t = Track(
                            id = item.mediaId,
                            title = item.mediaMetadata.title?.toString() ?: "Pista ${i + 1}",
                            artist = item.mediaMetadata.artist?.toString() ?: "",
                            album = item.mediaMetadata.albumTitle?.toString() ?: "",
                            durationMs = 0L,
                            coverUrl = item.mediaMetadata.artworkUri?.toString(),
                            streamUrl = item.localConfiguration?.uri?.toString() ?: ""
                        )
                        reconstructedQueue.add(t)
                    }
                    currentQueue = reconstructedQueue
                    _queue.value = reconstructedQueue
                    stateHolder?.updateQueue(reconstructedQueue)
                }
            }

            observeTrackDownloadState(track.id)
            observeTrackFavoriteState(track.id)

            if (_lyrics.value.isEmpty()) {
                loadLyricsForTrack(track.id)
            }
        }
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
                syncFromController(controller)

                pendingPlayRequest?.invoke()
                pendingPlayRequest = null
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

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            super.onMediaItemTransition(mediaItem, reason)
            val mediaId = mediaItem?.mediaId ?: return
            val track = currentQueue.find { it.id == mediaId } ?: Track(
                id = mediaId,
                title = mediaItem.mediaMetadata.title?.toString() ?: _currentTrackTitle.value,
                artist = mediaItem.mediaMetadata.artist?.toString() ?: _currentTrackArtist.value,
                album = mediaItem.mediaMetadata.albumTitle?.toString() ?: "",
                durationMs = _duration.value,
                coverUrl = mediaItem.mediaMetadata.artworkUri?.toString() ?: _coverUrl.value,
                streamUrl = mediaItem.localConfiguration?.uri?.toString() ?: ""
            )
            currentTrack = track
            _currentTrackTitle.value = track.title
            _currentTrackArtist.value = track.artist
            _coverUrl.value = track.coverUrl
            stateHolder?.updateCurrentTrack(track)
            loadLyricsForTrack(track.id)
            observeTrackDownloadState(track.id)
            observeTrackFavoriteState(track.id)

            viewModelScope.launch {
                localTrackRepository.recordTrackPlayed(track.id)
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)
            if (playbackState == Player.STATE_READY) {
                mediaController?.let {
                    _duration.value = it.duration.coerceAtLeast(0)
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)
            _isPlaying.value = false
            val errorMsg = parsePlaybackError(error)
            com.k11000.monkify.util.AppLogger.e("PlayerViewModel", "Error de reproducción: $errorMsg (código=${error.errorCode}, nombre=${error.errorCodeName})", error)
            _playbackError.tryEmit(errorMsg)
        }
    }

    fun parsePlaybackError(error: PlaybackException): String {
        val cause = error.cause
        val message = cause?.message ?: error.message ?: ""
        return when {
            message.contains("404") || error.errorCode == PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
                "Pista no disponible en el servidor (HTTP 404)"
            message.contains("Unable to connect") || message.contains("ConnectException") || error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ->
                "Error de conexión de red al reproducir"
            else ->
                "Error de reproducción: ${error.errorCodeName}"
        }
    }

    fun loadLyricsForTrack(trackId: String) {
        viewModelScope.launch {
            _isLoadingLyrics.value = true
            _lyrics.value = emptyList()
            try {
                // 1. Si la pista tiene letras descargadas localmente en disco, cargarlas directamente
                val localLyrics = downloadRepository.getLocalLyrics(trackId)
                if (!localLyrics.isNullOrBlank()) {
                    val parsed = LrcParser.parse(localLyrics)
                    _lyrics.value = parsed
                    stateHolder?.updateLyrics(parsed)
                    return@launch
                }

                // 2. Consulta al API de Monkify
                val response = monkifyApi.getLyrics(trackId)
                if (response.content.isNotBlank()) {
                    val parsed = LrcParser.parse(response.content)
                    _lyrics.value = parsed
                    stateHolder?.updateLyrics(parsed)
                    return@launch
                }

                // 3. Fallback directo a LRCLIB si el Worker no tiene letra
                val track = currentTrack
                if (track != null && track.title.isNotBlank()) {
                    val fallback = fetchLrclibClientFallback(track.title, track.artist)
                    if (!fallback.isNullOrBlank()) {
                        val parsed = LrcParser.parse(fallback)
                        _lyrics.value = parsed
                        stateHolder?.updateLyrics(parsed)
                    }
                }
            } catch (_: Exception) {
                val track = currentTrack
                if (track != null && track.title.isNotBlank()) {
                    try {
                        val fallback = fetchLrclibClientFallback(track.title, track.artist)
                        if (!fallback.isNullOrBlank()) {
                            val parsed = LrcParser.parse(fallback)
                            _lyrics.value = parsed
                            stateHolder?.updateLyrics(parsed)
                        }
                    } catch (_: Exception) {}
                }
            } finally {
                _isLoadingLyrics.value = false
            }
        }
    }

    private suspend fun fetchLrclibClientFallback(title: String, artist: String): String? =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val encodedTitle = java.net.URLEncoder.encode(title, "UTF-8")
                val encodedArtist = java.net.URLEncoder.encode(artist, "UTF-8")
                val url = java.net.URL("https://lrclib.net/api/get?artist_name=$encodedArtist&track_name=$encodedTitle")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                conn.setRequestProperty("User-Agent", "Monkify-Android/0.4.0")
                if (conn.responseCode == 200) {
                    val json = conn.inputStream.bufferedReader().use { it.readText() }
                    val syncedMatch = Regex(""""syncedLyrics":\s*"([^"]+)"""").find(json)
                    if (syncedMatch != null) {
                        return@withContext syncedMatch.groupValues[1].replace("\\n", "\n").replace("\\r", "")
                    }
                    val plainMatch = Regex(""""plainLyrics":\s*"([^"]+)"""").find(json)
                    if (plainMatch != null) {
                        return@withContext plainMatch.groupValues[1].replace("\\n", "\n").replace("\\r", "")
                    }
                }
                null
            } catch (_: Exception) {
                null
            }
        }

    fun setLyricsContent(lrcContent: String) {
        _lyrics.value = LrcParser.parse(lrcContent)
    }

    fun playQueue(
        tracks: List<Track>,
        startIndex: Int = 0,
        source: PlaybackSource = PlaybackSource.adHoc()
    ) {
        if (tracks.isEmpty()) return
        _playbackSource.value = source
        currentQueue = tracks
        _queue.value = tracks

        val validStartIndex = if (startIndex in tracks.indices) startIndex else 0
        val activeTrack = tracks[validStartIndex]
        currentTrack = activeTrack

        _currentTrackTitle.value = activeTrack.title
        _currentTrackArtist.value = activeTrack.artist
        _coverUrl.value = activeTrack.coverUrl
        stateHolder?.updatePlaybackSource(source)
        stateHolder?.updateQueue(tracks)
        stateHolder?.updateCurrentTrack(activeTrack)

        loadLyricsForTrack(activeTrack.id)
        observeTrackDownloadState(activeTrack.id)
        observeTrackFavoriteState(activeTrack.id)

        val controller = mediaController
        if (controller == null) {
            pendingPlayRequest = {
                playQueue(tracks, validStartIndex, source)
            }
            return
        }

        val mediaItems = tracks.map { track ->
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

            val localPath = downloadRepository.getLocalAudioPath(track.id)
            val streamUri = if (!localPath.isNullOrBlank() && java.io.File(localPath).exists()) {
                Uri.fromFile(java.io.File(localPath))
            } else if (track.streamUrl.isNotBlank()) {
                if (track.streamUrl.startsWith("/")) {
                    Uri.fromFile(java.io.File(track.streamUrl))
                } else {
                    Uri.parse(track.streamUrl)
                }
            } else {
                Uri.parse("https://cdn.pixabay.com/audio/2022/05/27/audio_1808fbf07a.mp3")
            }

            MediaItem.Builder()
                .setMediaId(track.id)
                .setUri(streamUri)
                .setMediaMetadata(metadata)
                .build()
        }

        controller.setMediaItems(mediaItems, validStartIndex, 0L)
        controller.prepare()
        controller.play()
    }

    fun playTrack(track: Track, source: PlaybackSource = PlaybackSource.adHoc()) {
        playQueue(listOf(track), 0, source)
    }

    fun playTestTrack() {
        val testTrack = Track(
            id = "test_track_1",
            title = "Lofi Study Beats",
            artist = "FASSounds",
            album = "Pixabay Chill",
            durationMs = 120000,
            coverUrl = "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=500&q=80",
            streamUrl = "https://cdn.pixabay.com/audio/2022/05/27/audio_1808fbf07a.mp3"
        )
        playTrack(testTrack)
        setLyricsContent(
            """
            [00:00.00] ♪ Instrumental Intro ♪
            [00:05.00] Relax and take a breath
            [00:10.00] Finding peace in the quiet night
            [00:15.00] Soft melodies floating by
            [00:20.00] Just enjoy the sound of the flow
            [00:30.00] Let the rhythm guide your mind
            [00:45.00] Deep focus, calm thoughts
            [01:00.00] Everything is in harmony
            """.trimIndent()
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
