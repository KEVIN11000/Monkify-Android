package com.k11000.monkify.ui.screens.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.k11000.monkify.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val cacheLimitMb: Long = 1024L,
    val cacheUsedBytes: Long = 0L,
    val dynamicColor: Boolean = false,
    val isClearingCache: Boolean = false,
    val message: String? = null,
    val zenModeEnabled: Boolean = true,
    val zenEngine: String = "AURORA"
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.cacheLimitMb.collect { limit ->
                _uiState.update { it.copy(cacheLimitMb = limit) }
            }
        }
        viewModelScope.launch {
            settingsRepository.dynamicColor.collect { dynamic ->
                _uiState.update { it.copy(dynamicColor = dynamic) }
            }
        }
        viewModelScope.launch {
            settingsRepository.zenModeEnabled.collect { enabled ->
                _uiState.update { it.copy(zenModeEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            settingsRepository.zenEngine.collect { engine ->
                _uiState.update { it.copy(zenEngine = engine) }
            }
        }
        refreshCacheUsage()
    }

    fun setCacheLimit(mb: Long) {
        viewModelScope.launch {
            settingsRepository.setCacheLimitMb(mb)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDynamicColor(enabled)
        }
    }

    fun setZenModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setZenModeEnabled(enabled)
        }
    }

    fun setZenEngine(engine: String) {
        viewModelScope.launch {
            settingsRepository.setZenEngine(engine)
        }
    }

    fun refreshCacheUsage() {
        viewModelScope.launch {
            val usedBytes = settingsRepository.getCacheUsedBytes()
            _uiState.update { it.copy(cacheUsedBytes = usedBytes) }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            _uiState.update { it.copy(isClearingCache = true) }
            settingsRepository.clearMediaCache()
            val usedBytes = settingsRepository.getCacheUsedBytes()
            _uiState.update {
                it.copy(
                    isClearingCache = false,
                    cacheUsedBytes = usedBytes,
                    message = "Caché de reproducción liberada"
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
