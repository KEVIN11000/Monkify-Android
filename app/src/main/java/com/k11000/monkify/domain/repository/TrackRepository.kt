package com.k11000.monkify.domain.repository

import com.k11000.monkify.data.remote.dto.IngestResponse
import com.k11000.monkify.domain.model.Track
import kotlinx.coroutines.flow.Flow

interface TrackRepository {
    fun getRecentTracks(): Flow<List<Track>>
    fun getMadeForYouTracks(): Flow<List<Track>>
    suspend fun requestIngest(url: String): Result<IngestResponse>
}
