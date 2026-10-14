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
}
