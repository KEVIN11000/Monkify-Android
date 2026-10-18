package com.k11000.monkify.ui.screens.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.k11000.monkify.data.remote.api.MonkifyApi
import com.k11000.monkify.data.remote.dto.AlbumSummaryDto
import com.k11000.monkify.data.remote.dto.PlaylistSummaryDto
import com.k11000.monkify.data.repository.LocalTrackRepository
import com.k11000.monkify.domain.model.Track
import com.k11000.monkify.domain.repository.DownloadRepository
import com.k11000.monkify.domain.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LibraryFilter(val displayName: String) {
    ALL("Todo"),
    FAVORITES("Favoritos"),
    DOWNLOADS("Descargas"),
    PLAYLISTS("Playlists"),
    ALBUMS("Álbumes")
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val localTrackRepository: LocalTrackRepository,
    private val monkifyApi: MonkifyApi? = null,
    private val playlistRepository: PlaylistRepository? = null,
    private val downloadRepository: DownloadRepository? = null
) : ViewModel() {

    val totalStorageBytes: StateFlow<Long> = (downloadRepository?.downloadedTrackIds
        ?.map { downloadRepository.getTotalDownloadsSizeBytes() }
        ?: flowOf(0L))
        .stateIn(viewModelScope, SharingStarted.Eagerly, downloadRepository?.getTotalDownloadsSizeBytes() ?: 0L)

    private val _selectedFilter = MutableStateFlow(LibraryFilter.ALL)
    val selectedFilter: StateFlow<LibraryFilter> = _selectedFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    private val _playlists = MutableStateFlow<List<PlaylistSummaryDto>>(emptyList())
    val playlists: StateFlow<List<PlaylistSummaryDto>> = _playlists.asStateFlow()

    val filteredPlaylists: StateFlow<List<PlaylistSummaryDto>> = combine(
        _playlists,
        _searchQuery
    ) { list, query ->
        if (query.isBlank()) list
        else list.filter { it.name.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _isLoadingPlaylists = MutableStateFlow(false)
    val isLoadingPlaylists: StateFlow<Boolean> = _isLoadingPlaylists.asStateFlow()

    private val _albums = MutableStateFlow<List<AlbumSummaryDto>>(emptyList())
    val albums: StateFlow<List<AlbumSummaryDto>> = _albums.asStateFlow()

    private val _isLoadingAlbums = MutableStateFlow(false)
    val isLoadingAlbums: StateFlow<Boolean> = _isLoadingAlbums.asStateFlow()

    val filteredAlbums: StateFlow<List<AlbumSummaryDto>> = combine(
        _albums,
        _searchQuery,
        localTrackRepository.favoriteTracks,
        localTrackRepository.downloadedTracks
    ) { remoteAlbums, query, favorites, downloads ->
        val baseAlbums = if (remoteAlbums.isNotEmpty()) {
            remoteAlbums
        } else {
            deriveLocalAlbums((favorites + downloads).distinctBy { it.id })
        }
        if (query.isBlank()) {
            baseAlbums
        } else {
            baseAlbums.filter {
                it.album.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private fun deriveLocalAlbums(tracks: List<Track>): List<AlbumSummaryDto> {
        return tracks
            .filter { it.album.isNotBlank() }
            .groupBy { it.album.trim() }
            .map { (albumName, albumTracks) ->
                AlbumSummaryDto(
                    album = albumName,
                    artist = albumTracks.firstOrNull()?.artist ?: "",
                    trackCount = albumTracks.size,
                    coverUrl = albumTracks.firstNotNullOfOrNull { it.coverUrl?.takeIf { url -> url.isNotBlank() } }
                )
            }
            .sortedWith(compareByDescending<AlbumSummaryDto> { it.trackCount }.thenBy { it.album.lowercase() })
    }

    fun deleteDownload(trackId: String, onComplete: (Result<Unit>) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val repo = downloadRepository
                if (repo != null) {
                    val result = repo.deleteDownloadedTrack(trackId)
                    onComplete(result)
                } else {
                    localTrackRepository.removeDownload(trackId)
                    onComplete(Result.success(Unit))
                }
            } catch (e: Exception) {
                onComplete(Result.failure(e))
            }
        }
    }

    init {
        loadPlaylists()
        loadAlbums()
    }

    fun loadAlbums() {
        viewModelScope.launch {
            _isLoadingAlbums.value = true
            try {
                val remote = monkifyApi?.getAlbums(limit = 100, offset = 0) ?: emptyList()
                if (remote.isNotEmpty()) {
                    _albums.value = remote
                } else {
                    val localTracks = localTrackRepository.getAllLocalTracks()
                    if (localTracks.isNotEmpty()) {
                        _albums.value = deriveLocalAlbums(localTracks)
                    }
                }
            } catch (_: Exception) {
                val localTracks = localTrackRepository.getAllLocalTracks()
                if (localTracks.isNotEmpty()) {
                    _albums.value = deriveLocalAlbums(localTracks)
                }
            } finally {
                _isLoadingAlbums.value = false
            }
        }
    }

    fun loadPlaylists() {
        viewModelScope.launch {
            _isLoadingPlaylists.value = true
            try {
                if (playlistRepository != null) {
                    val result = playlistRepository.getPlaylists()
                    if (result.isSuccess) {
                        _playlists.value = result.getOrThrow()
                    }
                } else if (monkifyApi != null) {
                    _playlists.value = monkifyApi.getPlaylists()
                }
            } catch (_: Exception) {
                // Silencioso o sin conexión
            } finally {
                _isLoadingPlaylists.value = false
            }
        }
    }

    fun createPlaylist(name: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val repo = playlistRepository
            if (repo != null) {
                val result = repo.createPlaylist(name)
                if (result.isSuccess) {
                    loadPlaylists()
                    onResult(true)
                } else {
                    onResult(false)
                }
            } else if (monkifyApi != null) {
                try {
                    val created = monkifyApi.createPlaylist(com.k11000.monkify.data.remote.dto.CreatePlaylistRequest(name))
                    _playlists.value = _playlists.value + created
                    onResult(true)
                } catch (_: Exception) {
                    onResult(false)
                }
            } else {
                onResult(false)
            }
        }
    }

    fun deletePlaylist(playlistId: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val repo = playlistRepository
            if (repo != null) {
                val result = repo.deletePlaylist(playlistId)
                if (result.isSuccess) {
                    _playlists.value = _playlists.value.filter { it.id != playlistId }
                    onResult(true)
                } else {
                    onResult(false)
                }
            } else if (monkifyApi != null) {
                try {
                    monkifyApi.deletePlaylist(playlistId)
                    _playlists.value = _playlists.value.filter { it.id != playlistId }
                    onResult(true)
                } catch (_: Exception) {
                    onResult(false)
                }
            } else {
                onResult(false)
            }
        }
    }

    val favoriteTracks: StateFlow<List<Track>> = localTrackRepository.favoriteTracks
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val downloadedTracks: StateFlow<List<Track>> = localTrackRepository.downloadedTracks
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val displayedTracks: StateFlow<List<Track>> = combine(
        _selectedFilter,
        _searchQuery,
        localTrackRepository.favoriteTracks,
        localTrackRepository.downloadedTracks
    ) { filter, query, favorites, downloads ->
        val baseList = when (filter) {
            LibraryFilter.FAVORITES -> favorites
            LibraryFilter.DOWNLOADS -> downloads
            LibraryFilter.ALL, LibraryFilter.PLAYLISTS, LibraryFilter.ALBUMS -> (favorites + downloads).distinctBy { it.id }
        }
        if (query.isBlank()) {
            baseList
        } else {
            baseList.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true) ||
                it.album.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setFilter(filter: LibraryFilter) {
        _selectedFilter.value = filter
        if (filter == LibraryFilter.ALBUMS && _albums.value.isEmpty()) {
            loadAlbums()
        }
    }
}

