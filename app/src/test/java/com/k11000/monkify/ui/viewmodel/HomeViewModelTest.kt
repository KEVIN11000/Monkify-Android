package com.k11000.monkify.ui.viewmodel

import com.k11000.monkify.data.remote.dto.IngestResponse
import com.k11000.monkify.domain.model.Track
import com.k11000.monkify.domain.repository.TrackRepository
import com.k11000.monkify.ui.screens.viewmodel.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createSampleTracks(count: Int, startId: Int = 1): List<Track> {
        return (startId until startId + count).map { id ->
            Track(
                id = "track_$id",
                title = "Title $id",
                artist = "Artist $id",
                album = "Album $id",
                durationMs = 180000L,
                coverUrl = "https://example.com/cover_$id.jpg",
                streamUrl = "https://example.com/stream_$id.mp3"
            )
        }
    }

    private class FakeTrackRepository(
        private val tracksProvider: (limit: Int, offset: Int) -> List<Track>
    ) : TrackRepository {
        override fun getRecentTracks(): Flow<List<Track>> = flowOf(tracksProvider(6, 0))
        override fun getMadeForYouTracks(): Flow<List<Track>> = flowOf(tracksProvider(10, 0))
        override suspend fun getTracks(limit: Int, offset: Int): List<Track> = tracksProvider(limit, offset)
        override suspend fun requestIngest(url: String): Result<IngestResponse> = error("unused")
    }

    @Test
    fun `carga inicial puebla feedTracks, recentTracks y hasMore`() = runBlocking {
        val fakeRepo = FakeTrackRepository { limit, offset ->
            if (offset == 0) createSampleTracks(HomeViewModel.PAGE_SIZE) else emptyList()
        }
        val viewModel = HomeViewModel(fakeRepo)

        assertEquals(HomeViewModel.PAGE_SIZE, viewModel.feedTracks.value.size)
        assertEquals(6, viewModel.recentTracks.value.size)
        assertTrue(viewModel.hasMore.value)
    }

    @Test
    fun `loadMore concatena paginas y actualiza hasMore al llegar al final`() = runBlocking {
        val allCatalog = createSampleTracks(25)
        val fakeRepo = FakeTrackRepository { limit, offset ->
            allCatalog.drop(offset).take(limit)
        }
        val viewModel = HomeViewModel(fakeRepo)
        assertEquals(20, viewModel.feedTracks.value.size)
        assertTrue(viewModel.hasMore.value)

        viewModel.loadMore()
        assertEquals(25, viewModel.feedTracks.value.size)
        assertFalse(viewModel.hasMore.value)
    }

    @Test
    fun `hasMore se desactiva si la primera pagina trae menos de PAGE_SIZE`() = runBlocking {
        val smallCatalog = createSampleTracks(5)
        val fakeRepo = FakeTrackRepository { limit, offset ->
            smallCatalog.drop(offset).take(limit)
        }
        val viewModel = HomeViewModel(fakeRepo)
        assertEquals(5, viewModel.feedTracks.value.size)
        assertFalse(viewModel.hasMore.value)
    }
}
