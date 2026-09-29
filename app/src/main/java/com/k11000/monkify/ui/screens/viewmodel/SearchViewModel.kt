package com.k11000.monkify.ui.screens.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.k11000.monkify.domain.repository.TrackRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class IngestUiState {
    object Idle : IngestUiState()
    object Loading : IngestUiState()
    data class Success(val message: String) : IngestUiState()
    data class Error(val message: String) : IngestUiState()
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val trackRepository: TrackRepository
) : ViewModel() {

    private val _ingestState = MutableStateFlow<IngestUiState>(IngestUiState.Idle)
    val ingestState: StateFlow<IngestUiState> = _ingestState.asStateFlow()

    fun ingestSpotifyUrl(url: String) {
        if (!url.contains("spotify.com")) {
            _ingestState.value = IngestUiState.Error("El enlace no es de Spotify")
            return
        }

        viewModelScope.launch {
            _ingestState.value = IngestUiState.Loading
            trackRepository.requestIngest(url)
                .onSuccess { response ->
                    _ingestState.value = IngestUiState.Success(response.message)
                }
                .onFailure {
                    _ingestState.value = IngestUiState.Error("Error al conectar con el servidor")
                }
        }
    }

    fun resetState() {
        _ingestState.value = IngestUiState.Idle
    }
}
