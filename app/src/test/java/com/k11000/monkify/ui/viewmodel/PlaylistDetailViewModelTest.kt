package com.k11000.monkify.ui.viewmodel

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
import com.k11000.monkify.data.remote.dto.toDomain
import com.k11000.monkify.domain.model.DownloadState
import com.k11000.monkify.domain.model.Track
import com.k11000.monkify.domain.repository.DownloadRepository
import com.k11000.monkify.domain.repository.PlaylistRepository
import com.k11000.monkify.ui.screens.viewmodel.PlaylistDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
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
class PlaylistDetailViewModelTest {

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
        private val tracksHandler: (playlistId: String) -> List<BackendTrackDto> = { emptyList() },
        private val removeTrackHandler: (playlistId: String, trackId: String) -> ActionResponseDto = { _, _ -> ActionResponseDto(true, "removed") },
        private val deletePlaylistHandler: (playlistId: String) -> ActionResponseDto = { ActionResponseDto(true, "deleted") }
    ) : MonkifyApi {
        override suspend fun getTracks(limit: Int, offset: Int, query: String?, sort: String?): List<BackendTrackDto> = emptyList()
        override suspend fun submitSupportReport(request: com.k11000.monkify.data.remote.dto.SupportReportRequestDto): com.k11000.monkify.data.remote.dto.SupportReportResponseDto = com.k11000.monkify.data.remote.dto.SupportReportResponseDto(ok = true)
        override suspend fun login(request: LoginRequest): LoginResponse = LoginResponse("token", "ref", "bearer", 3600)
        override suspend fun ingest(request: IngestRequest): Response<IngestResponse> = Response.success(IngestResponse("t1", "OK", "msg"))
        override suspend fun getLyrics(trackId: String): LyricsResponse = LyricsResponse(trackId, "lrc", "")
        override suspend fun getPlaylists(limit: Int, offset: Int): List<PlaylistSummaryDto> = emptyList()
        override suspend fun getPlaylistTracks(id: String): List<BackendTrackDto> = tracksHandler(id)
        override suspend fun createPlaylist(request: CreatePlaylistRequest): PlaylistSummaryDto = PlaylistSummaryDto("pl_id", request.name, 0)
        override suspend fun addTrackToPlaylist(playlistId: String, request: AddTrackToPlaylistRequest): ActionResponseDto = ActionResponseDto(true, "added")
        override suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String): ActionResponseDto = removeTrackHandler(playlistId, trackId)
        override suspend fun deletePlaylist(playlistId: String): ActionResponseDto = deletePlaylistHandler(playlistId)
    }

    private class FakePlaylistRepository(
        private val tracksHandler: (playlistId: String) -> List<Track> = { emptyList() },
        private val removeTrackHandler: (playlistId: String, trackId: String) -> Result<Unit> = { _, _ -> Result.success(Unit) },
        private val deletePlaylistHandler: (playlistId: String) -> Result<Unit> = { Result.success(Unit) }
    ) : PlaylistRepository {
        override suspend fun getPlaylists(limit: Int, offset: Int): Result<List<PlaylistSummaryDto>> = Result.success(emptyList())
        override suspend fun getPlaylistTracks(playlistId: String): Result<List<Track>> = Result.success(tracksHandler(playlistId))
        override suspend fun createPlaylist(name: String, description: String?): Result<PlaylistSummaryDto> = Result.success(PlaylistSummaryDto("pl_id", name, 0))
        override suspend fun deletePlaylist(playlistId: String): Result<Unit> = deletePlaylistHandler(playlistId)
        override suspend fun addTrackToPlaylist(playlistId: String, trackId: String): Result<Unit> = Result.success(Unit)
        override suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String): Result<Unit> = removeTrackHandler(playlistId, trackId)
    }

    private class FakeDownloadRepository(
        initialDownloaded: Set<String> = emptySet()
    ) : DownloadRepository {
        val downloadedSet = MutableStateFlow(initialDownloaded)
        override val downloadedTrackIds: Flow<Set<String>> = downloadedSet
        override val downloadingTrackIds: StateFlow<Set<String>> = MutableStateFlow(emptySet())
        var downloadTracksCalledWith: List<Track>? = null

        override fun getDownloadState(trackId: String): Flow<DownloadState> = flowOf(DownloadState.NotDownloaded)
        override suspend fun downloadTrack(track: Track): Result<String> {
            downloadedSet.value = downloadedSet.value + track.id
            return Result.success("/downloads/${track.id}.mp3")
        }
        override suspend fun downloadTracks(
            tracks: List<Track>,
            onProgress: (completed: Int, total: Int) -> Unit
        ): Result<Unit> {
            downloadTracksCalledWith = tracks
            tracks.forEachIndexed { index, track ->
                downloadedSet.value = downloadedSet.value + track.id
                onProgress(index + 1, tracks.size)
            }
            return Result.success(Unit)
        }
        override suspend fun deleteDownloadedTrack(trackId: String): Result<Unit> {
            downloadedSet.value = downloadedSet.value - trackId
            return Result.success(Unit)
        }
        override suspend fun toggleDownload(track: Track) {
            if (isTrackDownloaded(track.id)) {
                deleteDownloadedTrack(track.id)
            } else {
                downloadTrack(track)
            }
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


    private fun sampleTrackDto(id: String, title: String, artist: String) = BackendTrackDto(
        id = id,
        title = title,
        artist = artist,
        album = "Test Album",
        durationMs = 200000L,
        coverUrl = "https://cover/$id.jpg",
        streamUrl = "/api/v1/stream/$id",
        isDownloaded = false
    )

    @Test
    fun `loadPlaylist actualiza playlistName, lista de canciones y desactiva estado de carga`() = runTest(testDispatcher) {
        val testTracks = listOf(
            sampleTrackDto("t1", "Comfortably Numb", "Pink Floyd"),
            sampleTrackDto("t2", "Time", "Pink Floyd"),
            sampleTrackDto("t3", "Money", "Pink Floyd")
        )

        val fakeApi = FakeMonkifyApi(tracksHandler = { testTracks })
        val viewModel = PlaylistDetailViewModel(fakeApi)

        viewModel.loadPlaylist("pl_pink", "Pink Floyd Greatest")
        advanceUntilIdle()

        assertEquals("Pink Floyd Greatest", viewModel.playlistName.value)
        assertEquals(3, viewModel.tracks.value.size)
        assertEquals("Comfortably Numb", viewModel.tracks.value[0].title)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `loadPlaylist con error de red deja lista vacia y finaliza isLoading`() = runTest(testDispatcher) {
        val fakeApi = FakeMonkifyApi(tracksHandler = { throw RuntimeException("Network timeout") })
        val viewModel = PlaylistDetailViewModel(fakeApi)

        viewModel.loadPlaylist("pl_fail", "Failed Playlist")
        advanceUntilIdle()

        assertEquals("Failed Playlist", viewModel.playlistName.value)
        assertTrue(viewModel.tracks.value.isEmpty())
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `loadPlaylist conserva el nombre inicial provisto`() = runTest(testDispatcher) {
        val fakeApi = FakeMonkifyApi(tracksHandler = { emptyList() })
        val viewModel = PlaylistDetailViewModel(fakeApi)

        viewModel.loadPlaylist("pl_empty", "Chill Morning Vibes")
        advanceUntilIdle()

        assertEquals("Chill Morning Vibes", viewModel.playlistName.value)
        assertTrue(viewModel.tracks.value.isEmpty())
    }

    @Test
    fun `removeTrack invoca exitosamente la API y remueve la cancion de tracks`() = runTest(testDispatcher) {
        val testTracks = listOf(
            sampleTrackDto("t1", "Track 1", "Artist 1"),
            sampleTrackDto("t2", "Track 2", "Artist 2"),
            sampleTrackDto("t3", "Track 3", "Artist 3")
        )
        val fakeApi = FakeMonkifyApi(tracksHandler = { testTracks })
        val viewModel = PlaylistDetailViewModel(fakeApi)

        viewModel.loadPlaylist("pl_1", "My Playlist")
        advanceUntilIdle()
        assertEquals(3, viewModel.tracks.value.size)

        var resultSuccess = false
        viewModel.removeTrack("t2") { res ->
            resultSuccess = res.isSuccess
        }
        advanceUntilIdle()

        assertTrue(resultSuccess)
        assertEquals(2, viewModel.tracks.value.size)
        assertEquals(listOf("t1", "t3"), viewModel.tracks.value.map { it.id })
    }

    @Test
    fun `removeTrack con fallo no modifica el estado tracks y reporta error`() = runTest(testDispatcher) {
        val testTracks = listOf(
            sampleTrackDto("t1", "Track 1", "Artist 1"),
            sampleTrackDto("t2", "Track 2", "Artist 2")
        )
        val fakeApi = FakeMonkifyApi(
            tracksHandler = { testTracks },
            removeTrackHandler = { _, _ -> throw RuntimeException("HTTP 500 Server Error") }
        )
        val viewModel = PlaylistDetailViewModel(fakeApi)

        viewModel.loadPlaylist("pl_1", "My Playlist")
        advanceUntilIdle()
        assertEquals(2, viewModel.tracks.value.size)

        var resultFailure = false
        viewModel.removeTrack("t1") { res ->
            resultFailure = res.isFailure
        }
        advanceUntilIdle()

        assertTrue(resultFailure)
        assertEquals(2, viewModel.tracks.value.size)
    }

    @Test
    fun `deletePlaylist invoca la API y reporta exito`() = runTest(testDispatcher) {
        var deleteCalledWith: String? = null
        val fakeApi = FakeMonkifyApi(
            deletePlaylistHandler = { id ->
                deleteCalledWith = id
                ActionResponseDto(true, "deleted")
            }
        )
        val viewModel = PlaylistDetailViewModel(fakeApi)

        viewModel.loadPlaylist("pl_to_delete", "Delete Me")
        advanceUntilIdle()

        var resultSuccess = false
        viewModel.deletePlaylist { res ->
            resultSuccess = res.isSuccess
        }
        advanceUntilIdle()

        assertTrue(resultSuccess)
        assertEquals("pl_to_delete", deleteCalledWith)
    }

    @Test
    fun `deletePlaylist con fallo en API reporta error`() = runTest(testDispatcher) {
        val fakeApi = FakeMonkifyApi(
            deletePlaylistHandler = { _ -> ActionResponseDto(false, "DB Error") }
        )
        val viewModel = PlaylistDetailViewModel(fakeApi)

        viewModel.loadPlaylist("pl_fail_delete", "Delete Fail")
        advanceUntilIdle()

        var resultFailure = false
        viewModel.deletePlaylist { res ->
            resultFailure = res.isFailure
        }
        advanceUntilIdle()

        assertTrue(resultFailure)
    }

    @Test
    fun `removeTrack con PlaylistRepository delegado funciona y actualiza estado`() = runTest(testDispatcher) {
        val testTracks = listOf(
            sampleTrackDto("t1", "Track 1", "Artist 1"),
            sampleTrackDto("t2", "Track 2", "Artist 2")
        )
        val fakeApi = FakeMonkifyApi(tracksHandler = { testTracks })
        var repoRemoveCalled = false
        val fakeRepo = FakePlaylistRepository(
            tracksHandler = { testTracks.map { it.toDomain(com.k11000.monkify.BuildConfig.BASE_URL) } },
            removeTrackHandler = { plId, trackId ->
                repoRemoveCalled = (plId == "pl_repo" && trackId == "t1")
                Result.success(Unit)
            }
        )
        val viewModel = PlaylistDetailViewModel(fakeApi, fakeRepo)

        viewModel.loadPlaylist("pl_repo", "Repo Playlist")
        advanceUntilIdle()

        var resultSuccess = false
        viewModel.removeTrack("t1") { res ->
            resultSuccess = res.isSuccess
        }
        advanceUntilIdle()

        assertTrue(repoRemoveCalled)
        assertTrue(resultSuccess)
        assertEquals(1, viewModel.tracks.value.size)
        assertEquals("t2", viewModel.tracks.value.first().id)
    }

    @Test
    fun `deletePlaylist con PlaylistRepository delegado funciona correctamente`() = runTest(testDispatcher) {
        val fakeApi = FakeMonkifyApi()
        var repoDeleteCalled = false
        val fakeRepo = FakePlaylistRepository(
            deletePlaylistHandler = { plId ->
                repoDeleteCalled = (plId == "pl_repo_del")
                Result.success(Unit)
            }
        )
        val viewModel = PlaylistDetailViewModel(fakeApi, fakeRepo)

        viewModel.loadPlaylist("pl_repo_del", "Repo Del")
        advanceUntilIdle()

        var resultSuccess = false
        viewModel.deletePlaylist { res ->
            resultSuccess = res.isSuccess
        }
        advanceUntilIdle()

        assertTrue(repoDeleteCalled)
        assertTrue(resultSuccess)
    }

    @Test
    fun `downloadAllTracks descarga todas las canciones de la playlist y reporta progreso`() = runTest(testDispatcher) {
        val testTracks = listOf(
            sampleTrackDto("t1", "Track 1", "Artist 1"),
            sampleTrackDto("t2", "Track 2", "Artist 2")
        )
        val fakeApi = FakeMonkifyApi(tracksHandler = { testTracks })
        val fakeDownloadRepo = FakeDownloadRepository()
        val viewModel = PlaylistDetailViewModel(fakeApi, null, fakeDownloadRepo)

        viewModel.loadPlaylist("pl_download", "Download Playlist")
        advanceUntilIdle()

        var downloadResultSuccess = false
        viewModel.downloadAllTracks { res ->
            downloadResultSuccess = res.isSuccess
        }
        advanceUntilIdle()

        assertTrue(downloadResultSuccess)
        assertFalse(viewModel.isDownloadingAll.value)
        assertEquals(Pair(2, 2), viewModel.downloadProgress.value)
        assertEquals(listOf("t1", "t2"), fakeDownloadRepo.downloadTracksCalledWith?.map { it.id })
        assertEquals(setOf("t1", "t2"), fakeDownloadRepo.downloadedSet.value)
    }

    @Test
    fun `downloadedTrackIds refleja reactivamente las canciones descargadas de la playlist`() = runTest(testDispatcher) {
        val fakeApi = FakeMonkifyApi()
        val fakeDownloadRepo = FakeDownloadRepository(initialDownloaded = setOf("t1", "t3"))
        val viewModel = PlaylistDetailViewModel(fakeApi, null, fakeDownloadRepo)

        advanceUntilIdle()
        assertTrue(viewModel.downloadedTrackIds.value.contains("t1"))
        assertTrue(viewModel.downloadedTrackIds.value.contains("t3"))
        assertFalse(viewModel.downloadedTrackIds.value.contains("t2"))
    }

    @Test
    fun `toggleDownloadTrack descarga y alterna estado de cancion en la playlist`() = runTest(testDispatcher) {
        val fakeApi = FakeMonkifyApi()
        val fakeDownloadRepo = FakeDownloadRepository()
        val viewModel = PlaylistDetailViewModel(fakeApi, null, fakeDownloadRepo)

        val track = Track("t_toggle", "Toggle Title", "Artist", "Album", 120000, null, "https://stream")

        viewModel.toggleDownloadTrack(track)
        advanceUntilIdle()
        assertTrue(fakeDownloadRepo.isTrackDownloaded("t_toggle"))

        viewModel.toggleDownloadTrack(track)
        advanceUntilIdle()
        assertFalse(fakeDownloadRepo.isTrackDownloaded("t_toggle"))
    }
}

