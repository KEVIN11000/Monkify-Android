package com.k11000.monkify.data.repository

import android.content.Context
import com.k11000.monkify.data.remote.api.MonkifyApi
import com.k11000.monkify.domain.model.DownloadState
import com.k11000.monkify.domain.model.Track
import com.k11000.monkify.domain.repository.DownloadRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val localTrackRepository: LocalTrackRepository,
    private val okHttpClient: OkHttpClient,
    private val monkifyApi: MonkifyApi
) : DownloadRepository {

    private val downloadStates = ConcurrentHashMap<String, MutableStateFlow<DownloadState>>()

    private val musicDir: File
        get() = File(context.filesDir, "music").apply { if (!exists()) mkdirs() }

    private val lyricsDir: File
        get() = File(context.filesDir, "lyrics").apply { if (!exists()) mkdirs() }

    fun getAudioFile(trackId: String): File = File(musicDir, "$trackId.mp3")

    fun getLyricsFile(trackId: String): File = File(lyricsDir, "$trackId.lrc")

    override fun getDownloadState(trackId: String): Flow<DownloadState> {
        val flow = downloadStates.computeIfAbsent(trackId) {
            val initialState = if (isTrackDownloaded(trackId)) {
                DownloadState.Downloaded(getAudioFile(trackId).absolutePath)
            } else {
                DownloadState.NotDownloaded
            }
            MutableStateFlow(initialState)
        }
        return flow.asStateFlow()
    }

    override fun isTrackDownloaded(trackId: String): Boolean {
        val file = getAudioFile(trackId)
        return file.exists() && file.length() > 0
    }

    override fun getLocalAudioPath(trackId: String): String? {
        val file = getAudioFile(trackId)
        return if (file.exists() && file.length() > 0) file.absolutePath else null
    }

    override fun getLocalLyrics(trackId: String): String? {
        val file = getLyricsFile(trackId)
        return if (file.exists() && file.length() > 0) {
            runCatching { file.readText() }.getOrNull()
        } else {
            null
        }
    }

    private fun resolveStreamUrl(streamUrl: String, trackId: String): String {
        return when {
            streamUrl.startsWith("http://") || streamUrl.startsWith("https://") -> streamUrl
            streamUrl.startsWith("/") -> "https://monkify-backend.monkify.workers.dev$streamUrl"
            else -> "https://monkify-backend.monkify.workers.dev/api/v1/stream/$trackId"
        }
    }

    override suspend fun downloadTrack(track: Track): Result<String> = withContext(Dispatchers.IO) {
        val stateFlow = downloadStates.computeIfAbsent(track.id) { MutableStateFlow(DownloadState.NotDownloaded) }
        stateFlow.value = DownloadState.Downloading(0)

        val tempAudioFile = File(musicDir, "${track.id}.tmp")
        val finalAudioFile = getAudioFile(track.id)

        try {
            val url = resolveStreamUrl(track.streamUrl, track.id)
            val request = okhttp3.Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                throw java.io.IOException("Fallo en la descarga de audio HTTP ${response.code}")
            }

            val body = response.body ?: throw java.io.IOException("Cuerpo de respuesta vacío")
            val totalBytes = body.contentLength()

            body.byteStream().use { input ->
                java.io.FileOutputStream(tempAudioFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesCopied = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        bytesCopied += read
                        if (totalBytes > 0) {
                            val percent = ((bytesCopied * 100) / totalBytes).toInt().coerceIn(0, 99)
                            stateFlow.value = DownloadState.Downloading(percent)
                        }
                    }
                    output.flush()
                }
            }

            if (finalAudioFile.exists()) {
                finalAudioFile.delete()
            }
            if (!tempAudioFile.renameTo(finalAudioFile)) {
                tempAudioFile.copyTo(finalAudioFile, overwrite = true)
                tempAudioFile.delete()
            }

            // Descarga de letras en background
            runCatching {
                val lyricsResponse = monkifyApi.getLyrics(track.id)
                if (lyricsResponse.content.isNotBlank()) {
                    val finalLyricsFile = getLyricsFile(track.id)
                    finalLyricsFile.writeText(lyricsResponse.content)
                }
            }

            localTrackRepository.markAsDownloaded(track, finalAudioFile.absolutePath)
            stateFlow.value = DownloadState.Downloaded(finalAudioFile.absolutePath)
            Result.success(finalAudioFile.absolutePath)
        } catch (e: Exception) {
            if (tempAudioFile.exists()) {
                tempAudioFile.delete()
            }
            stateFlow.value = DownloadState.Error(e.message ?: "Error en la descarga")
            Result.failure(e)
        }
    }

    override suspend fun deleteDownloadedTrack(trackId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val audioFile = getAudioFile(trackId)
            if (audioFile.exists()) {
                audioFile.delete()
            }
            val tempAudioFile = File(musicDir, "$trackId.tmp")
            if (tempAudioFile.exists()) {
                tempAudioFile.delete()
            }
            val lyricsFile = getLyricsFile(trackId)
            if (lyricsFile.exists()) {
                lyricsFile.delete()
            }
            localTrackRepository.removeDownload(trackId)
            val stateFlow = downloadStates[trackId]
            stateFlow?.value = DownloadState.NotDownloaded
        }
    }
}
