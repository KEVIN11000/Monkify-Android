package com.k11000.monkify.ui.screens.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.k11000.monkify.BuildConfig
import com.k11000.monkify.data.remote.api.MonkifyApi
import com.k11000.monkify.data.remote.dto.toDomain
import com.k11000.monkify.data.repository.LocalTrackRepository
import com.k11000.monkify.domain.model.Track
import com.k11000.monkify.domain.repository.DownloadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    private val api: MonkifyApi,
    private val localTrackRepository: LocalTrackRepository? = null,
    private val downloadRepository: DownloadRepository? = null
) : ViewModel() {

    val downloadedTrackIds: StateFlow<Set<String>> = downloadRepository?.downloadedTrackIds
        ?.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
        ?: MutableStateFlow(emptySet())

    private val _isDownloadingAll = MutableStateFlow(false)
    val isDownloadingAll: StateFlow<Boolean> = _isDownloadingAll.asStateFlow()

    private val _downloadProgress = MutableStateFlow(Pair(0, 0))
    val downloadProgress: StateFlow<Pair<Int, Int>> = _downloadProgress.asStateFlow()

    private val _albumName = MutableStateFlow("")
    val albumName: StateFlow<String> = _albumName.asStateFlow()

    private val _artistName = MutableStateFlow("")
    val artistName: StateFlow<String> = _artistName.asStateFlow()

    private val _coverUrl = MutableStateFlow<String?>(null)
    val coverUrl: StateFlow<String?> = _coverUrl.asStateFlow()

    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var albumDownloadJob: Job? = null

    private fun albumDownloadKey(name: String): String = "album:${name.trim()}"

    fun loadAlbum(
        album: String,
        initialArtist: String = "",
        initialCoverUrl: String? = null
    ) {
        val cleanAlbum = album.trim()
        _albumName.value = cleanAlbum
        if (initialArtist.isNotBlank()) {
            _artistName.value = initialArtist
        }
        if (!initialCoverUrl.isNullOrBlank()) {
            _coverUrl.value = initialCoverUrl
        }

        val downloadKey = albumDownloadKey(cleanAlbum)
        albumDownloadJob?.cancel()
        albumDownloadJob = viewModelScope.launch {
            downloadRepository?.let { repo ->
                launch {
                    repo.isPlaylistDownloading(downloadKey).collect { isDl ->
                        _isDownloadingAll.value = isDl
                    }
                }
                launch {
                    repo.getPlaylistDownloadProgress(downloadKey).collect { progress ->
                        _downloadProgress.value = progress
                    }
                }
            }
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val dtos = api.getAlbumTracks(cleanAlbum)
                val mapped = dtos.map { it.toDomain(BuildConfig.BASE_URL) }
                if (mapped.isNotEmpty()) {
                    _tracks.value = mapped
                    if (_artistName.value.isBlank()) {
                        _artistName.value = mapped.firstOrNull()?.artist ?: ""
                    }
                    if (_coverUrl.value.isNullOrBlank()) {
                        _coverUrl.value = mapped.firstNotNullOfOrNull { it.coverUrl?.takeIf { url -> url.isNotBlank() } }
                    }
                } else {
                    val localFallback = localTrackRepository?.getTracksByAlbum(cleanAlbum) ?: emptyList()
                    _tracks.value = localFallback
                    if (_artistName.value.isBlank()) {
                        _artistName.value = localFallback.firstOrNull()?.artist ?: ""
                    }
                    if (_coverUrl.value.isNullOrBlank()) {
                        _coverUrl.value = localFallback.firstNotNullOfOrNull { it.coverUrl?.takeIf { url -> url.isNotBlank() } }
                    }
                }
            } catch (_: Exception) {
                val localFallback = localTrackRepository?.getTracksByAlbum(cleanAlbum) ?: emptyList()
                _tracks.value = localFallback
                if (_artistName.value.isBlank()) {
                    _artistName.value = localFallback.firstOrNull()?.artist ?: ""
                }
                if (_coverUrl.value.isNullOrBlank()) {
                    _coverUrl.value = localFallback.firstNotNullOfOrNull { it.coverUrl?.takeIf { url -> url.isNotBlank() } }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun downloadAllTracks(onComplete: (Result<Unit>) -> Unit = {}) {
        val currentAlbum = _albumName.value
        if (currentAlbum.isBlank()) {
            onComplete(Result.failure(IllegalStateException("No hay álbum seleccionado")))
            return
        }
        val tracksToDownload = _tracks.value
        if (tracksToDownload.isEmpty()) {
            onComplete(Result.success(Unit))
            return
        }
        val repo = downloadRepository ?: run {
            onComplete(Result.failure(IllegalStateException("DownloadRepository no disponible")))
            return
        }

        repo.downloadPlaylist(albumDownloadKey(currentAlbum), tracksToDownload)
        onComplete(Result.success(Unit))
    }

    fun cancelDownloadAll() {
        val currentAlbum = _albumName.value
        if (currentAlbum.isBlank()) return
        downloadRepository?.cancelPlaylistDownload(albumDownloadKey(currentAlbum))
    }

    fun toggleDownloadTrack(track: Track) {
        viewModelScope.launch {
            downloadRepository?.toggleDownload(track)
        }
    }
}
