package com.k11000.monkify.data.repository

import com.k11000.monkify.BuildConfig
import com.k11000.monkify.data.remote.api.GitHubReleaseApi
import com.k11000.monkify.domain.model.AppUpdate
import com.k11000.monkify.domain.repository.UpdateRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateRepositoryImpl @Inject constructor(
    private val gitHubReleaseApi: GitHubReleaseApi
) : UpdateRepository {

    override suspend fun checkForUpdate(): Result<AppUpdate> {
        return runCatching {
            val release = gitHubReleaseApi.getLatestRelease()
            val remoteVersion = release.tagName.trim()
            val currentVersion = BuildConfig.VERSION_NAME

            val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
                ?: release.assets.firstOrNull()

            val isAvailable = isNewerVersion(remoteVersion, currentVersion)

            AppUpdate(
                latestVersionName = remoteVersion,
                currentVersionName = currentVersion,
                releaseNotes = release.body ?: "",
                downloadUrl = apkAsset?.downloadUrl ?: "",
                apkSize = apkAsset?.size ?: 0L,
                isUpdateAvailable = isAvailable && !apkAsset?.downloadUrl.isNullOrBlank()
            )
        }
    }

    companion object {
        fun isNewerVersion(remote: String, local: String): Boolean {
            val rParts = remote.trim().removePrefix("v").removePrefix("V")
                .split(".")
                .map { it.toIntOrNull() ?: 0 }
            val lParts = local.trim().removePrefix("v").removePrefix("V")
                .split(".")
                .map { it.toIntOrNull() ?: 0 }

            val maxLen = maxOf(rParts.size, lParts.size)
            for (i in 0 until maxLen) {
                val r = rParts.getOrElse(i) { 0 }
                val l = lParts.getOrElse(i) { 0 }
                if (r > l) return true
                if (r < l) return false
            }
            return false
        }
    }
}
