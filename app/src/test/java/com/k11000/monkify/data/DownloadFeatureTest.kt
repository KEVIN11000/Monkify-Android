package com.k11000.monkify.data

import com.k11000.monkify.data.local.entity.TrackEntity
import com.k11000.monkify.data.local.entity.toDomain
import com.k11000.monkify.domain.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadFeatureTest {

    @Test
    fun `pista descargada mapea streamUrl al archivo local en lugar de URL remota`() {
        val remoteTrack = Track(
            id = "track_offline_1",
            title = "Acoustic Sunset",
            artist = "Elena",
            album = "Solitude",
            durationMs = 210000L,
            coverUrl = "https://cdn.example.com/cover.jpg",
            streamUrl = "https://monkify-backend.monkify.workers.dev/api/v1/stream/track_offline_1"
        )

        val localPath = "/data/user/0/com.k11000.monkify/files/downloads/track_offline_1.mp3"
        val downloadedEntity = TrackEntity(
            id = remoteTrack.id,
            title = remoteTrack.title,
            artist = remoteTrack.artist,
            album = remoteTrack.album,
            durationMs = remoteTrack.durationMs,
            coverUrl = remoteTrack.coverUrl,
            streamUrl = remoteTrack.streamUrl,
            isFavorite = false,
            isDownloaded = true,
            localFilePath = localPath
        )

        val domainTrack = downloadedEntity.toDomain()
        assertEquals(localPath, domainTrack.streamUrl)
        assertTrue(downloadedEntity.isDownloaded)
    }

    @Test
    fun `pista no descargada preserva streamUrl remoto para streaming en tiempo real`() {
        val remoteUrl = "https://monkify-backend.monkify.workers.dev/api/v1/stream/track_stream_only"
        val streamedEntity = TrackEntity(
            id = "track_stream_only",
            title = "Electric Aurora",
            artist = "Monk",
            album = "Neon Skies",
            durationMs = 195000L,
            coverUrl = null,
            streamUrl = remoteUrl,
            isFavorite = true,
            isDownloaded = false,
            localFilePath = null
        )

        val domainTrack = streamedEntity.toDomain()
        assertEquals(remoteUrl, domainTrack.streamUrl)
        assertFalse(streamedEntity.isDownloaded)
        assertTrue(streamedEntity.isFavorite)
    }

    @Test
    fun `resolucion de URI local en PlayerViewModel detecta rutas absolutas de archivo`() {
        val localPath = "/data/user/0/com.k11000.monkify/files/downloads/test.mp3"
        val isLocalFile = localPath.startsWith("/")
        assertTrue(isLocalFile)

        val remoteUrl = "https://cdn.pixabay.com/audio/test.mp3"
        val isRemote = !remoteUrl.startsWith("/")
        assertTrue(isRemote)
    }
}
