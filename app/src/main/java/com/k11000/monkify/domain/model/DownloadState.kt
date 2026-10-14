package com.k11000.monkify.domain.model

sealed interface DownloadState {
    data object NotDownloaded : DownloadState
    data class Downloading(val progressPercent: Int = 0) : DownloadState
    data class Downloaded(val localPath: String) : DownloadState
    data class Error(val message: String) : DownloadState
}
