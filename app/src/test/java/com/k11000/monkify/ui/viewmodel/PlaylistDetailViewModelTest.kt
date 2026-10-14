package com.k11000.monkify.ui.viewmodel

import com.k11000.monkify.data.remote.api.MonkifyApi
import com.k11000.monkify.data.remote.dto.BackendTrackDto
import com.k11000.monkify.data.remote.dto.IngestRequest
import com.k11000.monkify.data.remote.dto.IngestResponse
import com.k11000.monkify.data.remote.dto.LoginRequest
import com.k11000.monkify.data.remote.dto.LoginResponse
import com.k11000.monkify.data.remote.dto.LyricsResponse
import com.k11000.monkify.data.remote.dto.PlaylistSummaryDto
import com.k11000.monkify.ui.screens.viewmodel.PlaylistDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
        private val tracksHandler: (playlistId: String) -> List<BackendTrackDto> = { emptyList() }
    ) : MonkifyApi {
        override suspend fun getTracks(limit: Int, offset: Int, query: String?): List<BackendTrackDto> = emptyList()
        override suspend fun login(request: LoginRequest): LoginResponse = LoginResponse("token", "ref", "bearer", 3600)
        override suspend fun ingest(request: IngestRequest): Response<IngestResponse> = Response.success(IngestResponse("t1", "OK", "msg"))
        override suspend fun getLyrics(trackId: String): LyricsResponse = LyricsResponse(trackId, "lrc", "")
        override suspend fun getPlaylists(limit: Int, offset: Int): List<PlaylistSummaryDto> = emptyList()
        override suspend fun getPlaylistTracks(id: String): List<BackendTrackDto> = tracksHandler(id)
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
}
