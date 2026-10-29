package com.k11000.monkify.data.repository

import com.k11000.monkify.BuildConfig
import com.k11000.monkify.data.remote.api.MonkifyApi
import com.k11000.monkify.data.remote.api.ServerStatusMonitor
import com.k11000.monkify.data.remote.dto.IngestRequest
import com.k11000.monkify.data.remote.dto.IngestResponse
import com.k11000.monkify.data.remote.dto.toDomain
import com.k11000.monkify.domain.model.Track
import com.k11000.monkify.domain.repository.TrackRepository
import com.k11000.monkify.util.AppLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

open class NetworkTrackRepository(
    private val api: MonkifyApi,
    private val baseUrl: String,
    private val statusMonitor: ServerStatusMonitor = ServerStatusMonitor.shared
) : TrackRepository {

    @Inject
    constructor(
        api: MonkifyApi,
        statusMonitor: ServerStatusMonitor
    ) : this(api, BuildConfig.BASE_URL, statusMonitor)

    constructor(api: MonkifyApi) : this(api, BuildConfig.BASE_URL, ServerStatusMonitor.shared)

    private suspend fun fetchTracks(limit: Int, offset: Int, query: String? = null): List<Track> {
        val opLabel = if (!query.isNullOrBlank()) "Búsqueda '$query'" else "Sincronización de catálogo"
        statusMonitor.reportConnecting("$opLabel en el servidor...")
        return try {
            val tracks = api.getTracks(limit = limit, offset = offset, query = query)
                .map { it.toDomain(baseUrl) }
            statusMonitor.reportConnected("Conectado al servidor Monkify")
            tracks
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLogger.e(TAG, "Fallo en fetchTracks (limit=$limit, offset=$offset, query=$query): ${e.message}", e)
            statusMonitor.reportException(opLabel, e)
            emptyList()
        }
    }

    override fun getRecentTracks(): Flow<List<Track>> = flow {
        emit(fetchTracks(limit = PAGE_SIZE, offset = 0))
    }

    override fun getMadeForYouTracks(): Flow<List<Track>> = flow {
        emit(fetchTracks(limit = PAGE_SIZE, offset = 0).reversed())
    }

    override suspend fun getTracks(limit: Int, offset: Int, query: String?): List<Track> =
        fetchTracks(limit = limit, offset = offset, query = query)

    override suspend fun getLatestUploadedTracks(limit: Int): List<Track> {
        statusMonitor.reportConnecting("Cargando novedades del servidor...")
        return try {
            val tracks = api.getTracks(limit = limit, offset = 0, sort = "latest")
                .map { it.toDomain(baseUrl) }
            statusMonitor.reportConnected("Conectado al servidor Monkify")
            tracks
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLogger.e(TAG, "Fallo en getLatestUploadedTracks: ${e.message}", e)
            statusMonitor.reportException("Carga de novedades", e)
            emptyList()
        }
    }

    override suspend fun requestIngest(url: String): Result<IngestResponse> =
        try {
            statusMonitor.reportConnecting("Enviando enlace al servidor...")
            val response = api.ingest(IngestRequest(url))
            val body = response.body()
            if (response.isSuccessful && body != null) {
                statusMonitor.reportConnected("Enlace procesado por el servidor")
                Result.success(body)
            } else {
                val code = response.code()
                if (code == 401 || code == 403) {
                    statusMonitor.reportUnauthorized("Error HTTP $code al importar: Clave de servidor inválida")
                } else {
                    statusMonitor.reportError("Error HTTP $code al importar pista")
                }
                Result.failure(IllegalStateException("Ingest HTTP $code"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLogger.e(TAG, "Fallo en requestIngest: ${e.message}", e)
            statusMonitor.reportException("Importación de enlace", e)
            Result.failure(e)
        }

    private companion object {
        const val TAG = "NetworkTrackRepo"
        const val PAGE_SIZE = 20
    }
}
