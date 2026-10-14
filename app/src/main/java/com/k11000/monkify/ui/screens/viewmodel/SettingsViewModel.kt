package com.k11000.monkify.ui.screens.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.k11000.monkify.data.repository.SettingsRepository
import com.k11000.monkify.domain.model.AppUpdate
import com.k11000.monkify.domain.repository.UpdateRepository
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
    val zenEngine: String = "AURORA",
    val lockScreenEnabled: Boolean = false,
    val isCheckingUpdate: Boolean = false,
    val availableUpdate: AppUpdate? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository?,
    private val updateRepository: UpdateRepository
) : ViewModel() {

    constructor(updateRepository: UpdateRepository) : this(null, updateRepository)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        settingsRepository?.let { repo ->
            viewModelScope.launch {
                repo.cacheLimitMb.collect { limit ->
                    _uiState.update { it.copy(cacheLimitMb = limit) }
                }
            }
            viewModelScope.launch {
                repo.dynamicColor.collect { dynamic ->
                    _uiState.update { it.copy(dynamicColor = dynamic) }
                }
            }
            viewModelScope.launch {
                repo.zenModeEnabled.collect { enabled ->
                    _uiState.update { it.copy(zenModeEnabled = enabled) }
                }
            }
            viewModelScope.launch {
                repo.zenEngine.collect { engine ->
                    _uiState.update { it.copy(zenEngine = engine) }
                }
            }
            viewModelScope.launch {
                repo.lockScreenEnabled.collect { lockEnabled ->
                    _uiState.update { it.copy(lockScreenEnabled = lockEnabled) }
                }
            }
            refreshCacheUsage()
        }
    }

    fun setCacheLimit(mb: Long) {
        viewModelScope.launch {
            settingsRepository?.setCacheLimitMb(mb)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository?.setDynamicColor(enabled)
        }
    }

    fun setZenModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository?.setZenModeEnabled(enabled)
        }
    }

    fun setZenEngine(engine: String) {
        viewModelScope.launch {
            settingsRepository?.setZenEngine(engine)
        }
    }

    fun setLockScreenEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository?.setLockScreenEnabled(enabled)
        }
    }

    fun refreshCacheUsage() {
        viewModelScope.launch {
            val usedBytes = settingsRepository?.getCacheUsedBytes() ?: 0L
            _uiState.update { it.copy(cacheUsedBytes = usedBytes) }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            _uiState.update { it.copy(isClearingCache = true) }
            settingsRepository?.clearMediaCache()
            val usedBytes = settingsRepository?.getCacheUsedBytes() ?: 0L
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

    fun checkForUpdates(manual: Boolean = true) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingUpdate = true) }
            val result = updateRepository.checkForUpdate()
            result.fold(
                onSuccess = { update ->
                    if (update.isUpdateAvailable) {
                        _uiState.update {
                            it.copy(
                                isCheckingUpdate = false,
                                availableUpdate = update
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isCheckingUpdate = false,
                                message = if (manual) "Monkify está al día (versión ${update.currentVersionName})" else null
                            )
                        }
                    }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            isCheckingUpdate = false,
                            message = if (manual) "No se pudo verificar actualizaciones. Comprueba tu conexión." else null
                        )
                    }
                }
            )
        }
    }

    fun dismissUpdateDialog() {
        _uiState.update { it.copy(availableUpdate = null) }
    }
}
