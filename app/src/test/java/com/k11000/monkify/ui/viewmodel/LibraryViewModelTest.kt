package com.k11000.monkify.ui.viewmodel

import com.k11000.monkify.data.local.dao.TrackDao
import com.k11000.monkify.data.local.entity.TrackEntity
import com.k11000.monkify.data.repository.LocalTrackRepository
import com.k11000.monkify.domain.model.Track
import com.k11000.monkify.ui.screens.viewmodel.LibraryFilter
import com.k11000.monkify.ui.screens.viewmodel.LibraryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeTrackDao : TrackDao {
        val tracksFlow = MutableStateFlow<List<TrackEntity>>(emptyList())

        override fun getAllFavoriteTracks(): Flow<List<TrackEntity>> =
            tracksFlow.map { list -> list.filter { it.isFavorite } }

        override fun getDownloadedTracks(): Flow<List<TrackEntity>> =
            tracksFlow.map { list -> list.filter { it.isDownloaded } }

        override suspend fun getTrackById(id: String): TrackEntity? =
            tracksFlow.value.find { it.id == id }

        override fun isFavorite(id: String): Flow<Boolean> =
            tracksFlow.map { list -> list.any { it.id == id && it.isFavorite } }

        override suspend fun insertTrack(track: TrackEntity) {
            tracksFlow.value = tracksFlow.value.filter { it.id != track.id } + track
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

        override fun getAllTracksFlow(): Flow<List<TrackEntity>> = tracksFlow

        override suspend fun getPagedTracks(limit: Int, offset: Int): List<TrackEntity> =
            tracksFlow.value.drop(offset).take(limit)

        override suspend fun searchTracks(query: String, limit: Int, offset: Int): List<TrackEntity> =
            tracksFlow.value.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true) ||
                it.album.contains(query, ignoreCase = true)
            }.drop(offset).take(limit)

        override suspend fun insertTracks(tracks: List<TrackEntity>): List<Long> {
            val existingIds = tracksFlow.value.map { it.id }.toSet()
            val newTracks = tracks.filter { it.id !in existingIds }
            tracksFlow.value = tracksFlow.value + newTracks
            return newTracks.map { 1L }
        }

        override suspend fun updateDownloadStatus(id: String, isDownloaded: Boolean, localPath: String?): Int {
            tracksFlow.value = tracksFlow.value.map {
                if (it.id == id) it.copy(isDownloaded = isDownloaded, localFilePath = localPath) else it
            }
            return 1
        }

        override suspend fun getAllTracksSnapshot(): List<TrackEntity> = tracksFlow.value
    }


    @Test
    fun `canciones insertadas en Room como favoritas y descargadas se reflejan en displayedTracks`() = runTest(testDispatcher) {
        val fakeDao = FakeTrackDao()
        val localRepo = LocalTrackRepository(fakeDao)

        // Insertar pista favorita
        val trackFav = Track("t_fav", "Song Favorite", "Artist 1", "Album 1", 180000, null, "https://stream/1")
        localRepo.saveTrack(trackFav, isFavorite = true, isDownloaded = false)

        // Insertar pista descargada offline
        val trackDown = Track("t_down", "Song Downloaded", "Artist 2", "Album 2", 200000, null, "https://stream/2")
        localRepo.markAsDownloaded(trackDown, "/data/user/0/com.k11000.monkify/files/t_down.mp3")

        advanceUntilIdle()

        val viewModel = LibraryViewModel(localRepo)
        advanceUntilIdle()

        // Filtro ALL: Debe mostrar ambas canciones
        assertEquals(LibraryFilter.ALL, viewModel.selectedFilter.value)
        assertEquals(2, viewModel.displayedTracks.value.size)
        assertEquals(1, viewModel.favoriteTracks.value.size)
        assertEquals(1, viewModel.downloadedTracks.value.size)
    }

    @Test
    fun `cambiar filtro a FAVORITES o DOWNLOADS filtra reactivamente las pistas locales`() = runTest(testDispatcher) {
        val fakeDao = FakeTrackDao()
        val localRepo = LocalTrackRepository(fakeDao)

        val trackFav = Track("t_fav", "Song Favorite", "Artist 1", "Album 1", 180000, null, "https://stream/1")
        val trackDown = Track("t_down", "Song Downloaded", "Artist 2", "Album 2", 200000, null, "https://stream/2")

        localRepo.saveTrack(trackFav, isFavorite = true, isDownloaded = false)
        localRepo.markAsDownloaded(trackDown, "/data/user/0/com.k11000.monkify/files/t_down.mp3")
        advanceUntilIdle()

        val viewModel = LibraryViewModel(localRepo)
        advanceUntilIdle()

        // Filtrar por FAVORITES
        viewModel.setFilter(LibraryFilter.FAVORITES)
        advanceUntilIdle()
        assertEquals(1, viewModel.displayedTracks.value.size)
        assertEquals("t_fav", viewModel.displayedTracks.value.first().id)

        // Filtrar por DOWNLOADS
        viewModel.setFilter(LibraryFilter.DOWNLOADS)
        advanceUntilIdle()
        assertEquals(1, viewModel.displayedTracks.value.size)
        assertEquals("t_down", viewModel.displayedTracks.value.first().id)

        // Volver a ALL
        viewModel.setFilter(LibraryFilter.ALL)
        advanceUntilIdle()
        assertEquals(2, viewModel.displayedTracks.value.size)
    }

    @Test
    fun `biblioteca vacia emite listas vacias para todos los filtros`() = runTest(testDispatcher) {
        val fakeDao = FakeTrackDao()
        val localRepo = LocalTrackRepository(fakeDao)
        val viewModel = LibraryViewModel(localRepo)

        advanceUntilIdle()

        assertTrue(viewModel.displayedTracks.value.isEmpty())
        assertTrue(viewModel.favoriteTracks.value.isEmpty())
        assertTrue(viewModel.downloadedTracks.value.isEmpty())
    }

    private class FakePlaylistRepo : com.k11000.monkify.domain.repository.PlaylistRepository {
        val playlists = mutableListOf<com.k11000.monkify.data.remote.dto.PlaylistSummaryDto>(
            com.k11000.monkify.data.remote.dto.PlaylistSummaryDto("pl_1", "Lofi Beats", 5)
        )

        override suspend fun getPlaylists(limit: Int, offset: Int): Result<List<com.k11000.monkify.data.remote.dto.PlaylistSummaryDto>> =
            Result.success(playlists.toList())

        override suspend fun getPlaylistTracks(playlistId: String): Result<List<Track>> =
            Result.success(emptyList())

        override suspend fun createPlaylist(name: String, description: String?): Result<com.k11000.monkify.data.remote.dto.PlaylistSummaryDto> {
            val newPl = com.k11000.monkify.data.remote.dto.PlaylistSummaryDto("user_pl_${playlists.size + 1}", name, 0)
            playlists.add(newPl)
            return Result.success(newPl)
        }

        override suspend fun deletePlaylist(playlistId: String): Result<Unit> {
            playlists.removeAll { it.id == playlistId }
            return Result.success(Unit)
        }

        override suspend fun addTrackToPlaylist(playlistId: String, trackId: String): Result<Unit> =
            Result.success(Unit)

        override suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String): Result<Unit> =
            Result.success(Unit)
    }

    @Test
    fun `createPlaylist invoca al repositorio y actualiza playlists en el viewModel`() = runTest(testDispatcher) {
        val fakeDao = FakeTrackDao()
        val localRepo = LocalTrackRepository(fakeDao)
        val fakePlaylistRepo = FakePlaylistRepo()
        val viewModel = LibraryViewModel(localRepo, playlistRepository = fakePlaylistRepo)

        advanceUntilIdle()
        assertEquals(1, viewModel.playlists.value.size)

        var successCallback = false
        viewModel.createPlaylist("Rock Classics") { success ->
            successCallback = success
        }
        advanceUntilIdle()

        assertTrue(successCallback)
        assertEquals(2, viewModel.playlists.value.size)
        assertEquals("Rock Classics", viewModel.playlists.value.last().name)
    }

    @Test
    fun `deletePlaylist elimina la playlist de la lista en memoria`() = runTest(testDispatcher) {
        val fakeDao = FakeTrackDao()
        val localRepo = LocalTrackRepository(fakeDao)
        val fakePlaylistRepo = FakePlaylistRepo()
        val viewModel = LibraryViewModel(localRepo, playlistRepository = fakePlaylistRepo)

        advanceUntilIdle()
        assertEquals(1, viewModel.playlists.value.size)

        var successCallback = false
        viewModel.deletePlaylist("pl_1") { success ->
            successCallback = success
        }
        advanceUntilIdle()

        assertTrue(successCallback)
        assertEquals(0, viewModel.playlists.value.size)
    }

    private class FakeDownloadRepo(
        var totalBytes: Long = 0L,
        initialDownloaded: Set<String> = emptySet()
    ) : com.k11000.monkify.domain.repository.DownloadRepository {
        val downloadedSet = MutableStateFlow(initialDownloaded)
        override val downloadedTrackIds: Flow<Set<String>> = downloadedSet
        override val downloadingTrackIds: kotlinx.coroutines.flow.StateFlow<Set<String>> = MutableStateFlow(emptySet())
        var deleteCalledWith: String? = null

        override fun getDownloadState(trackId: String) = kotlinx.coroutines.flow.flowOf(com.k11000.monkify.domain.model.DownloadState.NotDownloaded)
        override suspend fun downloadTrack(track: Track): Result<String> = Result.success("/path")
        override suspend fun downloadTracks(tracks: List<Track>, onProgress: (completed: Int, total: Int) -> Unit): Result<Unit> = Result.success(Unit)
        override suspend fun deleteDownloadedTrack(trackId: String): Result<Unit> {
            deleteCalledWith = trackId
            downloadedSet.value = downloadedSet.value - trackId
            totalBytes = (totalBytes - 1000000L).coerceAtLeast(0L)
            return Result.success(Unit)
        }
        override suspend fun toggleDownload(track: Track) {}
        override fun isTrackDownloaded(trackId: String): Boolean = trackId in downloadedSet.value
        override fun getLocalLyrics(trackId: String): String? = null
        override fun getLocalAudioPath(trackId: String): String? = null
        override fun getTotalDownloadsSizeBytes(): Long = totalBytes
    }

    @Test
    fun `totalStorageBytes refleja el tamano total calculado desde DownloadRepository`() = runTest(testDispatcher) {
        val fakeDao = FakeTrackDao()
        val localRepo = LocalTrackRepository(fakeDao)
        val fakeDownloadRepo = FakeDownloadRepo(totalBytes = 38400000L, initialDownloaded = setOf("t1", "t2"))
        val viewModel = LibraryViewModel(localRepo, downloadRepository = fakeDownloadRepo)

        advanceUntilIdle()
        assertEquals(38400000L, viewModel.totalStorageBytes.value)
    }

    @Test
    fun `deleteDownload invoca eliminacion y actualiza tamano y estado`() = runTest(testDispatcher) {
        val fakeDao = FakeTrackDao()
        val localRepo = LocalTrackRepository(fakeDao)
        val fakeDownloadRepo = FakeDownloadRepo(totalBytes = 15000000L, initialDownloaded = setOf("t_del"))
        val viewModel = LibraryViewModel(localRepo, downloadRepository = fakeDownloadRepo)

        advanceUntilIdle()
        var deleteResult: Result<Unit>? = null
        viewModel.deleteDownload("t_del") { res ->
            deleteResult = res
        }
        advanceUntilIdle()

        assertEquals("t_del", fakeDownloadRepo.deleteCalledWith)
        assertTrue(deleteResult?.isSuccess == true)
        assertFalse(viewModel.totalStorageBytes.value == 15000000L)
    }
}

