package com.k11000.monkify.data.repository

import com.k11000.monkify.BuildConfig
import com.k11000.monkify.data.remote.api.MonkifyApi
import com.k11000.monkify.data.remote.dto.IngestRequest
import com.k11000.monkify.data.remote.dto.IngestResponse
import com.k11000.monkify.data.remote.dto.toDomain
import com.k11000.monkify.domain.model.Track
import com.k11000.monkify.domain.repository.TrackRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

open class NetworkTrackRepository(
    private val api: MonkifyApi,
    private val baseUrl: String
) : TrackRepository {

    @Inject
    constructor(api: MonkifyApi) : this(api, BuildConfig.BASE_URL)

    private suspend fun fetchTracks(limit: Int, offset: Int, query: String? = null): List<Track> =
        try {
            api.getTracks(limit = limit, offset = offset, query = query).map { it.toDomain(baseUrl) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }

    override fun getRecentTracks(): Flow<List<Track>> = flow {
        emit(fetchTracks(limit = PAGE_SIZE, offset = 0))
    }

    override fun getMadeForYouTracks(): Flow<List<Track>> = flow {
        emit(fetchTracks(limit = PAGE_SIZE, offset = 0).reversed())
    }

    override suspend fun getTracks(limit: Int, offset: Int, query: String?): List<Track> =
        fetchTracks(limit = limit, offset = offset, query = query)

    override suspend fun getLatestUploadedTracks(limit: Int): List<Track> =
        try {
            api.getTracks(limit = limit, offset = 0, sort = "latest").map { it.toDomain(baseUrl) }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }

    override suspend fun requestIngest(url: String): Result<IngestResponse> =
        try {
            val response = api.ingest(IngestRequest(url))
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(IllegalStateException("Ingest HTTP ${response.code()}"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    private companion object {
        const val PAGE_SIZE = 20
    }
}
