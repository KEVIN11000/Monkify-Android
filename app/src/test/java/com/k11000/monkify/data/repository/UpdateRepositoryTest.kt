package com.k11000.monkify.data.repository

import com.k11000.monkify.data.remote.api.GitHubReleaseApi
import com.k11000.monkify.data.remote.dto.GitHubAssetDto
import com.k11000.monkify.data.remote.dto.GitHubReleaseDto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class UpdateRepositoryTest {

    @Test
    fun version_comparison_identifies_newer_versions_correctly() {
        assertTrue(UpdateRepositoryImpl.isNewerVersion("v1.2.0", "1.0"))
        assertTrue(UpdateRepositoryImpl.isNewerVersion("1.10.0", "1.2.0"))
        assertTrue(UpdateRepositoryImpl.isNewerVersion("v2.0.0", "1.9.9"))
        assertTrue(UpdateRepositoryImpl.isNewerVersion("v1.0.1", "1.0.0"))
        assertTrue(UpdateRepositoryImpl.isNewerVersion("v1.0.1", "1.0"))

        assertFalse(UpdateRepositoryImpl.isNewerVersion("v1.0.0", "1.0"))
        assertFalse(UpdateRepositoryImpl.isNewerVersion("1.0", "1.0"))
        assertFalse(UpdateRepositoryImpl.isNewerVersion("v0.9.9", "1.0.0"))
        assertFalse(UpdateRepositoryImpl.isNewerVersion("v1.0.0", "1.1.0"))
    }

    @Test
    fun check_for_update_returns_available_when_newer_release_has_apk() = runBlocking {
        val fakeApi = object : GitHubReleaseApi {
            override suspend fun getLatestRelease(owner: String, repo: String): GitHubReleaseDto {
                return GitHubReleaseDto(
                    tagName = "v99.0.0",
                    name = "Future Version",
                    body = "Notas de la versión",
                    assets = listOf(
                        GitHubAssetDto(
                            name = "Monkify-v99.0.0.apk",
                            size = 15000000L,
                            downloadUrl = "https://github.com/KEVIN11000/Monkify-Android/releases/download/v99.0.0/Monkify-v99.0.0.apk"
                        )
                    )
                )
            }
        }

        val repository = UpdateRepositoryImpl(fakeApi)
        val result = repository.checkForUpdate()

        assertTrue(result.isSuccess)
        val update = result.getOrThrow()
        assertTrue(update.isUpdateAvailable)
        assertEquals("v99.0.0", update.latestVersionName)
        assertEquals("https://github.com/KEVIN11000/Monkify-Android/releases/download/v99.0.0/Monkify-v99.0.0.apk", update.downloadUrl)
        assertEquals(15000000L, update.apkSize)
        assertEquals("Notas de la versión", update.releaseNotes)
    }

    @Test
    fun check_for_update_returns_unavailable_when_no_apk_asset_exists() = runBlocking {
        val fakeApi = object : GitHubReleaseApi {
            override suspend fun getLatestRelease(owner: String, repo: String): GitHubReleaseDto {
                return GitHubReleaseDto(
                    tagName = "v99.0.0",
                    name = "Source Only Release",
                    body = "Sin apk",
                    assets = emptyList()
                )
            }
        }

        val repository = UpdateRepositoryImpl(fakeApi)
        val result = repository.checkForUpdate()

        assertTrue(result.isSuccess)
        val update = result.getOrThrow()
        assertFalse(update.isUpdateAvailable)
    }

    @Test
    fun check_for_update_returns_failure_on_network_error() = runBlocking {
        val fakeApi = object : GitHubReleaseApi {
            override suspend fun getLatestRelease(owner: String, repo: String): GitHubReleaseDto {
                throw IOException("Network timeout")
            }
        }

        val repository = UpdateRepositoryImpl(fakeApi)
        val result = repository.checkForUpdate()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }
}
