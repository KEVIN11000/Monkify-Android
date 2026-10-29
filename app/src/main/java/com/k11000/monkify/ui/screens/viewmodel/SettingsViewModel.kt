package com.k11000.monkify.ui.screens.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.k11000.monkify.data.remote.api.MonkifyApi
import com.k11000.monkify.data.remote.api.ServerStatusMonitor
import com.k11000.monkify.data.remote.dto.SupportReportRequestDto
import com.k11000.monkify.data.repository.SettingsRepository
import com.k11000.monkify.domain.model.AppUpdate
import com.k11000.monkify.domain.repository.UpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

sealed interface ServerConnectionTestState {
    data object Idle : ServerConnectionTestState
    data object Testing : ServerConnectionTestState
    data class Success(val message: String) : ServerConnectionTestState
    data class Error(val message: String) : ServerConnectionTestState
}

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
    val serverAccessToken: String = "",
    val connectionTestState: ServerConnectionTestState = ServerConnectionTestState.Idle
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
            if (trimmed.isBlank()) {
                _uiState.update {
                    it.copy(
                        serverAccessToken = "",
                        connectionTestState = ServerConnectionTestState.Idle,
                        message = "Clave de servidor eliminada"
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    serverAccessToken = trimmed,
                    message = "Clave de acceso al servidor guardada"
                )
            }

            if (monkifyApi != null) {
                verifyServerConnectionInternal()
            }
        }
    }

    fun testServerConnection(tokenInput: String? = null) {
        viewModelScope.launch {
            if (tokenInput != null) {
                val trimmed = tokenInput.trim()
                settingsRepository?.setServerAccessToken(trimmed)
                _uiState.update { it.copy(serverAccessToken = trimmed) }
            }
            verifyServerConnectionInternal()
        }
    }

    private suspend fun verifyServerConnectionInternal() {
        val api = monkifyApi ?: return
        _uiState.update { it.copy(connectionTestState = ServerConnectionTestState.Testing) }
        ServerStatusMonitor.shared.reportConnecting(
            "Verificando clave con el servidor Cloudflare...",
            isExplicit = true
        )
        try {
            val sample = api.getTracks(limit = 1, offset = 0)
            val okMsg = if (sample.isNotEmpty()) {
                "✓ Conectado y autenticado con éxito (Catálogo activo)"
            } else {
                "✓ Conectado al servidor con éxito"
            }
            _uiState.update {
                it.copy(
                    connectionTestState = ServerConnectionTestState.Success(okMsg),
                    message = okMsg
                )
            }
            ServerStatusMonitor.shared.reportConnected(okMsg, forceBanner = true)
        } catch (e: HttpException) {
            val code = e.code()
            val errMsg = if (code == 401 || code == 403) {
                "✗ Error HTTP $code: La clave ingresada no coincide con la del servidor"
            } else {
                "✗ Error del servidor (HTTP $code)"
            }
            _uiState.update {
                it.copy(
                    connectionTestState = ServerConnectionTestState.Error(errMsg),
                    message = errMsg
                )
            }
            ServerStatusMonitor.shared.reportException("Verificación de clave", e, isExplicit = true)
        } catch (e: Exception) {
            val errMsg = "✗ Sin respuesta del servidor: ${e.localizedMessage ?: "Comprueba tu conexión"}"
            _uiState.update {
                it.copy(
                    connectionTestState = ServerConnectionTestState.Error(errMsg),
                    message = errMsg
                )
            }
            ServerStatusMonitor.shared.reportException("Verificación de clave", e, isExplicit = true)
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
