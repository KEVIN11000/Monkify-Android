package com.k11000.monkify.ui.screens.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.k11000.monkify.data.remote.api.MonkifyApi
import com.k11000.monkify.data.remote.dto.PlaylistSummaryDto
import com.k11000.monkify.data.repository.LocalTrackRepository
import com.k11000.monkify.domain.model.Track
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LibraryFilter(val displayName: String) {
    ALL("Todo"),
    FAVORITES("Favoritos"),
    DOWNLOADS("Descargas"),
    PLAYLISTS("Playlists")
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val localTrackRepository: LocalTrackRepository,
    private val monkifyApi: MonkifyApi? = null
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(LibraryFilter.ALL)
    val selectedFilter: StateFlow<LibraryFilter> = _selectedFilter.asStateFlow()

    private val _playlists = MutableStateFlow<List<PlaylistSummaryDto>>(emptyList())
    val playlists: StateFlow<List<PlaylistSummaryDto>> = _playlists.asStateFlow()

    private val _isLoadingPlaylists = MutableStateFlow(false)
    val isLoadingPlaylists: StateFlow<Boolean> = _isLoadingPlaylists.asStateFlow()

    init {
        loadPlaylists()
    }

    fun loadPlaylists() {
        val api = monkifyApi ?: return
        viewModelScope.launch {
            _isLoadingPlaylists.value = true
            try {
                _playlists.value = api.getPlaylists()
            } catch (_: Exception) {
                // Silencioso o sin conexión
            } finally {
                _isLoadingPlaylists.value = false
            }
        }
    }

    val favoriteTracks: StateFlow<List<Track>> = localTrackRepository.favoriteTracks
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val downloadedTracks: StateFlow<List<Track>> = localTrackRepository.downloadedTracks
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val displayedTracks: StateFlow<List<Track>> = combine(
        _selectedFilter,
        localTrackRepository.favoriteTracks,
        localTrackRepository.downloadedTracks
    ) { filter, favorites, downloads ->
        when (filter) {
            LibraryFilter.FAVORITES -> favorites
            LibraryFilter.DOWNLOADS -> downloads
            LibraryFilter.ALL, LibraryFilter.PLAYLISTS -> (favorites + downloads).distinctBy { it.id }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setFilter(filter: LibraryFilter) {
        _selectedFilter.value = filter
    }
}
