package com.k11000.monkify.ui.screens.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.k11000.monkify.BuildConfig
import com.k11000.monkify.data.remote.api.MonkifyApi
import com.k11000.monkify.data.remote.dto.toDomain
import com.k11000.monkify.domain.model.Track
import com.k11000.monkify.domain.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    private val api: MonkifyApi,
    private val playlistRepository: PlaylistRepository? = null
) : ViewModel() {

    private val _playlistName = MutableStateFlow("")
    val playlistName: StateFlow<String> = _playlistName.asStateFlow()

    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var currentPlaylistId: String? = null
    val currentId: String? get() = currentPlaylistId

    fun loadPlaylist(playlistId: String, name: String? = null) {
        currentPlaylistId = playlistId
        if (!name.isNullOrBlank()) {
            _playlistName.value = name
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (playlistRepository != null) {
                    val result = playlistRepository.getPlaylistTracks(playlistId)
                    result.fold(
                        onSuccess = { _tracks.value = it },
                        onFailure = {
                            val dtos = api.getPlaylistTracks(playlistId)
                            _tracks.value = dtos.map { it.toDomain(BuildConfig.BASE_URL) }
                        }
                    )
                } else {
                    val dtos = api.getPlaylistTracks(playlistId)
                    _tracks.value = dtos.map { it.toDomain(BuildConfig.BASE_URL) }
                }
            } catch (_: Exception) {
                _tracks.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun removeTrack(trackId: String, onResult: (Result<Unit>) -> Unit = {}) {
        val plId = currentPlaylistId ?: run {
            onResult(Result.failure(IllegalStateException("No hay playlist seleccionada")))
            return
        }
        viewModelScope.launch {
            try {
                if (playlistRepository != null) {
                    val res = playlistRepository.removeTrackFromPlaylist(plId, trackId)
                    res.onSuccess {
                        _tracks.value = _tracks.value.filter { it.id != trackId }
                    }
                    onResult(res)
                } else {
                    val resp = api.removeTrackFromPlaylist(plId, trackId)
                    if (resp.success) {
                        _tracks.value = _tracks.value.filter { it.id != trackId }
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

    fun deletePlaylist(onResult: (Result<Unit>) -> Unit = {}) {
        val plId = currentPlaylistId ?: run {
            onResult(Result.failure(IllegalStateException("No hay playlist seleccionada")))
            return
        }
        viewModelScope.launch {
            try {
                if (playlistRepository != null) {
                    val res = playlistRepository.deletePlaylist(plId)
                    onResult(res)
                } else {
                    val resp = api.deletePlaylist(plId)
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
}
