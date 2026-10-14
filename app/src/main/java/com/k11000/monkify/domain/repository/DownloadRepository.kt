package com.k11000.monkify.domain.repository

import com.k11000.monkify.domain.model.DownloadState
import com.k11000.monkify.domain.model.Track
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {
    fun getDownloadState(trackId: String): Flow<DownloadState>
    suspend fun downloadTrack(track: Track): Result<String>
    suspend fun deleteDownloadedTrack(trackId: String): Result<Unit>
    fun isTrackDownloaded(trackId: String): Boolean
    fun getLocalLyrics(trackId: String): String?
    fun getLocalAudioPath(trackId: String): String?
}
