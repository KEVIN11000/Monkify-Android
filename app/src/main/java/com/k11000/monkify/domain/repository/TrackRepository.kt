package com.k11000.monkify.domain.repository

import com.k11000.monkify.data.remote.dto.IngestResponse
import com.k11000.monkify.domain.model.Track
import kotlinx.coroutines.flow.Flow

interface TrackRepository {
    fun getRecentTracks(): Flow<List<Track>>
    fun getMadeForYouTracks(): Flow<List<Track>>
    suspend fun getTracks(limit: Int = 20, offset: Int = 0, query: String? = null): List<Track>
    suspend fun getLatestUploadedTracks(limit: Int = 6): List<Track> = emptyList()
    suspend fun recordTrackPlayed(trackId: String) {}
    suspend fun requestIngest(url: String): Result<IngestResponse>
}
