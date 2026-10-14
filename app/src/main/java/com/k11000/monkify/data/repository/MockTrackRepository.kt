package com.k11000.monkify.data.repository

import com.k11000.monkify.data.remote.dto.IngestResponse
import com.k11000.monkify.domain.model.Track
import com.k11000.monkify.domain.repository.TrackRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class MockTrackRepository @Inject constructor() : TrackRepository {

    private val mockTracks = listOf(
        Track(
            id = "1",
            title = "Random Access Memories",
            artist = "Daft Punk",
            album = "Random Access Memories",
            durationMs = 200000,
            coverUrl = "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=500&q=80",
            streamUrl = ""
        ),
        Track(
            id = "2",
            title = "Midnight City",
            artist = "M83",
            album = "Hurry Up, We're Dreaming",
            durationMs = 240000,
            coverUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500&q=80",
            streamUrl = ""
        ),
        Track(
            id = "3",
            title = "Kids",
            artist = "MGMT",
            album = "Oracular Spectacular",
            durationMs = 180000,
            coverUrl = "https://images.unsplash.com/photo-1493225457124-a1a2a5f5f5af?w=500&q=80",
            streamUrl = ""
        )
    )

    override fun getRecentTracks(): Flow<List<Track>> {
        return flowOf(mockTracks)
    }

    override fun getMadeForYouTracks(): Flow<List<Track>> {
        return flowOf(mockTracks.reversed())
    }

    override suspend fun getTracks(limit: Int, offset: Int): List<Track> {
        return mockTracks.drop(offset).take(limit)
    }

    override suspend fun requestIngest(url: String): Result<IngestResponse> {
        delay(1500) // Simular latencia de red
        return Result.success(
            IngestResponse(
                taskId = "task_${System.currentTimeMillis()}",
                status = "QUEUED",
                message = "Extracción encolada en segundo plano"
            )
        )
    }
}
