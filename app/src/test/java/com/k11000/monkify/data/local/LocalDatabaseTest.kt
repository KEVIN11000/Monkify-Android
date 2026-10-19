package com.k11000.monkify.data.local

import com.k11000.monkify.data.local.entity.TrackEntity
import com.k11000.monkify.data.local.entity.toDomain
import com.k11000.monkify.data.local.entity.toEntity
import com.k11000.monkify.domain.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalDatabaseTest {

    @Test
    fun `nombre de la base de datos Room es monkify_local db`() {
        assertEquals("monkify_local.db", MonkifyDatabase.DATABASE_NAME)
    }

    @Test
    fun `conversion de TrackEntity a Track domain preserva datos y resuelve streamUrl`() {
        val entity = TrackEntity(
            id = "t1",
            title = "Midnight Resonance",
            artist = "Aether",
            album = "Cosmic Dreams",
            durationMs = 210000L,
            coverUrl = "https://example.com/cover.jpg",
            streamUrl = "https://example.com/stream/t1",
            isFavorite = true,
            isDownloaded = false,
            localFilePath = null
        )

        val domain = entity.toDomain()
        assertEquals("t1", domain.id)
        assertEquals("Midnight Resonance", domain.title)
        assertEquals("Aether", domain.artist)
        assertEquals("Cosmic Dreams", domain.album)
        assertEquals(210000L, domain.durationMs)
        assertEquals("https://example.com/cover.jpg", domain.coverUrl)
        assertEquals("https://example.com/stream/t1", domain.streamUrl)
    }

    @Test
    fun `conversion a domain prioriza localFilePath cuando el archivo esta descargado`() {
        val downloadedEntity = TrackEntity(
            id = "t2",
            title = "Solar Winds",
            artist = "Aether",
            album = "Cosmic Dreams",
            durationMs = 180000L,
            coverUrl = null,
            streamUrl = "https://example.com/stream/t2",
            isFavorite = false,
            isDownloaded = true,
            localFilePath = "/data/user/0/com.k11000.monkify/files/t2.mp3"
        )

        val domain = downloadedEntity.toDomain()
        assertEquals("/data/user/0/com.k11000.monkify/files/t2.mp3", domain.streamUrl)
    }

    @Test
    fun `conversion de domain Track a TrackEntity establece banderas correctamente`() {
        val track = Track(
            id = "t3",
            title = "Lunar Eclipse",
            artist = "Nova",
            album = "Night Sky",
            durationMs = 240000L,
            coverUrl = "https://example.com/lunar.jpg",
            streamUrl = "https://example.com/stream/t3"
        )

        val entityFav = track.toEntity(isFavorite = true, isDownloaded = false)
        assertTrue(entityFav.isFavorite)
        assertFalse(entityFav.isDownloaded)
        assertNull(entityFav.localFilePath)
        assertEquals("t3", entityFav.id)

        val entityDown = track.toEntity(isFavorite = false, isDownloaded = true, localFilePath = "/local/path.mp3")
        assertFalse(entityDown.isFavorite)
        assertTrue(entityDown.isDownloaded)
        assertEquals("/local/path.mp3", entityDown.localFilePath)
        assertEquals(0, entityDown.playCount)
        assertEquals(0L, entityDown.lastPlayedAt)
    }

    @Test
    fun `TrackEntity soporta conteo de reproduccion e historial temporal`() {
        val entity = TrackEntity(
            id = "t_history",
            title = "Test",
            artist = "Artist",
            album = "Album",
            durationMs = 1000L,
            coverUrl = null,
            streamUrl = "https://stream/th",
            playCount = 5,
            lastPlayedAt = 1729350000000L
        )

        assertEquals(5, entity.playCount)
        assertEquals(1729350000000L, entity.lastPlayedAt)
    }

    @Test
    fun `MIGRATION_1_2 migra version 1 a 2`() {
        assertEquals(1, MonkifyDatabase.MIGRATION_1_2.startVersion)
        assertEquals(2, MonkifyDatabase.MIGRATION_1_2.endVersion)
    }
}
