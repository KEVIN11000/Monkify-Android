package com.k11000.monkify.data.repository

import com.k11000.monkify.data.local.dao.TrackDao
import com.k11000.monkify.data.local.entity.TrackEntity
import com.k11000.monkify.data.remote.api.MonkifyApi
import com.k11000.monkify.data.remote.dto.ActionResponseDto
import com.k11000.monkify.data.remote.dto.AddTrackToPlaylistRequest
import com.k11000.monkify.data.remote.dto.BackendTrackDto
import com.k11000.monkify.data.remote.dto.CreatePlaylistRequest
import com.k11000.monkify.data.remote.dto.IngestRequest
import com.k11000.monkify.data.remote.dto.IngestResponse
import com.k11000.monkify.data.remote.dto.LoginRequest
import com.k11000.monkify.data.remote.dto.LoginResponse
import com.k11000.monkify.data.remote.dto.LyricsResponse
import com.k11000.monkify.data.remote.dto.PlaylistSummaryDto
import com.k11000.monkify.data.remote.dto.SupportReportRequestDto
import com.k11000.monkify.data.remote.dto.SupportReportResponseDto
import com.k11000.monkify.domain.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class OfflineFirstTrackRepositoryTest {

    private lateinit var fakeDao: InMemoryTrackDao
    private lateinit var fakeNetworkRepo: FakeNetworkTrackRepository
    private lateinit var repository: OfflineFirstTrackRepository
    private lateinit var localRepo: LocalTrackRepository

    @Before
    fun setUp() {
        fakeDao = InMemoryTrackDao()
        fakeNetworkRepo = FakeNetworkTrackRepository()
        repository = OfflineFirstTrackRepository(fakeNetworkRepo, fakeDao)
        localRepo = LocalTrackRepository(fakeDao)
    }

    @Test
    fun `getTracks preserva isFavorite, isDownloaded, localFilePath y playCount al sincronizar desde el servidor`() = runTest {
        val existingEntity = TrackEntity(
            id = "track_1",
            title = "Old Title",
            artist = "Artist A",
            album = "Album X",
            durationMs = 180000L,
            coverUrl = "https://old/cover.jpg",
            streamUrl = "https://remote/stream/track_1",
            isFavorite = true,
            isDownloaded = true,
            localFilePath = "/data/user/0/com.k11000.monkify/files/downloads/track_1.mp3",
            addedAt = 1700000000000L,
            playCount = 12,
            lastPlayedAt = 1700005000000L
        )
        fakeDao.insertTrack(existingEntity)

        fakeNetworkRepo.tracksToReturn = listOf(
            Track(
                id = "track_1",
                title = "Updated Remastered Title",
                artist = "Artist A",
                album = "Album X",
                durationMs = 182000L,
                coverUrl = "https://new/cover.jpg",
                streamUrl = "https://remote/stream/track_1"
            ),
            Track(
                id = "track_2",
                title = "New Track",
                artist = "Artist B",
                album = "Album Y",
                durationMs = 200000L,
                coverUrl = null,
                streamUrl = "https://remote/stream/track_2"
            )
        )

        val result = repository.getTracks(limit = 20, offset = 0, query = null)
        assertEquals(2, result.size)

        val persistedTrack1 = fakeDao.getTrackById("track_1")!!
        assertEquals("Updated Remastered Title", persistedTrack1.title)
        assertEquals(182000L, persistedTrack1.durationMs)
        assertTrue("Debe preservar isFavorite = true", persistedTrack1.isFavorite)
        assertTrue("Debe preservar isDownloaded = true", persistedTrack1.isDownloaded)
        assertEquals("/data/user/0/com.k11000.monkify/files/downloads/track_1.mp3", persistedTrack1.localFilePath)
        assertEquals(1700000000000L, persistedTrack1.addedAt)
        assertEquals(12, persistedTrack1.playCount)
        assertEquals(1700005000000L, persistedTrack1.lastPlayedAt)
    }

    @Test
    fun `getRecentTracks emite lista vacia cuando la base local y la respuesta remota estan vacias`() = runTest {
        fakeNetworkRepo.tracksToReturn = emptyList()

        val recent = repository.getRecentTracks().first()
        assertTrue("Debe emitir lista vacía sin quedar colgado", recent.isEmpty())
    }

    @Test
    fun `getTracksByAlbum consulta por nombre de album normalizando signos mas e ignorando mayusculas`() = runTest {
        fakeDao.insertTrack(
            TrackEntity(
                id = "a1",
                title = "Song 1",
                artist = "Artist",
                album = "Dark Side",
                durationMs = 1000L,
                coverUrl = null,
                streamUrl = "https://remote/a1"
            )
        )
        val matches = localRepo.getTracksByAlbum("dark+side")
        assertEquals(1, matches.size)
        assertEquals("a1", matches.first().id)
    }

    private class FakeNetworkTrackRepository : NetworkTrackRepository(NoOpMonkifyApi, "https://example.com") {
        var tracksToReturn: List<Track> = emptyList()

        override suspend fun getTracks(limit: Int, offset: Int, query: String?): List<Track> = tracksToReturn

        override suspend fun getLatestUploadedTracks(limit: Int): List<Track> = tracksToReturn
    }

    private object NoOpMonkifyApi : MonkifyApi {
        override suspend fun getTracks(limit: Int, offset: Int, query: String?, sort: String?): List<BackendTrackDto> = emptyList()
        override suspend fun submitSupportReport(request: SupportReportRequestDto): SupportReportResponseDto = throw UnsupportedOperationException()
        override suspend fun login(request: LoginRequest): LoginResponse = throw UnsupportedOperationException()
        override suspend fun ingest(request: IngestRequest): Response<IngestResponse> = throw UnsupportedOperationException()
        override suspend fun getLyrics(trackId: String): LyricsResponse = throw UnsupportedOperationException()
        override suspend fun getPlaylists(limit: Int, offset: Int): List<PlaylistSummaryDto> = emptyList()
        override suspend fun getPlaylistTracks(id: String): List<BackendTrackDto> = emptyList()
        override suspend fun createPlaylist(request: CreatePlaylistRequest): PlaylistSummaryDto = throw UnsupportedOperationException()
        override suspend fun addTrackToPlaylist(playlistId: String, request: AddTrackToPlaylistRequest): ActionResponseDto = throw UnsupportedOperationException()
        override suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String): ActionResponseDto = throw UnsupportedOperationException()
        override suspend fun deletePlaylist(playlistId: String): ActionResponseDto = throw UnsupportedOperationException()
    }

    private class InMemoryTrackDao : TrackDao {
        private val state = MutableStateFlow<Map<String, TrackEntity>>(emptyMap())

        override fun getAllFavoriteTracks(): Flow<List<TrackEntity>> =
            state.map { map -> map.values.filter { it.isFavorite }.sortedByDescending { it.addedAt } }

        override fun getDownloadedTracks(): Flow<List<TrackEntity>> =
            state.map { map -> map.values.filter { it.isDownloaded }.sortedByDescending { it.addedAt } }

        override fun getAllTracksFlow(): Flow<List<TrackEntity>> =
            state.map { map -> map.values.sortedByDescending { it.addedAt } }

        override suspend fun getPagedTracks(limit: Int, offset: Int): List<TrackEntity> =
            state.value.values.sortedByDescending { it.addedAt }.drop(offset).take(limit)

        override suspend fun searchTracks(query: String, limit: Int, offset: Int): List<TrackEntity> =
            state.value.values.filter {
                it.title.contains(query, true) || it.artist.contains(query, true) || it.album.contains(query, true)
            }.drop(offset).take(limit)

        override suspend fun getTrackById(id: String): TrackEntity? = state.value[id]

        override fun isFavorite(id: String): Flow<Boolean> =
            state.map { it[id]?.isFavorite == true }

        override suspend fun insertTrack(track: TrackEntity) {
            state.value = state.value + (track.id to track)
        }

        override suspend fun insertTracks(tracks: List<TrackEntity>): List<Long> {
            val mutable = state.value.toMutableMap()
            tracks.forEach { if (!mutable.containsKey(it.id)) mutable[it.id] = it }
            state.value = mutable
            return tracks.map { 1L }
        }

        override suspend fun deleteTrackById(id: String): Int {
            val existed = state.value.containsKey(id)
            state.value = state.value - id
            return if (existed) 1 else 0
        }

        override suspend fun updateFavoriteStatus(id: String, isFavorite: Boolean): Int {
            val current = state.value[id] ?: return 0
            state.value = state.value + (id to current.copy(isFavorite = isFavorite))
            return 1
        }

        override suspend fun updateDownloadStatus(id: String, isDownloaded: Boolean, localPath: String?): Int {
            val current = state.value[id] ?: return 0
            state.value = state.value + (id to current.copy(isDownloaded = isDownloaded, localFilePath = localPath))
            return 1
        }

        override suspend fun getAllTracksSnapshot(): List<TrackEntity> = state.value.values.toList()

        override suspend fun recordTrackPlay(trackId: String, timestamp: Long): Int {
            val current = state.value[trackId] ?: return 0
            state.value = state.value + (trackId to current.copy(playCount = current.playCount + 1, lastPlayedAt = timestamp))
            return 1
        }

        override fun getRecentlyPlayedTracks(limit: Int): Flow<List<TrackEntity>> =
            state.map { map -> map.values.filter { it.lastPlayedAt > 0 }.sortedByDescending { it.lastPlayedAt }.take(limit) }

        override fun getTracksOrderedByPlayCount(): Flow<List<TrackEntity>> =
            state.map { map -> map.values.sortedWith(compareByDescending<TrackEntity> { it.playCount }.thenByDescending { it.lastPlayedAt }) }

        override suspend fun getRecentlyPlayedTracksSnapshot(limit: Int): List<TrackEntity> =
            state.value.values.filter { it.lastPlayedAt > 0 }.sortedByDescending { it.lastPlayedAt }.take(limit)

        override suspend fun getTracksOrderedByPlayCountSnapshot(limit: Int): List<TrackEntity> =
            state.value.values.sortedWith(compareByDescending<TrackEntity> { it.playCount }.thenByDescending { it.lastPlayedAt }).take(limit)
    }
}
