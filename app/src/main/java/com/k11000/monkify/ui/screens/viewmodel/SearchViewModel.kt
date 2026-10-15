package com.k11000.monkify.ui.screens.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.k11000.monkify.domain.model.Track
import com.k11000.monkify.domain.repository.TrackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class IngestUiState {
    object Idle : IngestUiState()
    object Loading : IngestUiState()
    data class Success(val message: String) : IngestUiState()
    data class Error(val message: String) : IngestUiState()
}

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val trackRepository: TrackRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Track>>(emptyList())
    val searchResults: StateFlow<List<Track>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _selectedTrack = MutableStateFlow<Track?>(null)
    val selectedTrack: StateFlow<Track?> = _selectedTrack.asStateFlow()

    private val _ingestState = MutableStateFlow<IngestUiState>(IngestUiState.Idle)
    val ingestState: StateFlow<IngestUiState> = _ingestState.asStateFlow()

    init {
        observeSearchQuery()
    }

    private fun observeSearchQuery() {
        viewModelScope.launch {
            _searchQuery
                .debounce(400L)
                .distinctUntilChanged()
                .collectLatest { query ->
                    val trimmed = query.trim()
                    if (trimmed.isEmpty()) {
                        _searchResults.value = emptyList()
                        _isSearching.value = false
                    } else {
                        _isSearching.value = true
                        try {
                            val results = trackRepository.getTracks(limit = 30, offset = 0, query = trimmed)
                            _searchResults.value = results
                        } catch (e: Exception) {
                            _searchResults.value = emptyList()
                        } finally {
                            _isSearching.value = false
                        }
                    }
                }
        }
    }

    fun onQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun clearQuery() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _isSearching.value = false
    }

    fun onTrackSelected(track: Track) {
        _selectedTrack.value = track
    }

    fun ingestMediaUrl(url: String) {
        val trimmed = url.trim()
        val isValid = trimmed.contains("spotify.com") ||
                      trimmed.contains("youtube.com") ||
                      trimmed.contains("youtu.be")
        if (!isValid) {
            _ingestState.value = IngestUiState.Error("El enlace debe ser de Spotify o YouTube")
            return
        }

        viewModelScope.launch {
            _ingestState.value = IngestUiState.Loading
            trackRepository.requestIngest(trimmed)
                .onSuccess { response ->
                    _ingestState.value = IngestUiState.Success(response.message)
                }
                .onFailure {
                    _ingestState.value = IngestUiState.Error("Error al conectar con el servidor")
                }
        }
    }

    fun ingestSpotifyUrl(url: String) = ingestMediaUrl(url)

    fun resetState() {
        _ingestState.value = IngestUiState.Idle
    }
}
