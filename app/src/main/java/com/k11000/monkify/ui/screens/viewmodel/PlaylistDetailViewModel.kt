package com.k11000.monkify.ui.screens.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.k11000.monkify.BuildConfig
import com.k11000.monkify.data.remote.api.MonkifyApi
import com.k11000.monkify.data.remote.dto.toDomain
import com.k11000.monkify.domain.model.Track
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    private val api: MonkifyApi
) : ViewModel() {

    private val _playlistName = MutableStateFlow("")
    val playlistName: StateFlow<String> = _playlistName.asStateFlow()

    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadPlaylist(playlistId: String, name: String? = null) {
        if (!name.isNullOrBlank()) {
            _playlistName.value = name
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val dtos = api.getPlaylistTracks(playlistId)
                _tracks.value = dtos.map { it.toDomain(BuildConfig.BASE_URL) }
            } catch (_: Exception) {
                _tracks.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
