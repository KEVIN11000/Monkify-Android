package com.k11000.monkify.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
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

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val localTrackRepository: LocalTrackRepository,
    private val downloadRepository: DownloadRepository,
    private val monkifyApi: MonkifyApi,
    private val playlistRepository: PlaylistRepository? = null,
    private val settingsRepository: SettingsRepository? = null
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

    private fun observeTrackDownloadState(trackId: String) {
        downloadStateJob?.cancel()
        downloadStateJob = viewModelScope.launch {
            downloadRepository.getDownloadState(trackId).collect { state ->
                _currentDownloadState.value = state
                _isCurrentDownloaded.value = state is DownloadState.Downloaded
                _isCurrentDownloading.value = state is DownloadState.Downloading
            }
        }
    }

    fun toggleZenCleared() {
        _isZenCleared.value = !_isZenCleared.value
    }

    fun setZenCleared(cleared: Boolean) {
        _isZenCleared.value = cleared
    }

    fun toggleFavorite() {
        val track = currentTrack ?: return
        viewModelScope.launch {
            localTrackRepository.toggleFavorite(track)
            _isCurrentFavorite.value = !_isCurrentFavorite.value
        }
    }

    fun toggleDownload() {
        val track = currentTrack ?: return
        viewModelScope.launch {
            downloadRepository.toggleDownload(track)
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

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            super.onMediaItemTransition(mediaItem, reason)
            val mediaId = mediaItem?.mediaId ?: return
            val track = currentQueue.find { it.id == mediaId }
            if (track != null) {
                currentTrack = track
                _currentTrackTitle.value = track.title
                _currentTrackArtist.value = track.artist
                _coverUrl.value = track.coverUrl
                loadLyricsForTrack(track.id)
                observeTrackDownloadState(track.id)
                viewModelScope.launch {
                    localTrackRepository.isFavorite(track.id).collect { isFav ->
                        _isCurrentFavorite.value = isFav
                    }
                }
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
                // Si la pista tiene letras descargadas localmente en disco, cargarlas directamente
                val localLyrics = downloadRepository.getLocalLyrics(trackId)
                if (!localLyrics.isNullOrBlank()) {
                    _lyrics.value = LrcParser.parse(localLyrics)
                    return@launch
                }

                val response = monkifyApi.getLyrics(trackId)
                if (response.content.isNotBlank()) {
                    _lyrics.value = LrcParser.parse(response.content)
                }
            } catch (_: Exception) {
                // Silencioso: pista sin letras asociadas
            } finally {
                _isLoadingLyrics.value = false
            }
        }
    }

    fun setLyricsContent(lrcContent: String) {
        _lyrics.value = LrcParser.parse(lrcContent)
    }

    fun playQueue(tracks: List<Track>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        currentQueue = tracks
        _queue.value = tracks

        val validStartIndex = if (startIndex in tracks.indices) startIndex else 0
        val activeTrack = tracks[validStartIndex]
        currentTrack = activeTrack

        _currentTrackTitle.value = activeTrack.title
        _currentTrackArtist.value = activeTrack.artist
        _coverUrl.value = activeTrack.coverUrl
        loadLyricsForTrack(activeTrack.id)
        observeTrackDownloadState(activeTrack.id)

        viewModelScope.launch {
            localTrackRepository.isFavorite(activeTrack.id).collect { isFav ->
                _isCurrentFavorite.value = isFav
            }
        }

        val controller = mediaController ?: return

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

    fun playTrack(track: Track) {
        playQueue(listOf(track), 0)
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
