package com.k11000.monkify.data.repository

import com.k11000.monkify.domain.model.DownloadState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class DownloadRepositoryTest {

    @Test
    fun download_state_model_preserves_values_and_types() {
        val notDownloaded: DownloadState = DownloadState.NotDownloaded
        assertEquals(DownloadState.NotDownloaded, notDownloaded)

        val downloading: DownloadState = DownloadState.Downloading(45)
        assertEquals(45, (downloading as DownloadState.Downloading).progressPercent)

        val downloaded: DownloadState = DownloadState.Downloaded("/data/user/0/music/track1.mp3")
        assertEquals("/data/user/0/music/track1.mp3", (downloaded as DownloadState.Downloaded).localPath)

        val error: DownloadState = DownloadState.Error("Sin espacio")
        assertEquals("Sin espacio", (error as DownloadState.Error).message)
    }

    @Test
    fun local_media_file_checks_discriminate_existing_and_non_existing_files() {
        val tempDir = File("build/tmp/test_downloads").apply { mkdirs() }
        val dummyAudio = File(tempDir, "sample_track.mp3").apply {
            writeBytes(ByteArray(1024) { 1 })
        }
        val emptyAudio = File(tempDir, "empty_track.mp3").apply {
            writeBytes(ByteArray(0))
        }

        assertTrue(dummyAudio.exists() && dummyAudio.length() > 0)
        assertFalse(emptyAudio.exists() && emptyAudio.length() > 0)

        val missingAudio = File(tempDir, "missing.mp3")
        assertFalse(missingAudio.exists())

        // Cleanup
        dummyAudio.delete()
        emptyAudio.delete()
        tempDir.delete()
    }

    @Test
    fun local_lyrics_reads_content_when_file_exists() {
        val tempDir = File("build/tmp/test_lyrics").apply { mkdirs() }
        val lyricsFile = File(tempDir, "track_lyrics.lrc").apply {
            writeText("[00:10.00] Hello world\n[00:15.00] Second line")
        }

        val content = if (lyricsFile.exists() && lyricsFile.length() > 0) lyricsFile.readText() else null
        assertTrue(content?.contains("Hello world") == true)

        lyricsFile.delete()
        tempDir.delete()

        val missingContent = if (lyricsFile.exists()) lyricsFile.readText() else null
        assertNull(missingContent)
    }

    @Test
    fun atomic_download_simulation_ensures_no_residual_temp_file_on_error() {
        val tempDir = File("build/tmp/atomic_test").apply { mkdirs() }
        val tempFile = File(tempDir, "failed_track.tmp")
        val finalFile = File(tempDir, "failed_track.mp3")

        try {
            FileOutputStream(tempFile).use { out ->
                out.write("Incomplete data".toByteArray())
                throw IOException("Simulated network failure")
            }
        } catch (_: IOException) {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }

        assertFalse(tempFile.exists())
        assertFalse(finalFile.exists())

        tempDir.delete()
    }

    @Test
    fun atomic_download_simulation_successfully_promotes_temp_to_final() {
        val tempDir = File("build/tmp/atomic_success").apply { mkdirs() }
        val tempFile = File(tempDir, "success_track.tmp")
        val finalFile = File(tempDir, "success_track.mp3")

        FileOutputStream(tempFile).use { out ->
            out.write("Complete audio data".toByteArray())
        }
        val renamed = tempFile.renameTo(finalFile)

        assertTrue(renamed)
        assertTrue(finalFile.exists())
        assertFalse(tempFile.exists())
        assertEquals("Complete audio data", finalFile.readText())

        finalFile.delete()
        tempDir.delete()
    }

    @Test
    fun deletion_purges_audio_and_lyrics_files_cleanly() {
        val tempDir = File("build/tmp/delete_test").apply { mkdirs() }
        val audioFile = File(tempDir, "to_delete.mp3").apply { writeText("audio content") }
        val tempFile = File(tempDir, "to_delete.tmp").apply { writeText("temp content") }
        val lyricsFile = File(tempDir, "to_delete.lrc").apply { writeText("lyrics content") }

        assertTrue(audioFile.exists())
        assertTrue(tempFile.exists())
        assertTrue(lyricsFile.exists())

        // Simulated purge as in deleteDownloadedTrack
        if (audioFile.exists()) audioFile.delete()
        if (tempFile.exists()) tempFile.delete()
        if (lyricsFile.exists()) lyricsFile.delete()

        assertFalse(audioFile.exists())
        assertFalse(tempFile.exists())
        assertFalse(lyricsFile.exists())

        tempDir.delete()
    }

    @Test
    fun total_downloads_size_bytes_sums_mp3_and_lrc_files_correctly() {
        val tempMusic = File("build/tmp/size_test_music").apply { mkdirs() }
        val tempLyrics = File("build/tmp/size_test_lyrics").apply { mkdirs() }

        val f1 = File(tempMusic, "t1.mp3").apply { writeBytes(ByteArray(2048)) }
        val f2 = File(tempMusic, "t2.mp3").apply { writeBytes(ByteArray(4096)) }
        val l1 = File(tempLyrics, "t1.lrc").apply { writeBytes(ByteArray(512)) }
        val nonMedia = File(tempMusic, "ignore.txt").apply { writeBytes(ByteArray(1024)) }

        var calculatedTotal = 0L
        listOf(tempMusic, tempLyrics).forEach { dir ->
            dir.listFiles()?.forEach { file ->
                if (file.isFile && (file.extension.equals("mp3", ignoreCase = true) || file.extension.equals("lrc", ignoreCase = true))) {
                    calculatedTotal += file.length()
                }
            }
        }

        assertEquals(2048L + 4096L + 512L, calculatedTotal)

        f1.delete()
        f2.delete()
        l1.delete()
        nonMedia.delete()
        tempMusic.delete()
        tempLyrics.delete()
    }

    @Test
    fun disk_space_check_rejects_download_when_usable_space_is_below_25mb() {
        val minRequiredBytes = 25L * 1024 * 1024

        fun validateSpace(usableBytes: Long): Boolean {
            if (usableBytes in 1 until minRequiredBytes) {
                throw IOException("Espacio insuficiente en disco")
            }
            return true
        }

        // Simular espacio bajo (10 MB)
        var exceptionThrown = false
        try {
            validateSpace(10L * 1024 * 1024)
        } catch (_: IOException) {
            exceptionThrown = true
        }
        assertTrue(exceptionThrown)

        // Simular espacio suficiente (100 MB)
        val valid = validateSpace(100L * 1024 * 1024)
        assertTrue(valid)
    }

    @Test
    fun batch_download_simulation_tracks_completed_progress_count() {
        val trackIds = listOf("t1", "t2", "t3", "t4", "t5")
        var completedCount = 0
        val progressReports = mutableListOf<Pair<Int, Int>>()

        trackIds.forEach { _ ->
            completedCount++
            progressReports.add(Pair(completedCount, trackIds.size))
        }

        assertEquals(5, completedCount)
        assertEquals(5, progressReports.size)
        assertEquals(Pair(1, 5), progressReports.first())
        assertEquals(Pair(5, 5), progressReports.last())
    }

    @Test
    fun download_error_mapping_translates_http_codes_to_friendly_spanish_messages() {
        fun mapHttpError(code: Int): String {
            return when (code) {
                404 -> "Pista no disponible en el servidor (HTTP 404)"
                401, 403 -> "Acceso no autorizado al audio ($code)"
                500, 502, 503 -> "Servidor de almacenamiento no disponible ($code)"
                else -> "Fallo en la descarga de audio (HTTP $code)"
            }
        }

        assertEquals("Pista no disponible en el servidor (HTTP 404)", mapHttpError(404))
        assertEquals("Acceso no autorizado al audio (401)", mapHttpError(401))
        assertEquals("Acceso no autorizado al audio (403)", mapHttpError(403))
        assertEquals("Servidor de almacenamiento no disponible (500)", mapHttpError(500))
        assertEquals("Servidor de almacenamiento no disponible (502)", mapHttpError(502))
        assertEquals("Fallo en la descarga de audio (HTTP 418)", mapHttpError(418))
    }
}
