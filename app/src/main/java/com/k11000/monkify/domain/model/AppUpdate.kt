package com.k11000.monkify.domain.model

data class AppUpdate(
    val latestVersionName: String,
    val currentVersionName: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val apkSize: Long,
    val isUpdateAvailable: Boolean
)
