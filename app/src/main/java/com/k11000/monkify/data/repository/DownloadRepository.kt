package com.k11000.monkify.data.repository

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.DownloadManager
import com.k11000.monkify.domain.model.Track
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@UnstableApi
@Singleton
class DownloadRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val localTrackRepository: LocalTrackRepository,
    private val okHttpClient: OkHttpClient,
    private val downloadManager: DownloadManager
) {

    private val _downloadingTrackIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadingTrackIds: StateFlow<Set<String>> = _downloadingTrackIds.asStateFlow()

    val downloadedTrackIds: Flow<Set<String>> = localTrackRepository.downloadedTracks.map { tracks ->
        tracks.map { it.id }.toSet()
    }

    suspend fun isDownloaded(trackId: String): Boolean = withContext(Dispatchers.IO) {
        val file = getDownloadFile(trackId)
        file.exists() && file.length() > 0
    }

    suspend fun downloadTrack(track: Track): Boolean = withContext(Dispatchers.IO) {
        _downloadingTrackIds.value = _downloadingTrackIds.value + track.id
        try {
            val downloadDir = File(context.filesDir, "downloads")
            if (!downloadDir.exists()) downloadDir.mkdirs()

            val targetFile = File(downloadDir, "${track.id}.mp3")

            // Si ya existe y es válido, registrar en Room directamente
            if (targetFile.exists() && targetFile.length() > 0) {
                localTrackRepository.markAsDownloaded(track, targetFile.absolutePath)
                return@withContext true
            }

            val request = Request.Builder()
                .url(track.streamUrl)
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext false
            }

            response.body?.byteStream()?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }

            // Registrar en Room DB para acceso offline permanente
            localTrackRepository.markAsDownloaded(track, targetFile.absolutePath)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            _downloadingTrackIds.value = _downloadingTrackIds.value - track.id
        }
    }

    suspend fun removeDownload(trackId: String) = withContext(Dispatchers.IO) {
        val file = getDownloadFile(trackId)
        if (file.exists()) {
            file.delete()
        }
        localTrackRepository.removeDownload(trackId)
    }

    suspend fun toggleDownload(track: Track) {
        val file = getDownloadFile(track.id)
        if (file.exists() && file.length() > 0) {
            removeDownload(track.id)
        } else {
            downloadTrack(track)
        }
    }

    private fun getDownloadFile(trackId: String): File {
        val downloadDir = File(context.filesDir, "downloads")
        return File(downloadDir, "${trackId}.mp3")
    }
}
