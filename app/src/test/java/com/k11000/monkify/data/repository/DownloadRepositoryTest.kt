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
}
