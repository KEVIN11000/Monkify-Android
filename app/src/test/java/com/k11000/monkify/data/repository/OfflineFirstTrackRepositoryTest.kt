package com.k11000.monkify.data.repository

import com.k11000.monkify.data.local.dao.TrackDao
import com.k11000.monkify.data.local.entity.TrackEntity
import com.k11000.monkify.data.remote.api.MonkifyApi
import com.k11000.monkify.data.remote.dto.BackendTrackDto
import com.k11000.monkify.data.remote.dto.IngestRequest
import com.k11000.monkify.data.remote.dto.IngestResponse
import com.k11000.monkify.data.remote.dto.LoginRequest
import com.k11000.monkify.data.remote.dto.LoginResponse
import com.k11000.monkify.data.remote.dto.LyricsResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class OfflineFirstTrackRepositoryTest {

    private class FakeTrackDao : TrackDao {
        val tracksFlow = MutableStateFlow<List<TrackEntity>>(emptyList())

        override fun getAllFavoriteTracks(): Flow<List<TrackEntity>> =
            tracksFlow.map { list -> list.filter { it.isFavorite } }

        override fun getDownloadedTracks(): Flow<List<TrackEntity>> =
            tracksFlow.map { list -> list.filter { it.isDownloaded } }

        override fun getAllTracksFlow(): Flow<List<TrackEntity>> = tracksFlow

        override suspend fun getPagedTracks(limit: Int, offset: Int): List<TrackEntity> =
            tracksFlow.value.drop(offset).take(limit)

        override suspend fun searchTracks(query: String, limit: Int, offset: Int): List<TrackEntity> =
            tracksFlow.value.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true) ||
                it.album.contains(query, ignoreCase = true)
            }.drop(offset).take(limit)

        override suspend fun getTrackById(id: String): TrackEntity? =
            tracksFlow.value.find { it.id == id }

        override fun isFavorite(id: String): Flow<Boolean> =
            tracksFlow.map { list -> list.any { it.id == id && it.isFavorite } }

        override suspend fun insertTrack(track: TrackEntity) {
            tracksFlow.value = tracksFlow.value.filter { it.id != track.id } + track
        }

        override suspend fun insertTracks(tracks: List<TrackEntity>): List<Long> {
            val existingIds = tracksFlow.value.map { it.id }.toSet()
            val newTracks = tracks.filter { it.id !in existingIds }
            tracksFlow.value = tracksFlow.value + newTracks
            return newTracks.map { 1L }
        }

        override suspend fun deleteTrackById(id: String): Int {
            val initialSize = tracksFlow.value.size
            tracksFlow.value = tracksFlow.value.filter { it.id != id }
            return initialSize - tracksFlow.value.size
        }

        override suspend fun updateFavoriteStatus(id: String, isFavorite: Boolean): Int {
            tracksFlow.value = tracksFlow.value.map {
                if (it.id == id) it.copy(isFavorite = isFavorite) else it
            }
            return 1
        }

        override suspend fun updateDownloadStatus(id: String, isDownloaded: Boolean, localPath: String?): Int {
            tracksFlow.value = tracksFlow.value.map {
                if (it.id == id) it.copy(isDownloaded = isDownloaded, localFilePath = localPath) else it
            }
            return 1
        }
    }

    private class FakeMonkifyApi(
        private val getTracksHandler: (limit: Int, offset: Int, query: String?) -> List<BackendTrackDto> = { _, _, _ -> emptyList() },
        private val ingestHandler: (request: IngestRequest) -> Response<IngestResponse> = {
            Response.success(IngestResponse("task_1", "OK", "Queued"))
        }
    ) : MonkifyApi {
        override suspend fun getTracks(limit: Int, offset: Int, query: String?): List<BackendTrackDto> =
            getTracksHandler(limit, offset, query)

        override suspend fun login(request: LoginRequest): LoginResponse =
            LoginResponse("fake_token", "fake_refresh", "bearer", 3600)

        override suspend fun ingest(request: IngestRequest): Response<IngestResponse> =
            ingestHandler(request)

        override suspend fun getLyrics(trackId: String): LyricsResponse =
            LyricsResponse(trackId, "lrc", "")

        override suspend fun getPlaylists(limit: Int, offset: Int): List<com.k11000.monkify.data.remote.dto.PlaylistSummaryDto> =
            emptyList()

        override suspend fun getPlaylistTracks(id: String): List<BackendTrackDto> =
            emptyList()
    }

    private fun sampleEntity(id: String, title: String, artist: String, album: String = "Test Album") = TrackEntity(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = 210000L,
        coverUrl = "https://example.com/$id.jpg",
        streamUrl = "https://stream/$id",
        isFavorite = false,
        isDownloaded = false,
        localFilePath = null
    )

    private fun sampleBackendTrack(id: String, title: String, artist: String, album: String = "Test Album") = BackendTrackDto(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = 210000L,
        coverUrl = "https://example.com/$id.jpg",
        streamUrl = "/api/v1/stream/$id",
        isDownloaded = false
    )

    @Test
    fun `cuando la red falla o no esta disponible, getTracks retorna el catalogo persistido en Room`() = runTest {
        val fakeDao = FakeTrackDao()
        fakeDao.insertTrack(sampleEntity("t1", "Comfortably Numb", "Pink Floyd"))
        fakeDao.insertTrack(sampleEntity("t2", "Wish You Were Here", "Pink Floyd"))

        // La API falla lanzando excepción de conexión
        val fakeApi = FakeMonkifyApi(
            getTracksHandler = { _, _, _ -> throw RuntimeException("No network connection") }
        )
        val networkRepo = NetworkTrackRepository(fakeApi, "https://api.monkify.com/")
        val repository = OfflineFirstTrackRepository(networkRepo, fakeDao)

        val tracks = repository.getTracks(limit = 10, offset = 0)

        assertEquals(2, tracks.size)
        assertEquals("Comfortably Numb", tracks[0].title)
        assertEquals("Wish You Were Here", tracks[1].title)
    }

    @Test
    fun `cuando la red responde con exito, getTracks sincroniza e inserta las pistas en Room y las retorna`() = runTest {
        val fakeDao = FakeTrackDao()
        assertTrue(fakeDao.tracksFlow.value.isEmpty())

        val networkDtos = listOf(
            sampleBackendTrack("net1", "Starman", "David Bowie"),
            sampleBackendTrack("net2", "Space Oddity", "David Bowie")
        )

        val fakeApi = FakeMonkifyApi(
            getTracksHandler = { _, _, _ -> networkDtos }
        )
        val networkRepo = NetworkTrackRepository(fakeApi, "https://api.monkify.com/")
        val repository = OfflineFirstTrackRepository(networkRepo, fakeDao)

        val tracks = repository.getTracks(limit = 10, offset = 0)

        assertEquals(2, tracks.size)
        assertEquals("Starman", tracks[0].title)
        // Confirmar que Room fue actualizado en background
        assertEquals(2, fakeDao.tracksFlow.value.size)
        assertEquals("Starman", fakeDao.tracksFlow.value[0].title)
    }

    @Test
    fun `en busqueda offline, getTracks filtra las pistas locales en Room por query`() = runTest {
        val fakeDao = FakeTrackDao()
        fakeDao.insertTrack(sampleEntity("t1", "Bohemian Rhapsody", "Queen"))
        fakeDao.insertTrack(sampleEntity("t2", "Hotel California", "Eagles"))
        fakeDao.insertTrack(sampleEntity("t3", "Radio Ga Ga", "Queen"))

        // Offline (falla red)
        val fakeApi = FakeMonkifyApi(
            getTracksHandler = { _, _, _ -> throw RuntimeException("Offline mode") }
        )
        val networkRepo = NetworkTrackRepository(fakeApi, "https://api.monkify.com/")
        val repository = OfflineFirstTrackRepository(networkRepo, fakeDao)

        val queenResults = repository.getTracks(limit = 10, offset = 0, query = "Queen")
        assertEquals(2, queenResults.size)
        assertTrue(queenResults.all { it.artist == "Queen" })

        val hotelResults = repository.getTracks(limit = 10, offset = 0, query = "Hotel")
        assertEquals(1, hotelResults.size)
        assertEquals("Hotel California", hotelResults[0].title)
    }

    @Test
    fun `getRecentTracks emite catalogo local inmediatamente en ausencia de red`() = runTest {
        val fakeDao = FakeTrackDao()
        fakeDao.insertTrack(sampleEntity("t1", "Clocks", "Coldplay"))

        val fakeApi = FakeMonkifyApi(
            getTracksHandler = { _, _, _ -> throw RuntimeException("Offline") }
        )
        val networkRepo = NetworkTrackRepository(fakeApi, "https://api.monkify.com/")
        val repository = OfflineFirstTrackRepository(networkRepo, fakeDao)

        val firstEmission = repository.getRecentTracks().first()
        assertEquals(1, firstEmission.size)
        assertEquals("Clocks", firstEmission[0].title)
    }

    @Test
    fun `requestIngest delega en networkTrackRepository`() = runTest {
        val fakeDao = FakeTrackDao()
        var ingestCalled = false
        val fakeApi = FakeMonkifyApi(
            ingestHandler = {
                ingestCalled = true
                Response.success(IngestResponse("task_ingest", "QUEUED", "Enqueued"))
            }
        )
        val networkRepo = NetworkTrackRepository(fakeApi, "https://api.monkify.com/")
        val repository = OfflineFirstTrackRepository(networkRepo, fakeDao)

        val result = repository.requestIngest("https://open.spotify.com/track/123")
        assertTrue(ingestCalled)
        assertTrue(result.isSuccess)
        assertEquals("task_ingest", result.getOrNull()?.taskId)
    }
}
