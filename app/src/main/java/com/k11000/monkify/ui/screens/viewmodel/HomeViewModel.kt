package com.k11000.monkify.ui.screens.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.k11000.monkify.domain.model.Track
import com.k11000.monkify.domain.repository.DownloadRepository
import com.k11000.monkify.domain.repository.TrackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.k11000.monkify.domain.model.SyncStatus
import com.k11000.monkify.domain.repository.SyncScheduler
import javax.inject.Inject

enum class HomeFilter(val displayName: String) {
    ALL("Todo"),
    MUSIC("Música"),
    PODCASTS("Podcasts")
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val trackRepository: TrackRepository,
    private val downloadRepository: DownloadRepository? = null,
    private val syncScheduler: SyncScheduler? = null
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(HomeFilter.ALL)
    val selectedFilter: StateFlow<HomeFilter> = _selectedFilter.asStateFlow()

    fun setFilter(filter: HomeFilter) {
        _selectedFilter.value = filter
    }

    val downloadedTrackIds: StateFlow<Set<String>> = downloadRepository?.downloadedTrackIds
        ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())
        ?: MutableStateFlow(emptySet())

    val downloadingTrackIds: StateFlow<Set<String>> = downloadRepository?.downloadingTrackIds
        ?: MutableStateFlow(emptySet())

    val syncStatus: StateFlow<SyncStatus> = syncScheduler?.syncStatus
        ?.stateIn(viewModelScope, SharingStarted.Eagerly, SyncStatus.IDLE)
        ?: MutableStateFlow(SyncStatus.IDLE)

    val lastSyncTimestamp: StateFlow<Long> = syncScheduler?.lastSyncTimestamp
        ?.stateIn(viewModelScope, SharingStarted.Eagerly, 0L)
        ?: MutableStateFlow(0L)

    fun requestSync() {
        syncScheduler?.scheduleImmediateSync()
    }

    fun toggleDownload(track: Track) {
        viewModelScope.launch {
            downloadRepository?.toggleDownload(track)
        }
    }

    private val _recentTracks = MutableStateFlow<List<Track>>(emptyList())
    val recentTracks: StateFlow<List<Track>> = _recentTracks.asStateFlow()

    private val _madeForYouTracks = MutableStateFlow<List<Track>>(emptyList())
    val madeForYouTracks: StateFlow<List<Track>> = _madeForYouTracks.asStateFlow()

    private val _feedTracks = MutableStateFlow<List<Track>>(emptyList())
    val feedTracks: StateFlow<List<Track>> = _feedTracks.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _hasMore = MutableStateFlow(true)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    init {
        loadData()
    }

    fun refresh() {
        _hasMore.value = true
        loadData()
        syncScheduler?.scheduleImmediateSync()
    }

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val tracks = trackRepository.getTracks(limit = PAGE_SIZE, offset = 0)
                if (tracks.isNotEmpty()) {
                    _feedTracks.value = tracks
                    _recentTracks.value = tracks.take(6)
                    _madeForYouTracks.value = if (tracks.size > 6) tracks.drop(6).take(10) else tracks.reversed()
                    _hasMore.value = tracks.size >= PAGE_SIZE
                } else {
                    // Fallback a los flows tradicionales si getTracks devolvió lista vacía
                    fallbackToFlows()
                }
            } catch (e: Exception) {
                fallbackToFlows()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun fallbackToFlows() {
        viewModelScope.launch {
            trackRepository.getRecentTracks().collect { tracks ->
                if (tracks.isNotEmpty()) {
                    _recentTracks.value = tracks.take(6)
                    _feedTracks.value = tracks
                }
            }
        }
        viewModelScope.launch {
            trackRepository.getMadeForYouTracks().collect { tracks ->
                if (tracks.isNotEmpty()) {
                    _madeForYouTracks.value = tracks
                }
            }
        }
    }

    fun loadMore() {
        if (_isLoadingMore.value || !_hasMore.value || _isLoading.value) return
        viewModelScope.launch {
            _isLoadingMore.value = true
            try {
                val currentOffset = _feedTracks.value.size
                val newTracks = trackRepository.getTracks(limit = PAGE_SIZE, offset = currentOffset)
                if (newTracks.isEmpty()) {
                    _hasMore.value = false
                } else {
                    _feedTracks.value = _feedTracks.value + newTracks
                    if (newTracks.size < PAGE_SIZE) {
                        _hasMore.value = false
                    }
                }
            } catch (e: Exception) {
                // Silencioso o reintentable en el siguiente scroll
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    companion object {
        const val PAGE_SIZE = 20
    }
}
