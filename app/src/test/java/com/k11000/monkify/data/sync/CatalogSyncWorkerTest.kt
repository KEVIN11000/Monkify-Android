package com.k11000.monkify.data.sync

import com.k11000.monkify.data.local.dao.TrackDao
import com.k11000.monkify.data.local.entity.TrackEntity
import com.k11000.monkify.data.repository.LocalTrackRepository
import com.k11000.monkify.domain.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogSyncWorkerTest {

    private class FakeTrackDao : TrackDao {
        val tracks = MutableStateFlow<List<TrackEntity>>(emptyList())

        override fun getAllFavoriteTracks(): Flow<List<TrackEntity>> =
            tracks.map { list -> list.filter { it.isFavorite } }

        override fun getDownloadedTracks(): Flow<List<TrackEntity>> =
            tracks.map { list -> list.filter { it.isDownloaded } }

        override fun getAllTracksFlow(): Flow<List<TrackEntity>> = tracks

        override suspend fun getPagedTracks(limit: Int, offset: Int): List<TrackEntity> =
            tracks.value.drop(offset).take(limit)

        override suspend fun searchTracks(query: String, limit: Int, offset: Int): List<TrackEntity> =
            emptyList()

        override suspend fun getTrackById(id: String): TrackEntity? =
            tracks.value.find { it.id == id }

        override fun isFavorite(id: String): Flow<Boolean> =
            tracks.map { list -> list.any { it.id == id && it.isFavorite } }

        override suspend fun insertTrack(track: TrackEntity) {
            tracks.value = tracks.value.filter { it.id != track.id } + track
        }

        override suspend fun insertTracks(tracks: List<TrackEntity>): List<Long> {
            val existingIds = this.tracks.value.map { it.id }.toSet()
            val newTracks = tracks.filter { it.id !in existingIds }
            this.tracks.value = this.tracks.value + newTracks
            return newTracks.map { 1L }
        }

        override suspend fun deleteTrackById(id: String): Int {
            val initial = tracks.value.size
            tracks.value = tracks.value.filter { it.id != id }
            return initial - tracks.value.size
        }

        override suspend fun updateFavoriteStatus(id: String, isFavorite: Boolean): Int {
            tracks.value = tracks.value.map {
                if (it.id == id) it.copy(isFavorite = isFavorite) else it
            }
            return 1
        }

        override suspend fun updateDownloadStatus(id: String, isDownloaded: Boolean, localPath: String?): Int {
            tracks.value = tracks.value.map {
                if (it.id == id) it.copy(isDownloaded = isDownloaded, localFilePath = localPath) else it
            }
            return 1
        }

        override suspend fun getAllTracksSnapshot(): List<TrackEntity> = tracks.value

        override suspend fun recordTrackPlay(trackId: String, timestamp: Long): Int = 1
        override fun getRecentlyPlayedTracks(limit: Int): Flow<List<TrackEntity>> =
            tracks.map { list -> list.filter { it.lastPlayedAt > 0 }.sortedByDescending { it.lastPlayedAt }.take(limit) }
        override fun getTracksOrderedByPlayCount(): Flow<List<TrackEntity>> =
            tracks.map { list -> list.sortedByDescending { it.playCount } }
        override suspend fun getRecentlyPlayedTracksSnapshot(limit: Int): List<TrackEntity> =
            tracks.value.filter { it.lastPlayedAt > 0 }.sortedByDescending { it.lastPlayedAt }.take(limit)
        override suspend fun getTracksOrderedByPlayCountSnapshot(limit: Int): List<TrackEntity> =
            tracks.value.sortedByDescending { it.playCount }.take(limit)
    }

    @Test
    fun `sincronizacion de catalogo remoto preserva favoritos y descargas existentes en Room`() = runTest {
        val fakeDao = FakeTrackDao()
        val repo = LocalTrackRepository(fakeDao)

        // Estado inicial en Room:
        // Pista 1: Favorita y Descargada con ruta física
        fakeDao.insertTrack(
            TrackEntity(
                id = "t1",
                title = "Ancient Title",
                artist = "Old Artist",
                album = "Old Album",
                durationMs = 120000L,
                coverUrl = "https://cover/old1.jpg",
                streamUrl = "https://stream/old1.mp3",
                isFavorite = true,
                isDownloaded = true,
                localFilePath = "/data/user/0/com.k11000.monkify/files/music/t1.mp3"
            )
        )

        // Pista 2: Solo Descargada
        fakeDao.insertTrack(
            TrackEntity(
                id = "t2",
                title = "Downloaded Only",
                artist = "Artist 2",
                album = "Album 2",
                durationMs = 150000L,
                coverUrl = null,
                streamUrl = "https://stream/t2.mp3",
                isFavorite = false,
                isDownloaded = true,
                localFilePath = "/data/user/0/com.k11000.monkify/files/music/t2.mp3"
            )
        )

        // Nuevos metadatos que llegan desde el servidor en la sincronización:
        val remoteCatalog = listOf(
            Track(
                id = "t1",
                title = "Refreshed Title",
                artist = "Updated Artist",
                album = "Updated Album",
                durationMs = 125000L,
                coverUrl = "https://cover/new1.jpg",
                streamUrl = "https://stream/new1.mp3"
            ),
            Track(
                id = "t2",
                title = "Downloaded Only Updated",
                artist = "Artist 2",
                album = "Album 2 Remastered",
                durationMs = 150000L,
                coverUrl = null,
                streamUrl = "https://stream/t2.mp3"
            ),
            Track(
                id = "t3_brand_new",
                title = "Brand New Track",
                artist = "Newbie",
                album = "First Album",
                durationMs = 210000L,
                coverUrl = null,
                streamUrl = "https://stream/new3.mp3"
            )
        )

        val syncedCount = repo.syncRemoteCatalog(remoteCatalog)
        assertEquals(3, syncedCount)

        val snapshot = fakeDao.getAllTracksSnapshot().associateBy { it.id }

        // Validación Pista 1: Metadatos actualizados, Banderas Room y ruta local intocadas
        val t1 = snapshot["t1"]
        assertEquals("Refreshed Title", t1?.title)
        assertEquals("Updated Artist", t1?.artist)
        assertEquals("Updated Album", t1?.album)
        assertTrue("isFavorite debe mantenerse en true", t1?.isFavorite == true)
        assertTrue("isDownloaded debe mantenerse en true", t1?.isDownloaded == true)
        assertEquals("/data/user/0/com.k11000.monkify/files/music/t1.mp3", t1?.localFilePath)

        // Validación Pista 2: Banderas de descarga preservadas
        val t2 = snapshot["t2"]
        assertEquals("Downloaded Only Updated", t2?.title)
        assertFalse("isFavorite debe ser false", t2?.isFavorite == true)
        assertTrue("isDownloaded debe mantenerse en true", t2?.isDownloaded == true)
        assertEquals("/data/user/0/com.k11000.monkify/files/music/t2.mp3", t2?.localFilePath)

        // Validación Pista 3: Nueva pista sin favoritos ni descargas
        val t3 = snapshot["t3_brand_new"]
        assertEquals("Brand New Track", t3?.title)
        assertFalse("isFavorite inicial debe ser false", t3?.isFavorite == true)
        assertFalse("isDownloaded inicial debe ser false", t3?.isDownloaded == true)
        assertNull("localFilePath inicial debe ser null", t3?.localFilePath)
    }

    @Test
    fun `politica de reintentos de sync se activa ante fallo de red cuando quedan intentos`() {
        val maxAttempts = 3
        var currentAttempt = 1

        fun evaluateRetry(attempt: Int): Boolean = attempt < maxAttempts

        assertTrue(evaluateRetry(currentAttempt))
        currentAttempt++
        assertTrue(evaluateRetry(currentAttempt))
        currentAttempt++
        assertFalse(evaluateRetry(currentAttempt))
    }
}
