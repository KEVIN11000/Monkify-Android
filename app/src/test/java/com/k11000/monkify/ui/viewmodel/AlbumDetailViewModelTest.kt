package com.k11000.monkify.ui.viewmodel

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
import com.k11000.monkify.data.repository.LocalTrackRepository
import com.k11000.monkify.domain.model.DownloadState
import com.k11000.monkify.domain.model.Track
import com.k11000.monkify.domain.repository.DownloadRepository
import com.k11000.monkify.ui.screens.viewmodel.AlbumDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class AlbumDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeMonkifyApi(
        private val albumTracksHandler: (album: String) -> List<BackendTrackDto> = { emptyList() }
    ) : MonkifyApi {
        override suspend fun getTracks(limit: Int, offset: Int, query: String?, sort: String?): List<BackendTrackDto> = emptyList()
        override suspend fun getAlbumTracks(album: String): List<BackendTrackDto> = albumTracksHandler(album)
        override suspend fun login(request: LoginRequest): LoginResponse = LoginResponse("token", "ref", "bearer", 3600)
        override suspend fun ingest(request: IngestRequest): Response<IngestResponse> = Response.success(IngestResponse("t1", "OK", "msg"))
        override suspend fun getLyrics(trackId: String): LyricsResponse = LyricsResponse(trackId, "lrc", "")
        override suspend fun submitSupportReport(request: com.k11000.monkify.data.remote.dto.SupportReportRequestDto): com.k11000.monkify.data.remote.dto.SupportReportResponseDto = com.k11000.monkify.data.remote.dto.SupportReportResponseDto(ok = true)
        override suspend fun getPlaylists(limit: Int, offset: Int): List<PlaylistSummaryDto> = emptyList()
        override suspend fun getPlaylistTracks(id: String): List<BackendTrackDto> = emptyList()
        override suspend fun createPlaylist(request: CreatePlaylistRequest): PlaylistSummaryDto = PlaylistSummaryDto("pl_id", request.name, 0)
        override suspend fun addTrackToPlaylist(playlistId: String, request: AddTrackToPlaylistRequest): ActionResponseDto = ActionResponseDto(true, "added")
        override suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String): ActionResponseDto = ActionResponseDto(true, "removed")
        override suspend fun deletePlaylist(playlistId: String): ActionResponseDto = ActionResponseDto(true, "deleted")
    }

    private class FakeTrackDao : TrackDao {
        val tracksFlow = MutableStateFlow<List<TrackEntity>>(emptyList())

        override fun getAllFavoriteTracks(): Flow<List<TrackEntity>> = tracksFlow.map { list -> list.filter { it.isFavorite } }
        override fun getDownloadedTracks(): Flow<List<TrackEntity>> = tracksFlow.map { list -> list.filter { it.isDownloaded } }
        override fun getAllTracksFlow(): Flow<List<TrackEntity>> = tracksFlow
        override suspend fun getPagedTracks(limit: Int, offset: Int): List<TrackEntity> = tracksFlow.value.drop(offset).take(limit)
        override suspend fun searchTracks(query: String, limit: Int, offset: Int): List<TrackEntity> = emptyList()
        override suspend fun getTrackById(id: String): TrackEntity? = tracksFlow.value.find { it.id == id }
        override fun isFavorite(id: String): Flow<Boolean> = tracksFlow.map { list -> list.any { it.id == id && it.isFavorite } }
        override suspend fun insertTrack(track: TrackEntity) {
            tracksFlow.value = tracksFlow.value.filter { it.id != track.id } + track
        }
        override suspend fun insertTracks(tracks: List<TrackEntity>): List<Long> = emptyList()
        override suspend fun deleteTrackById(id: String): Int = 0
        override suspend fun updateFavoriteStatus(id: String, isFavorite: Boolean): Int = 1
        override suspend fun updateDownloadStatus(id: String, isDownloaded: Boolean, localPath: String?): Int = 1
        override suspend fun getAllTracksSnapshot(): List<TrackEntity> = tracksFlow.value
        override suspend fun recordTrackPlay(trackId: String, timestamp: Long): Int = 1
        override fun getRecentlyPlayedTracks(limit: Int): Flow<List<TrackEntity>> =
            tracksFlow.map { list -> list.filter { it.lastPlayedAt > 0 }.sortedByDescending { it.lastPlayedAt }.take(limit) }
        override fun getTracksOrderedByPlayCount(): Flow<List<TrackEntity>> =
            tracksFlow.map { list -> list.sortedByDescending { it.playCount } }
        override suspend fun getRecentlyPlayedTracksSnapshot(limit: Int): List<TrackEntity> =
            tracksFlow.value.filter { it.lastPlayedAt > 0 }.sortedByDescending { it.lastPlayedAt }.take(limit)
        override suspend fun getTracksOrderedByPlayCountSnapshot(limit: Int): List<TrackEntity> =
            tracksFlow.value.sortedByDescending { it.playCount }.take(limit)
    }

    private class FakeDownloadRepository(
        initialDownloaded: Set<String> = emptySet()
    ) : DownloadRepository {
        val downloadedSet = MutableStateFlow(initialDownloaded)
        override val downloadedTrackIds: Flow<Set<String>> = downloadedSet
        override val downloadingTrackIds: StateFlow<Set<String>> = MutableStateFlow(emptySet())
        var downloadTracksCalledWith: List<Track>? = null
        var downloadKeyCalledWith: String? = null

        override fun getDownloadState(trackId: String): Flow<DownloadState> = flowOf(DownloadState.NotDownloaded)
        override suspend fun downloadTrack(track: Track): Result<String> {
            downloadedSet.value = downloadedSet.value + track.id
            return Result.success("/downloads/${track.id}.mp3")
        }
        override suspend fun downloadTracks(tracks: List<Track>, onProgress: (completed: Int, total: Int) -> Unit): Result<Unit> = Result.success(Unit)
        override suspend fun deleteDownloadedTrack(trackId: String): Result<Unit> {
            downloadedSet.value = downloadedSet.value - trackId
            return Result.success(Unit)
        }
        override suspend fun toggleDownload(track: Track) {
            if (isTrackDownloaded(track.id)) deleteDownloadedTrack(track.id) else downloadTrack(track)
        }
        override fun isTrackDownloaded(trackId: String): Boolean = trackId in downloadedSet.value
        override fun getLocalLyrics(trackId: String): String? = null
        override fun getLocalAudioPath(trackId: String): String? = null
        override fun getTotalDownloadsSizeBytes(): Long = 0L

        val isDownloading = MutableStateFlow(false)
        val downloadProgress = MutableStateFlow(Pair(0, 0))

        override fun isPlaylistDownloading(playlistId: String): StateFlow<Boolean> = isDownloading
        override fun getPlaylistDownloadProgress(playlistId: String): StateFlow<Pair<Int, Int>> = downloadProgress

        override fun downloadPlaylist(playlistId: String, tracks: List<Track>) {
            downloadKeyCalledWith = playlistId
            downloadTracksCalledWith = tracks
            isDownloading.value = true
            tracks.forEachIndexed { index, track ->
                downloadedSet.value = downloadedSet.value + track.id
                downloadProgress.value = Pair(index + 1, tracks.size)
            }
            isDownloading.value = false
        }
        override fun cancelPlaylistDownload(playlistId: String) {
            isDownloading.value = false
        }
    }

    @Test
    fun `loadAlbum carga pistas desde la API e infiere artista y coverUrl si no se enviaron`() = runTest(testDispatcher) {
        val dtos = listOf(
            BackendTrackDto("t1", "Breathe", "Pink Floyd", "The Dark Side of the Moon", 163000L, "https://cover/dsotm.jpg", "/api/v1/stream/t1", false),
            BackendTrackDto("t2", "Time", "Pink Floyd", "The Dark Side of the Moon", 413000L, "https://cover/dsotm.jpg", "/api/v1/stream/t2", false)
        )
        val fakeApi = FakeMonkifyApi(albumTracksHandler = { dtos })
        val viewModel = AlbumDetailViewModel(fakeApi)

        viewModel.loadAlbum("The Dark Side of the Moon")
        advanceUntilIdle()

        assertEquals("The Dark Side of the Moon", viewModel.albumName.value)
        assertEquals("Pink Floyd", viewModel.artistName.value)
        assertEquals("https://cover/dsotm.jpg", viewModel.coverUrl.value)
        assertEquals(2, viewModel.tracks.value.size)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `loadAlbum usa fallback de LocalTrackRepository cuando falla la red`() = runTest(testDispatcher) {
        val fakeDao = FakeTrackDao()
        fakeDao.insertTrack(
            TrackEntity(
                id = "loc1",
                title = "Smells Like Teen Spirit",
                artist = "Nirvana",
                album = "Nevermind",
                durationMs = 301000L,
                coverUrl = "https://cover/nevermind.jpg",
                streamUrl = "https://stream/loc1",
                isFavorite = true,
                isDownloaded = true,
                localFilePath = "/local/loc1.mp3"
            )
        )
        val localRepo = LocalTrackRepository(fakeDao)
        val fakeApi = FakeMonkifyApi(albumTracksHandler = { throw RuntimeException("Offline") })
        val viewModel = AlbumDetailViewModel(fakeApi, localRepo)

        viewModel.loadAlbum("Nevermind")
        advanceUntilIdle()

        assertEquals(1, viewModel.tracks.value.size)
        assertEquals("Smells Like Teen Spirit", viewModel.tracks.value.first().title)
        assertEquals("Nirvana", viewModel.artistName.value)
        assertEquals("https://cover/nevermind.jpg", viewModel.coverUrl.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `downloadAllTracks delega descarga completa del album a DownloadRepository`() = runTest(testDispatcher) {
        val dtos = listOf(
            BackendTrackDto("t1", "Track A", "Artist A", "Album X", 180000L, "https://cover/x.jpg", "/api/v1/stream/t1", false),
            BackendTrackDto("t2", "Track B", "Artist A", "Album X", 195000L, "https://cover/x.jpg", "/api/v1/stream/t2", false)
        )
        val fakeApi = FakeMonkifyApi(albumTracksHandler = { dtos })
        val fakeDownloadRepo = FakeDownloadRepository()
        val viewModel = AlbumDetailViewModel(fakeApi, null, fakeDownloadRepo)

        viewModel.loadAlbum("Album X", "Artist A", "https://cover/x.jpg")
        advanceUntilIdle()

        var success = false
        viewModel.downloadAllTracks { res ->
            success = res.isSuccess
        }
        advanceUntilIdle()

        assertTrue(success)
        assertEquals("album:Album X", fakeDownloadRepo.downloadKeyCalledWith)
        assertEquals(listOf("t1", "t2"), fakeDownloadRepo.downloadTracksCalledWith?.map { it.id })
        assertEquals(Pair(2, 2), viewModel.downloadProgress.value)
    }

    @Test
    fun `filteredAndSortedTracks filtra por query y ordena segun TrackSortOption`() = runTest(testDispatcher) {
        val dtos = listOf(
            BackendTrackDto("t1", "Zebra Song", "Artist B", "Album Z", 100000L, null, "/stream/1", false),
            BackendTrackDto("t2", "Apple Song", "Artist A", "Album Z", 300000L, null, "/stream/2", false),
            BackendTrackDto("t3", "Berry Song", "Artist C", "Album Z", 200000L, null, "/stream/3", false)
        )
        val fakeApi = FakeMonkifyApi(albumTracksHandler = { dtos })
        val viewModel = AlbumDetailViewModel(fakeApi, null, null)

        viewModel.loadAlbum("Album Z")
        advanceUntilIdle()

        assertEquals(3, viewModel.filteredAndSortedTracks.value.size)

        // Ordenar A-Z
        viewModel.setSortOption(com.k11000.monkify.domain.model.TrackSortOption.TITLE_ASC)
        advanceUntilIdle()
        assertEquals(listOf("Apple Song", "Berry Song", "Zebra Song"), viewModel.filteredAndSortedTracks.value.map { it.title })

        // Filtrar por query
        viewModel.onSearchQueryChange("berry")
        advanceUntilIdle()
        assertEquals(1, viewModel.filteredAndSortedTracks.value.size)
        assertEquals("Berry Song", viewModel.filteredAndSortedTracks.value.first().title)
    }

    @Test
    fun `loadAlbum decodifica nombres con signos plus o url encoding y carga pistas correctamente`() = runTest(testDispatcher) {
        val dtos = listOf(
            BackendTrackDto("t1", "Bohemian Rhapsody", "Queen", "A Night at the Opera", 354000L, null, "/stream/1", false)
        )
        var receivedAlbumArg = ""
        val fakeApi = FakeMonkifyApi(albumTracksHandler = { album ->
            receivedAlbumArg = album
            dtos
        })
        val viewModel = AlbumDetailViewModel(fakeApi)

        // Se envía codificado con "+" como sucede en application/x-www-form-urlencoded
        viewModel.loadAlbum("A+Night+at+the+Opera")
        advanceUntilIdle()

        assertEquals("A Night at the Opera", viewModel.albumName.value)
        assertEquals("A Night at the Opera", receivedAlbumArg)
        assertEquals(1, viewModel.tracks.value.size)
        assertEquals("Bohemian Rhapsody", viewModel.tracks.value.first().title)
    }

    @Test
    fun `loadAlbum con fallback local resuelve nombres con signos plus hacia el album guardado en Room`() = runTest(testDispatcher) {
        val fakeDao = FakeTrackDao()
        fakeDao.insertTrack(
            TrackEntity(
                id = "loc1",
                title = "Starman",
                artist = "David Bowie",
                album = "The Rise and Fall of Ziggy Stardust",
                durationMs = 250000L,
                coverUrl = null,
                streamUrl = "/stream/loc1",
                isFavorite = false,
                isDownloaded = false
            )
        )
        val localRepo = LocalTrackRepository(fakeDao)
        val fakeApi = FakeMonkifyApi(albumTracksHandler = { emptyList() })
        val viewModel = AlbumDetailViewModel(fakeApi, localRepo)

        // Enviar con "+" o percent encoding
        viewModel.loadAlbum("The+Rise+and+Fall+of+Ziggy+Stardust")
        advanceUntilIdle()

        assertEquals("The Rise and Fall of Ziggy Stardust", viewModel.albumName.value)
        assertEquals(1, viewModel.tracks.value.size)
        assertEquals("Starman", viewModel.tracks.value.first().title)
    }
}
