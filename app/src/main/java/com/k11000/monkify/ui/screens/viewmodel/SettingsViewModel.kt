package com.k11000.monkify.ui.screens.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.k11000.monkify.data.repository.SettingsRepository
import com.k11000.monkify.domain.model.AppUpdate
import com.k11000.monkify.domain.repository.UpdateRepository
import com.k11000.monkify.data.remote.api.MonkifyApi
import com.k11000.monkify.data.remote.dto.SupportReportRequestDto
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
    val availableUpdate: AppUpdate? = null,
    val isSubmittingReport: Boolean = false,
    val serverAccessToken: String = ""
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository?,
    private val updateRepository: UpdateRepository,
    private val monkifyApi: MonkifyApi? = null
) : ViewModel() {

    constructor(updateRepository: UpdateRepository) : this(null, updateRepository, null)

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
            viewModelScope.launch {
                repo.serverAccessToken.collect { token ->
                    _uiState.update { it.copy(serverAccessToken = token) }
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

    fun setServerAccessToken(token: String) {
        viewModelScope.launch {
            val trimmed = token.trim()
            settingsRepository?.setServerAccessToken(trimmed)
            _uiState.update {
                it.copy(
                    serverAccessToken = trimmed,
                    message = if (trimmed.isNotBlank()) "Clave de acceso al servidor guardada" else "Clave de servidor eliminada"
                )
            }
        }
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

    fun submitOneClickReport(
        category: String,
        message: String,
        logs: String?,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingReport = true) }
            try {
                val api = monkifyApi
                if (api == null) {
                    _uiState.update { it.copy(isSubmittingReport = false) }
                    onResult(false, "Servicio de soporte no disponible temporalmente")
                    return@launch
                }
                val deviceSummary = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE}, SDK ${android.os.Build.VERSION.SDK_INT})"
                val diagnosticsText = buildString {
                    appendLine("Category: $category")
                    appendLine("Device: $deviceSummary")
                    if (!logs.isNullOrBlank()) {
                        appendLine("--- LOGS ---")
                        appendLine(logs)
                    }
                }
                val request = SupportReportRequestDto(
                    message = message,
                    diagnostics = diagnosticsText,
                    appVersion = "0.4.0",
                    deviceModel = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
                )
                val response = api.submitSupportReport(request)
                _uiState.update { it.copy(isSubmittingReport = false) }
                if (response.ok) {
                    onResult(true, "¡Reporte enviado exitosamente! Gracias por tu feedback.")
                } else {
                    onResult(false, "No se pudo procesar el reporte")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSubmittingReport = false) }
                onResult(false, e.localizedMessage ?: "Error de conexión al enviar reporte")
            }
        }
    }
}
