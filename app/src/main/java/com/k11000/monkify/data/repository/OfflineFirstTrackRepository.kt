package com.k11000.monkify.data.repository

import com.k11000.monkify.data.local.dao.TrackDao
import com.k11000.monkify.data.local.entity.toDomain
import com.k11000.monkify.data.local.entity.toEntity
import com.k11000.monkify.data.remote.dto.IngestResponse
import com.k11000.monkify.domain.model.Track
import com.k11000.monkify.domain.repository.TrackRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineFirstTrackRepository @Inject constructor(
    private val networkTrackRepository: NetworkTrackRepository,
    private val trackDao: TrackDao
) : TrackRepository {

    override fun getRecentTracks(): Flow<List<Track>> = flow {
        // 1. Emitir inmediatamente el contenido local persistido en Room
        val cached = trackDao.getPagedTracks(limit = PAGE_SIZE, offset = 0).map { it.toDomain() }
        if (cached.isNotEmpty()) {
            emit(cached)
        }

        // 2. Sincronizar en segundo plano con el backend
        try {
            val remote = networkTrackRepository.getTracks(limit = PAGE_SIZE, offset = 0)
            if (remote.isNotEmpty()) {
                val entities = remote.map { it.toEntity(isFavorite = false, isDownloaded = false) }
                trackDao.insertTracks(entities)
                val updated = trackDao.getPagedTracks(limit = PAGE_SIZE, offset = 0).map { it.toDomain() }
                emit(updated)
            }
        } catch (_: Exception) {
            // En caso de fallo de red, si no se había emitido nada se emite la lista disponible
            if (cached.isEmpty()) {
                emit(emptyList())
            }
        }
    }

    override fun getMadeForYouTracks(): Flow<List<Track>> = flow {
        val cached = trackDao.getPagedTracks(limit = PAGE_SIZE, offset = 0).map { it.toDomain() }
        if (cached.isNotEmpty()) {
            emit(cached.reversed())
        }

        try {
            val remote = networkTrackRepository.getTracks(limit = PAGE_SIZE, offset = 0)
            if (remote.isNotEmpty()) {
                val entities = remote.map { it.toEntity(isFavorite = false, isDownloaded = false) }
                trackDao.insertTracks(entities)
                val updated = trackDao.getPagedTracks(limit = PAGE_SIZE, offset = 0).map { it.toDomain() }
                emit(updated.reversed())
            }
        } catch (_: Exception) {
            if (cached.isEmpty()) {
                emit(emptyList())
            }
        }
    }

    override suspend fun getTracks(limit: Int, offset: Int, query: String?): List<Track> {
        val trimmedQuery = query?.trim()
        val isSearching = !trimmedQuery.isNullOrEmpty()

        try {
            val remote = networkTrackRepository.getTracks(limit = limit, offset = offset, query = trimmedQuery)
            if (remote.isNotEmpty()) {
                val entities = remote.map { it.toEntity(isFavorite = false, isDownloaded = false) }
                trackDao.insertTracks(entities)
                return remote
            }
        } catch (_: Exception) {
            // Continuar al fallback de Room
        }

        // Fallback local: recuperar del almacenamiento persistente de Room
        return if (isSearching) {
            trackDao.searchTracks(query = trimmedQuery!!, limit = limit, offset = offset).map { it.toDomain() }
        } else {
            trackDao.getPagedTracks(limit = limit, offset = offset).map { it.toDomain() }
        }
    }

    override suspend fun requestIngest(url: String): Result<IngestResponse> {
        return networkTrackRepository.requestIngest(url)
    }

    private companion object {
        const val PAGE_SIZE = 20
    }
}
