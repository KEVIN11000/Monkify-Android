package com.k11000.monkify.domain.repository

import com.k11000.monkify.data.remote.dto.PlaylistSummaryDto
import com.k11000.monkify.domain.model.Track

interface PlaylistRepository {
    suspend fun getPlaylists(limit: Int = 50, offset: Int = 0): Result<List<PlaylistSummaryDto>>
    suspend fun getPlaylistTracks(playlistId: String): Result<List<Track>>
    suspend fun createPlaylist(name: String, description: String? = null): Result<PlaylistSummaryDto>
    suspend fun deletePlaylist(playlistId: String): Result<Unit>
    suspend fun addTrackToPlaylist(playlistId: String, trackId: String): Result<Unit>
    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String): Result<Unit>
}
