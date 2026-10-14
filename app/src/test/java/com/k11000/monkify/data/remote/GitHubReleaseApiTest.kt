package com.k11000.monkify.data.remote

import com.google.gson.Gson
import com.k11000.monkify.data.remote.dto.GitHubReleaseDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubReleaseApiTest {

    private val gson = Gson()

    @Test
    fun deserializes_valid_github_release_json_correctly() {
        val sampleJson = """
            {
              "tag_name": "v1.2.0",
              "name": "Monkify v1.2.0 - Offline Mode & Fixes",
              "body": "## Cambios\n* Descarga offline\n* Mejoras en UI",
              "published_at": "2026-10-14T20:00:00Z",
              "assets": [
                {
                  "name": "Monkify-v1.2.0.apk",
                  "size": 18239000,
                  "browser_download_url": "https://github.com/KEVIN11000/Monkify-Android/releases/download/v1.2.0/Monkify-v1.2.0.apk",
                  "content_type": "application/vnd.android.package-archive"
                }
              ]
            }
        """.trimIndent()

        val release = gson.fromJson(sampleJson, GitHubReleaseDto::class.java)

        assertNotNull(release)
        assertEquals("v1.2.0", release.tagName)
        assertEquals("Monkify v1.2.0 - Offline Mode & Fixes", release.name)
        assertTrue(release.body?.contains("Descarga offline") == true)
        assertEquals(1, release.assets.size)

        val asset = release.assets[0]
        assertEquals("Monkify-v1.2.0.apk", asset.name)
        assertEquals(18239000L, asset.size)
        assertEquals(
            "https://github.com/KEVIN11000/Monkify-Android/releases/download/v1.2.0/Monkify-v1.2.0.apk",
            asset.downloadUrl
        )
        assertEquals("application/vnd.android.package-archive", asset.contentType)
    }

    @Test
    fun handles_release_without_assets_gracefully() {
        val jsonWithoutAssets = """
            {
              "tag_name": "v1.0.0",
              "name": "Initial Release",
              "body": "Versión inicial",
              "assets": []
            }
        """.trimIndent()

        val release = gson.fromJson(jsonWithoutAssets, GitHubReleaseDto::class.java)

        assertNotNull(release)
        assertEquals("v1.0.0", release.tagName)
        assertTrue(release.assets.isEmpty())
    }
}
