package com.k11000.monkify.data.repository

import com.k11000.monkify.data.local.dao.TrackDao
import com.k11000.monkify.data.local.entity.toDomain
import com.k11000.monkify.data.local.entity.toEntity
import com.k11000.monkify.domain.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalTrackRepository @Inject constructor(
    private val trackDao: TrackDao
) {

    val favoriteTracks: Flow<List<Track>> = trackDao.getAllFavoriteTracks().map { entities ->
        entities.map { it.toDomain() }
    }

    val downloadedTracks: Flow<List<Track>> = trackDao.getDownloadedTracks().map { entities ->
        entities.map { it.toDomain() }
    }

    fun isFavorite(trackId: String): Flow<Boolean> = trackDao.isFavorite(trackId)

    suspend fun toggleFavorite(track: Track) {
        val existing = trackDao.getTrackById(track.id)
        if (existing == null) {
            trackDao.insertTrack(track.toEntity(isFavorite = true))
        } else {
            val newFav = !existing.isFavorite
            if (!newFav && !existing.isDownloaded) {
                trackDao.deleteTrackById(track.id)
            } else {
                trackDao.updateFavoriteStatus(track.id, newFav)
            }
        }
    }

    suspend fun saveTrack(track: Track, isFavorite: Boolean = true, isDownloaded: Boolean = false, localPath: String? = null) {
        trackDao.insertTrack(track.toEntity(isFavorite = isFavorite, isDownloaded = isDownloaded, localFilePath = localPath))
    }

    suspend fun markAsDownloaded(track: Track, localFilePath: String) {
        val existing = trackDao.getTrackById(track.id)
        if (existing == null) {
            trackDao.insertTrack(track.toEntity(isFavorite = false, isDownloaded = true, localFilePath = localFilePath))
        } else {
            trackDao.updateDownloadStatus(track.id, isDownloaded = true, localPath = localFilePath)
        }
    }

    suspend fun removeDownload(trackId: String) {
        val existing = trackDao.getTrackById(trackId) ?: return
        if (!existing.isFavorite) {
            trackDao.deleteTrackById(trackId)
        } else {
            trackDao.updateDownloadStatus(trackId, isDownloaded = false, localPath = null)
        }
    }

    suspend fun syncRemoteCatalog(remoteTracks: List<Track>): Int {
        val existingMap = trackDao.getAllTracksSnapshot().associateBy { it.id }
        val entities = remoteTracks.map { track ->
            val local = existingMap[track.id]
            com.k11000.monkify.data.local.entity.TrackEntity(
                id = track.id,
                title = track.title,
                artist = track.artist,
                album = track.album,
                durationMs = track.durationMs,
                coverUrl = track.coverUrl,
                streamUrl = track.streamUrl,
                isFavorite = local?.isFavorite ?: false,
                isDownloaded = local?.isDownloaded ?: false,
                localFilePath = local?.localFilePath
            )
        }
        entities.forEach { trackDao.insertTrack(it) }
        return entities.size
    }
}

