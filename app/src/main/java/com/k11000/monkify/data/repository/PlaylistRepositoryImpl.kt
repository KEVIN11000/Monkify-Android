package com.k11000.monkify.data.repository

import com.k11000.monkify.BuildConfig
import com.k11000.monkify.data.remote.api.MonkifyApi
import com.k11000.monkify.data.remote.dto.AddTrackToPlaylistRequest
import com.k11000.monkify.data.remote.dto.CreatePlaylistRequest
import com.k11000.monkify.data.remote.dto.PlaylistSummaryDto
import com.k11000.monkify.data.remote.dto.toDomain
import com.k11000.monkify.domain.model.Track
import com.k11000.monkify.domain.repository.PlaylistRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepositoryImpl @Inject constructor(
    private val api: MonkifyApi
) : PlaylistRepository {

    override suspend fun getPlaylists(limit: Int, offset: Int): Result<List<PlaylistSummaryDto>> {
        return runCatching {
            api.getPlaylists(limit, offset)
        }
    }

    override suspend fun getPlaylistTracks(playlistId: String): Result<List<Track>> {
        return runCatching {
            val dtos = api.getPlaylistTracks(playlistId)
            dtos.map { it.toDomain(BuildConfig.BASE_URL) }
        }
    }

    override suspend fun createPlaylist(name: String, description: String?): Result<PlaylistSummaryDto> {
        return runCatching {
            require(name.isNotBlank()) { "El nombre de la playlist no puede estar vacío" }
            api.createPlaylist(CreatePlaylistRequest(name.trim(), description?.trim()))
        }
    }

    override suspend fun deletePlaylist(playlistId: String): Result<Unit> {
        return runCatching {
            val response = api.deletePlaylist(playlistId)
            check(response.success) { response.message ?: "Error al eliminar la playlist" }
        }
    }

    override suspend fun addTrackToPlaylist(playlistId: String, trackId: String): Result<Unit> {
        return runCatching {
            val response = api.addTrackToPlaylist(playlistId, AddTrackToPlaylistRequest(trackId))
            check(response.success) { response.message ?: "Error al agregar la pista a la playlist" }
        }
    }

    override suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String): Result<Unit> {
        return runCatching {
            val response = api.removeTrackFromPlaylist(playlistId, trackId)
            check(response.success) { response.message ?: "Error al remover la pista de la playlist" }
        }
    }
}
