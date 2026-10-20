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
        override suspend fun getTracks(limit: Int, offset: Int, query: String?): List<Track> = tracksProvider(limit, offset)
        override suspend fun getLatestUploadedTracks(limit: Int): List<Track> = tracksProvider(limit, 0)
        override suspend fun recordTrackPlayed(trackId: String) {}
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
    fun `carga inicial expone latestUploadedTracks desde repositorio`() = runBlocking {
        val fakeRepo = FakeTrackRepository { limit, _ ->
            createSampleTracks(limit)
        }
        val viewModel = HomeViewModel(fakeRepo)
        assertEquals(6, viewModel.latestUploadedTracks.value.size)
        assertEquals("track_1", viewModel.latestUploadedTracks.value.first().id)
    }

    @Test
    fun `getDynamicGreeting retorna saludo y frase adecuada para las 7 franjas del dia`() {
        val fakeRepo = FakeTrackRepository { _, _ -> emptyList() }
        val viewModel = HomeViewModel(fakeRepo)

        // 1. Madrugada (00:00 - 04:59)
        val madrugada = viewModel.getDynamicGreeting(3)
        assertEquals("Buenas noches", madrugada.title)
        assertEquals("La noche es joven y la música no duerme", madrugada.phrase)

        // 2. Amanecer (05:00 - 06:59)
        val amanecer = viewModel.getDynamicGreeting(5)
        assertEquals("Buenos días", amanecer.title)
        assertEquals("Comienza el día con buen ritmo", amanecer.phrase)

        // 3. Mañana (07:00 - 11:59)
        val manana = viewModel.getDynamicGreeting(9)
        assertEquals("Buenos días", manana.title)
        assertEquals("Que la música impulse tu día", manana.phrase)

        // 4. Mediodía (12:00 - 13:59)
        val mediodia = viewModel.getDynamicGreeting(13)
        assertEquals("Buenas tardes", mediodia.title)
        assertEquals("Pausa para disfrutar de tus canciones", mediodia.phrase)

        // 5. Tarde (14:00 - 17:59)
        val tarde = viewModel.getDynamicGreeting(16)
        assertEquals("Buenas tardes", tarde.title)
        assertEquals("Buena música para seguir adelante", tarde.phrase)

        // 6. Atardecer (18:00 - 19:59)
        val atardecer = viewModel.getDynamicGreeting(19)
        assertEquals("Buenas tardes", atardecer.title)
        assertEquals("El día cae, el ritmo sigue", atardecer.phrase)

        // 7. Noche (20:00 - 23:59)
        val noche = viewModel.getDynamicGreeting(22)
        assertEquals("Buenas noches", noche.title)
        assertEquals("Relájate y disfruta del sonido", noche.phrase)
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

    private class FakeSyncScheduler : com.k11000.monkify.domain.repository.SyncScheduler {
        val _status = kotlinx.coroutines.flow.MutableStateFlow(com.k11000.monkify.domain.model.SyncStatus.IDLE)
        override val syncStatus: Flow<com.k11000.monkify.domain.model.SyncStatus> = _status

        val _timestamp = kotlinx.coroutines.flow.MutableStateFlow(0L)
        override val lastSyncTimestamp: Flow<Long> = _timestamp

        var immediateSyncCalled = false
        var periodicSyncCalled = false

        override fun schedulePeriodicSync() {
            periodicSyncCalled = true
        }

        override fun scheduleImmediateSync() {
            immediateSyncCalled = true
            _status.value = com.k11000.monkify.domain.model.SyncStatus.SYNCING
        }

        override fun cancelSync() {
            _status.value = com.k11000.monkify.domain.model.SyncStatus.IDLE
        }
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

    @Test
    fun `refresh invoca scheduleImmediateSync en SyncScheduler y recarga catalogo`() = runBlocking {
        val fakeRepo = FakeTrackRepository { limit, offset ->
            createSampleTracks(5)
        }
        val fakeScheduler = FakeSyncScheduler()
        val viewModel = HomeViewModel(
            trackRepository = fakeRepo,
            downloadRepository = null,
            syncScheduler = fakeScheduler
        )

        assertFalse(fakeScheduler.immediateSyncCalled)
        viewModel.refresh()
        assertTrue(fakeScheduler.immediateSyncCalled)
        assertEquals(com.k11000.monkify.domain.model.SyncStatus.SYNCING, viewModel.syncStatus.value)
    }

    @Test
    fun `requestSync invoca planificador bajo demanda y expone lastSyncTimestamp`() = runBlocking {
        val fakeRepo = FakeTrackRepository { _, _ -> emptyList() }
        val fakeScheduler = FakeSyncScheduler()
        fakeScheduler._timestamp.value = 1729000000000L

        val viewModel = HomeViewModel(
            trackRepository = fakeRepo,
            downloadRepository = null,
            syncScheduler = fakeScheduler
        )

        assertEquals(1729000000000L, viewModel.lastSyncTimestamp.value)
        viewModel.requestSync()
        assertTrue(fakeScheduler.immediateSyncCalled)
    }

    @Test
    fun `setFilter conmuta reactivamente entre ALL, MUSIC, ALBUMS y PODCASTS`() = runBlocking {
        val fakeRepo = FakeTrackRepository { _, _ -> emptyList() }
        val viewModel = HomeViewModel(fakeRepo)

        assertEquals(com.k11000.monkify.ui.screens.viewmodel.HomeFilter.ALL, viewModel.selectedFilter.value)

        viewModel.setFilter(com.k11000.monkify.ui.screens.viewmodel.HomeFilter.MUSIC)
        assertEquals(com.k11000.monkify.ui.screens.viewmodel.HomeFilter.MUSIC, viewModel.selectedFilter.value)

        viewModel.setFilter(com.k11000.monkify.ui.screens.viewmodel.HomeFilter.ALBUMS)
        assertEquals(com.k11000.monkify.ui.screens.viewmodel.HomeFilter.ALBUMS, viewModel.selectedFilter.value)

        viewModel.setFilter(com.k11000.monkify.ui.screens.viewmodel.HomeFilter.PODCASTS)
        assertEquals(com.k11000.monkify.ui.screens.viewmodel.HomeFilter.PODCASTS, viewModel.selectedFilter.value)
    }
}
