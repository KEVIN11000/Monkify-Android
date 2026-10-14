package com.k11000.monkify.ui.viewmodel

import com.k11000.monkify.data.remote.dto.IngestResponse
import com.k11000.monkify.domain.model.Track
import com.k11000.monkify.domain.repository.TrackRepository
import com.k11000.monkify.ui.screens.viewmodel.SearchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeTrackRepository : TrackRepository {
        var lastQuery: String? = null
        var queryCallCount = 0

        override fun getRecentTracks(): Flow<List<Track>> = flowOf(emptyList())
        override fun getMadeForYouTracks(): Flow<List<Track>> = flowOf(emptyList())

        override suspend fun getTracks(limit: Int, offset: Int, query: String?): List<Track> {
            lastQuery = query
            queryCallCount++
            return if (!query.isNullOrBlank()) {
                listOf(
                    Track(
                        id = "track_1",
                        title = "Gabriel's Oboe",
                        artist = "Ennio Morricone",
                        album = "The Mission",
                        durationMs = 180000L,
                        coverUrl = null,
                        streamUrl = "https://stream/1"
                    )
                )
            } else {
                emptyList()
            }
        }

        override suspend fun requestIngest(url: String): Result<IngestResponse> = error("unused")
    }

    @Test
    fun `emitir texto en searchQuery con debounce dispara busqueda y actualiza searchResults`() = runTest(testDispatcher) {
        val fakeRepo = FakeTrackRepository()
        val viewModel = SearchViewModel(fakeRepo)

        // Estado inicial
        assertEquals("", viewModel.searchQuery.value)
        assertTrue(viewModel.searchResults.value.isEmpty())
        assertFalse(viewModel.isSearching.value)

        // Cambiar query
        viewModel.onQueryChange("Gabriel")
        assertEquals("Gabriel", viewModel.searchQuery.value)

        // Antes del debounce (400ms) no se debe haber consultado el repositorio
        advanceTimeBy(200)
        runCurrent()
        assertEquals(0, fakeRepo.queryCallCount)
        assertTrue(viewModel.searchResults.value.isEmpty())

        // Avanzar el tiempo más allá del debounce
        advanceTimeBy(300)
        runCurrent()

        // Verificaciones
        assertEquals(1, fakeRepo.queryCallCount)
        assertEquals("Gabriel", fakeRepo.lastQuery)
        assertEquals(1, viewModel.searchResults.value.size)
        assertEquals("Gabriel's Oboe", viewModel.searchResults.value.first().title)
        assertFalse(viewModel.isSearching.value)
    }

    @Test
    fun `limpiar query con clearQuery vacia los resultados y resetea el estado`() = runTest(testDispatcher) {
        val fakeRepo = FakeTrackRepository()
        val viewModel = SearchViewModel(fakeRepo)

        viewModel.onQueryChange("Gabriel")
        advanceTimeBy(500)
        runCurrent()
        assertEquals(1, viewModel.searchResults.value.size)

        // Limpiar query
        viewModel.clearQuery()
        assertEquals("", viewModel.searchQuery.value)
        assertTrue(viewModel.searchResults.value.isEmpty())
        assertFalse(viewModel.isSearching.value)
    }

    @Test
    fun `seleccionar una pista actualiza selectedTrack en el ViewModel`() = runTest(testDispatcher) {
        val fakeRepo = FakeTrackRepository()
        val viewModel = SearchViewModel(fakeRepo)
        val sampleTrack = Track("t1", "Track 1", "Artist 1", "Album 1", 120000, null, "https://stream/1")

        viewModel.onTrackSelected(sampleTrack)
        assertEquals("t1", viewModel.selectedTrack.value?.id)
        assertEquals("Track 1", viewModel.selectedTrack.value?.title)
    }
}
