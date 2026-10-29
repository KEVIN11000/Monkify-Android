package com.k11000.monkify.data.remote.api

import com.k11000.monkify.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Estado global de la conexión con el servidor Cloudflare con política Anti-Spam:
 * - No interrumpe con avisos flotantes durante navegación normal cuando la conexión está sana.
 * - Solo muestra avisos flotantes ante:
 *   1. Reconexión explícita / validación de token (`isExplicitAction = true`).
 *   2. Error de autorización (`HTTP 401/403` - Clave faltante o inválida).
 *   3. Caída de red / Timeout / Error 5xx (respetando si el usuario descartó el aviso para usar modo offline).
 *   4. Confirmación breve de recuperación (`Connected`) ÚNICAMENTE cuando se sale de un estado de error o test manual.
 */
sealed interface GlobalServerStatus {
    data object Idle : GlobalServerStatus

    data class Connecting(
        val message: String = "Conectando con el servidor..."
    ) : GlobalServerStatus

    data class Connected(
        val message: String = "Conectado al servidor",
        val timestampMs: Long = System.currentTimeMillis()
    ) : GlobalServerStatus

    data class Unauthorized(
        val reason: String = "Acceso denegado (HTTP 401): Configura o verifica tu Clave de Servidor en Ajustes"
    ) : GlobalServerStatus

    data class Error(
        val reason: String
    ) : GlobalServerStatus
}

@Singleton
class ServerStatusMonitor @Inject constructor() {

    private val _status = MutableStateFlow<GlobalServerStatus>(GlobalServerStatus.Idle)
    val status: StateFlow<GlobalServerStatus> = _status.asStateFlow()

    /**
     * Indica si el usuario venía de un fallo (401 / Error) o de una prueba explícita de conexión.
     * Evita mostrar el banner verde "Conectado" en cada petición rutinaria de paginación o búsqueda.
     */
    @Volatile
    private var needsRecoveryConfirmation: Boolean = false

    /**
     * Si el usuario cierra manualmente (✕) el aviso de "Sin conexión (Modo local)",
     * no volvemos a molestar con el mismo error de red en segundo plano salvo acción explícita.
     */
    @Volatile
    private var offlineBannerDismissedByUser: Boolean = false

    /**
     * Reporta inicio de conexión.
     * @param isExplicit true solo cuando el usuario pulsa "Guardar Token", "Probar Conexión" o "Reintentar".
     * En cargas rutinarias en segundo plano (`isExplicit = false`), no muestra píldora flotante
     * a menos que estemos saliendo de un error previo.
     */
    fun reportConnecting(
        message: String = "Conectando con el servidor...",
        isExplicit: Boolean = false
    ) {
        if (isExplicit) {
            offlineBannerDismissedByUser = false
            needsRecoveryConfirmation = true
            updateState(GlobalServerStatus.Connecting(message))
        }
    }

    /**
     * Reporta éxito de conexión.
     * Solo emite el aviso flotante `Connected` si veníamos de un error (`401`/`Error`) o de un test explícito;
     * de lo contrario mantiene `Idle` para una experiencia limpia y sin ruido visual.
     */
    fun reportConnected(
        message: String = "Conectado al servidor Monkify",
        forceBanner: Boolean = false
    ) {
        offlineBannerDismissedByUser = false
        val current = _status.value
        val wasUnhealthy = current is GlobalServerStatus.Unauthorized ||
            current is GlobalServerStatus.Error ||
            current is GlobalServerStatus.Connecting

        if (forceBanner || needsRecoveryConfirmation || wasUnhealthy) {
            needsRecoveryConfirmation = false
            updateState(
                GlobalServerStatus.Connected(
                    message = message,
                    timestampMs = System.currentTimeMillis()
                )
            )
        } else if (current !is GlobalServerStatus.Idle) {
            updateState(GlobalServerStatus.Idle)
        }
    }

    fun reportUnauthorized(
        reason: String = "Error 401: Clave de servidor ausente o rechazada. Revísala en Ajustes."
    ) {
        needsRecoveryConfirmation = true
        updateState(GlobalServerStatus.Unauthorized(reason))
        AppLogger.w(TAG, reason)
    }

    fun reportError(reason: String, isExplicit: Boolean = false) {
        if (offlineBannerDismissedByUser && !isExplicit) {
            AppLogger.w(TAG, "Aviso flotante omitido (descartado por usuario): $reason")
            return
        }
        needsRecoveryConfirmation = true
        updateState(GlobalServerStatus.Error(reason))
        AppLogger.w(TAG, reason)
    }

    fun reportException(operation: String, throwable: Throwable, isExplicit: Boolean = false) {
        when (throwable) {
            is HttpException -> {
                val code = throwable.code()
                if (code == 401 || code == 403) {
                    reportUnauthorized(
                        "Error HTTP $code: Clave de servidor inválida o no configurada"
                    )
                } else {
                    reportError(
                        "Fallo del servidor (HTTP $code) en $operation · Usando caché local",
                        isExplicit = isExplicit
                    )
                }
            }
            is SocketTimeoutException -> {
                reportError(
                    "Tiempo de espera agotado ($operation) · Usando caché local",
                    isExplicit = isExplicit
                )
            }
            is UnknownHostException -> {
                reportError(
                    "Sin conexión al servidor · Modo offline activo",
                    isExplicit = isExplicit
                )
            }
            is IOException -> {
                reportError(
                    "Sin conexión con el servidor ($operation) · Modo local",
                    isExplicit = isExplicit
                )
            }
            else -> {
                reportError(
                    "Error en $operation: ${throwable.localizedMessage ?: throwable.javaClass.simpleName}",
                    isExplicit = isExplicit
                )
            }
        }
    }

    fun dismiss(byUser: Boolean = true) {
        if (byUser && _status.value is GlobalServerStatus.Error) {
            offlineBannerDismissedByUser = true
        }
        updateState(GlobalServerStatus.Idle)
    }

    private fun updateState(next: GlobalServerStatus) {
        _status.value = next
        if (this !== shared) {
            shared._status.value = next
        }
    }

    companion object {
        private const val TAG = "ServerStatusMonitor"

        /** Instancia global compartida para acceso directo desde UI y repositorios. */
        val shared = ServerStatusMonitor()
    }
}
