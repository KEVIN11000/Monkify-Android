package com.k11000.monkify.data.repository

import android.content.Context
import android.os.Environment
import com.k11000.monkify.data.remote.api.MonkifyApi
import com.k11000.monkify.domain.model.DownloadState
import com.k11000.monkify.domain.model.Track
import com.k11000.monkify.domain.repository.DownloadRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import com.k11000.monkify.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
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

    private val _downloadingTrackIds = MutableStateFlow<Set<String>>(emptySet())
    override val downloadingTrackIds: StateFlow<Set<String>> = _downloadingTrackIds.asStateFlow()

    private val repoScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activePlaylistJobs = ConcurrentHashMap<String, Job>()
    private val playlistProgressMap = ConcurrentHashMap<String, MutableStateFlow<Pair<Int, Int>>>()
    private val playlistDownloadingMap = ConcurrentHashMap<String, MutableStateFlow<Boolean>>()

    override fun isPlaylistDownloading(playlistId: String): StateFlow<Boolean> {
        return playlistDownloadingMap.computeIfAbsent(playlistId) { MutableStateFlow(false) }.asStateFlow()
    }

    override fun getPlaylistDownloadProgress(playlistId: String): StateFlow<Pair<Int, Int>> {
        return playlistProgressMap.computeIfAbsent(playlistId) { MutableStateFlow(Pair(0, 0)) }.asStateFlow()
    }

    override val downloadedTrackIds: Flow<Set<String>> = localTrackRepository.downloadedTracks.map { tracks ->
        tracks.map { it.id }.toSet()
    }

    /**
     * Directorio de almacenamiento Scoped para música:
     * Prioriza context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) que no requiere permisos
     * runtime en Android 10-15 y evita saturar la partición interna restringida.
     */
    private val musicDir: File
        get() = (context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: File(context.filesDir, "music")).apply {
            if (!exists()) mkdirs()
        }

    /**
     * Directorio para letras sincronizadas en sandbox privado.
     */
    private val lyricsDir: File
        get() = File(context.filesDir, "lyrics").apply {
            if (!exists()) mkdirs()
        }

    fun getAudioFile(trackId: String): File {
        val primary = File(musicDir, "$trackId.mp3")
        if (primary.exists() && primary.length() > 0) return primary

        val fallbackInternal = File(context.filesDir, "music/$trackId.mp3")
        if (fallbackInternal.exists() && fallbackInternal.length() > 0) return fallbackInternal

        val legacyDownloads = File(context.filesDir, "downloads/$trackId.mp3")
        if (legacyDownloads.exists() && legacyDownloads.length() > 0) return legacyDownloads

        return primary
    }

    fun getLyricsFile(trackId: String): File {
        val primary = File(lyricsDir, "$trackId.lrc")
        if (primary.exists() && primary.length() > 0) return primary

        val legacyDownloads = File(context.filesDir, "downloads/$trackId.lrc")
        if (legacyDownloads.exists() && legacyDownloads.length() > 0) return legacyDownloads

        return primary
    }

    override fun getDownloadState(trackId: String): Flow<DownloadState> {
        val flow = downloadStates.computeIfAbsent(trackId) {
            val initialState = if (isTrackDownloaded(trackId)) {
                DownloadState.Downloaded(getAudioFile(trackId).absolutePath)
            } else {
                DownloadState.NotDownloaded
            }
            MutableStateFlow(initialState)
        }
        // Reconciliación síncrona: si no está en progreso activo, verificar la presencia física del archivo en almacenamiento
        val current = flow.value
        if (current !is DownloadState.Downloading) {
            val isDownloaded = isTrackDownloaded(trackId)
            val expected = if (isDownloaded) DownloadState.Downloaded(getAudioFile(trackId).absolutePath) else DownloadState.NotDownloaded
            if (current != expected) {
                flow.value = expected
            }
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

    override fun getTotalDownloadsSizeBytes(): Long {
        var total = 0L
        val scannedDirs = listOfNotNull(
            musicDir,
            File(context.filesDir, "music"),
            File(context.filesDir, "downloads")
        ).distinctBy { it.absolutePath }

        scannedDirs.forEach { dir ->
            if (dir.exists()) {
                dir.listFiles()?.forEach { file ->
                    if (file.isFile && (file.extension.equals("mp3", ignoreCase = true) || file.extension.equals("lrc", ignoreCase = true))) {
                        total += file.length()
                    }
                }
            }
        }
        if (lyricsDir.exists()) {
            lyricsDir.listFiles()?.forEach { file ->
                if (file.isFile && file.extension.equals("lrc", ignoreCase = true)) {
                    total += file.length()
                }
            }
        }
        return total
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
        _downloadingTrackIds.value = _downloadingTrackIds.value + track.id

        val tempAudioFile = File(musicDir, "${track.id}.tmp")
        val finalAudioFile = File(musicDir, "${track.id}.mp3")

        try {
            // Verificar espacio en disco (> 25 MB)
            val usableSpace = musicDir.usableSpace
            if (usableSpace in 1 until (25L * 1024 * 1024)) {
                throw IOException("Espacio insuficiente en disco para descargar la pista (menos de 25 MB disponibles)")
            }

            // Si ya existe y es válido, registrar en Room y retornar de inmediato
            if (finalAudioFile.exists() && finalAudioFile.length() > 0) {
                localTrackRepository.markAsDownloaded(track, finalAudioFile.absolutePath)
                stateFlow.value = DownloadState.Downloaded(finalAudioFile.absolutePath)
                return@withContext Result.success(finalAudioFile.absolutePath)
            }

            val url = resolveStreamUrl(track.streamUrl, track.id)
            val request = okhttp3.Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                val errorMsg = when (response.code) {
                    404 -> "Pista no disponible en el servidor (HTTP 404)"
                    401, 403 -> "Acceso no autorizado al audio (${response.code})"
                    500, 502, 503 -> "Servidor de almacenamiento no disponible (${response.code})"
                    else -> "Fallo en la descarga de audio (HTTP ${response.code})"
                }
                throw IOException(errorMsg)
            }

            val body = response.body ?: throw IOException("Cuerpo de respuesta vacío")
            val totalBytes = body.contentLength()

            body.byteStream().use { input ->
                FileOutputStream(tempAudioFile).use { output ->
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
                    val finalLyricsFile = File(lyricsDir, "${track.id}.lrc")
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
            val userMsg = when {
                e.message?.contains("404") == true -> "Pista no disponible en el servidor (HTTP 404)"
                e.message?.contains("Espacio insuficiente") == true -> e.message ?: "Espacio insuficiente"
                else -> e.message ?: "Error en la descarga"
            }
            stateFlow.value = DownloadState.Error(userMsg)
            Result.failure(e)
        } finally {
            _downloadingTrackIds.value = _downloadingTrackIds.value - track.id
        }
    }

    override suspend fun downloadTracks(
        tracks: List<Track>,
        onProgress: (completed: Int, total: Int) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        var completed = 0
        for (track in tracks) {
            if (!isTrackDownloaded(track.id)) {
                downloadTrack(track)
            }
            completed++
            onProgress(completed, tracks.size)
        }
        Result.success(Unit)
    }

    override suspend fun toggleDownload(track: Track) {
        if (isTrackDownloaded(track.id)) {
            deleteDownloadedTrack(track.id)
        } else {
            downloadTrack(track)
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

    override fun downloadPlaylist(playlistId: String, tracks: List<Track>) {
        if (activePlaylistJobs.containsKey(playlistId)) {
            AppLogger.w("DownloadRepo", "La playlist $playlistId ya está descargándose actualmente")
            return
        }

        val progressFlow = playlistProgressMap.computeIfAbsent(playlistId) { MutableStateFlow(Pair(0, 0)) }
        val downloadingFlow = playlistDownloadingMap.computeIfAbsent(playlistId) { MutableStateFlow(false) }

        val job = repoScope.launch {
            downloadingFlow.value = true
            progressFlow.value = Pair(0, tracks.size)
            AppLogger.i("DownloadRepo", "Iniciando descarga en segundo plano de playlist $playlistId (${tracks.size} canciones)")

            var completed = 0
            try {
                for (track in tracks) {
                    if (!isActive) break
                    if (!isTrackDownloaded(track.id)) {
                        downloadTrack(track)
                    }
                    completed++
                    progressFlow.value = Pair(completed, tracks.size)
                }
                AppLogger.i("DownloadRepo", "Descarga de playlist $playlistId finalizada: $completed/${tracks.size}")
            } catch (e: Exception) {
                AppLogger.e("DownloadRepo", "Error durante descarga de playlist $playlistId", e)
            } finally {
                downloadingFlow.value = false
                activePlaylistJobs.remove(playlistId)
            }
        }
        activePlaylistJobs[playlistId] = job
    }

    override fun cancelPlaylistDownload(playlistId: String) {
        val job = activePlaylistJobs.remove(playlistId)
        if (job != null) {
            job.cancel()
            AppLogger.i("DownloadRepo", "Descarga de playlist $playlistId cancelada por el usuario")
            playlistDownloadingMap[playlistId]?.value = false
        }
    }
}
