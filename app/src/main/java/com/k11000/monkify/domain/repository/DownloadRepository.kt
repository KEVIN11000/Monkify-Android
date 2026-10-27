package com.k11000.monkify.domain.repository

import com.k11000.monkify.domain.model.DownloadState
import com.k11000.monkify.domain.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface DownloadRepository {
    fun getDownloadState(trackId: String): Flow<DownloadState>
    val downloadingTrackIds: StateFlow<Set<String>>
    val downloadedTrackIds: Flow<Set<String>>

    suspend fun downloadTrack(track: Track): Result<String>
    suspend fun downloadTracks(
        tracks: List<Track>,
        onProgress: (completed: Int, total: Int) -> Unit = { _, _ -> }
    ): Result<Unit>
    suspend fun deleteDownloadedTrack(trackId: String): Result<Unit>
    suspend fun toggleDownload(track: Track)
    fun isTrackDownloaded(trackId: String): Boolean
    fun getLocalLyrics(trackId: String): String?
    fun getLocalAudioPath(trackId: String): String?
    fun getTotalDownloadsSizeBytes(): Long

    // Descargas de playlists completas en segundo plano persistente
    fun downloadPlaylist(playlistId: String, tracks: List<Track>)
    fun cancelPlaylistDownload(playlistId: String)
    fun isPlaylistDownloading(playlistId: String): StateFlow<Boolean>
    fun getPlaylistDownloadProgress(playlistId: String): StateFlow<Pair<Int, Int>>

    // Métodos de compatibilidad y conveniencia
    suspend fun isDownloaded(trackId: String): Boolean = isTrackDownloaded(trackId)
    suspend fun removeDownload(trackId: String): Result<Unit> = deleteDownloadedTrack(trackId)

    // Reconciliación de descargas huérfanas o ante reinstalación de app
    suspend fun reconcileDownloads(): Int = 0
}
