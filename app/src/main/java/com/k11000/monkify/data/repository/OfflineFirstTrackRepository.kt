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
        trackDao.getRecentlyPlayedTracks(limit = PAGE_SIZE).collect { history ->
            if (history.isNotEmpty()) {
                emit(history.map { it.toDomain() })
            } else {
                val cached = trackDao.getPagedTracks(limit = PAGE_SIZE, offset = 0).map { it.toDomain() }
                if (cached.isNotEmpty()) {
                    emit(cached)
                } else {
                    try {
                        val remote = networkTrackRepository.getTracks(limit = PAGE_SIZE, offset = 0)
                        if (remote.isNotEmpty()) {
                            val entities = remote.map { it.toEntity(isFavorite = false, isDownloaded = false) }
                            trackDao.insertTracks(entities)
                        }
                    } catch (_: Exception) {
                        emit(emptyList())
                    }
                }
            }
        }
    }

    override fun getMadeForYouTracks(): Flow<List<Track>> = flow {
        trackDao.getTracksOrderedByPlayCount().collect { topEntities ->
            val topArtists = topEntities.filter { it.playCount > 0 }.map { it.artist.trim() }.distinct().take(5)
            if (topArtists.isNotEmpty()) {
                val allTracks = trackDao.getAllTracksSnapshot()
                val recommended = allTracks.filter { it.artist.trim() in topArtists }.shuffled().take(PAGE_SIZE).map { it.toDomain() }
                if (recommended.isNotEmpty()) {
                    emit(recommended)
                    return@collect
                }
            }
            val cached = trackDao.getPagedTracks(limit = PAGE_SIZE, offset = 0).map { it.toDomain() }
            if (cached.isNotEmpty()) {
                emit(cached.reversed())
            } else {
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

    override suspend fun getLatestUploadedTracks(limit: Int): List<Track> {
        val remote = networkTrackRepository.getLatestUploadedTracks(limit = limit)
        if (remote.isNotEmpty()) {
            return remote
        }
        return trackDao.getPagedTracks(limit = limit, offset = 0).map { it.toDomain() }
    }

    override suspend fun recordTrackPlayed(trackId: String) {
        trackDao.recordTrackPlay(trackId, System.currentTimeMillis())
    }

    override suspend fun requestIngest(url: String): Result<IngestResponse> {
        return networkTrackRepository.requestIngest(url)
    }

    private companion object {
        const val PAGE_SIZE = 20
    }
}
